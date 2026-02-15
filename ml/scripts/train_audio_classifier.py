#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Audio Classification with UrbanSound8K
--------------------------------------
This script trains a CNN on Mel-Frequency Cepstral Coefficients (MFCCs)
extracted from UrbanSound8K audio clips. It saves:
  - A TensorFlow SavedModel
  - A quantized TFLite model (suitable for Android)
"""

import os
import argparse
import pickle
import numpy as np
import pandas as pd
import tensorflow as tf
import librosa
import soundfile as sf   # optional, used for robust audio reading
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
from tensorflow.keras import layers, models, callbacks

# ----------------------------------------------------------------------
# CONFIGURATION (all hyperparameters in one place)
# ----------------------------------------------------------------------
class Config:
    # Paths – these should be relative to the project root (android studio)
    # The script is expected to be run from the project root or with proper paths.
    DATASET_ROOT = "../datasets/UrbanSound8K"          # folder containing folds and csv
    METADATA_FILE = "../datasets/UrbanSound8K/UrbanSound8K.csv"
    AUDIO_FOLDER = "../datasets/UrbanSound8K"
    OUTPUT_MODEL_DIR = "../models/audio_classifier"    # where SavedModel will be stored
    OUTPUT_TFLITE_PATH = "../models/audio_classifier.tflite"

    # Audio preprocessing parameters
    SAMPLE_RATE = 22050          # UrbanSound8K is already 22.05kHz,
    DURATION = 4.0              # pad/truncate all clips to 4 seconds
    N_MFCC = 40                # number of MFCC coefficients
    N_FFT = 2048              # FFT window size
    HOP_LENGTH = 512          # hop length for STFT
    N_MELS = 128              # number of Mel bands (used internally by librosa)

    # Derived constants
    SAMPLES_PER_CLIP = int(SAMPLE_RATE * DURATION)
    EXPECTED_MFCC_SHAPE = (None, N_MFCC)  # (time_steps, n_mfcc) – time

    # Training hyperparameters
    BATCH_SIZE = 32
    EPOCHS = 50
    LEARNING_RATE = 1e-4
    VALIDATION_SPLIT = 0.2    # fraction of training data used for validation
    TEST_FOLD = 10             # UrbanSound8K uses 10 folds so we will too

    # Model architecture
    DROPOUT_RATE = 0.5
    L2_REG = 1e-4

# ----------------------------------------------------------------------
# DATA LOADING & PREPROCESSING
# ----------------------------------------------------------------------
def load_metadata():
    """
    Load the UrbanSound8K CSV metadata file.
    Returns a pandas DataFrame with columns: slice_file_name, fsID, start, end,
    salience, fold, classID, class.
    """
    df = pd.read_csv(Config.METADATA_FILE)
    return df

def build_file_path(row):
    """
    Construct the full path to an audio file from a metadata row.
    UrbanSound8K stores files as: fold<fold>/<slice_file_name>
    """
    fold = f"fold{row['fold']}"
    filename = row['slice_file_name']
    return os.path.join(Config.AUDIO_FOLDER, fold, filename)

def load_audio(file_path, target_sr=Config.SAMPLE_RATE, duration=Config.DURATION):
    """
    Load an audio file and ensure it has a fixed duration.
    - If longer than duration: truncated.
    - If shorter: zero-padded at the end.
    Returns the waveform as a numpy array of shape (samples,).
    """
    try:
        # Use soundfile + librosa for reliable reading
        audio, sr = sf.read(file_path)
        if len(audio.shape) > 1:
            audio = audio.mean(axis=1)  # convert to mono
        audio = librosa.resample(audio, orig_sr=sr, target_sr=target_sr)
    except Exception as e:
        print(f"Error loading {file_path}: {e}")
        # Return silent audio as fallback
        audio = np.zeros(target_sr * int(duration))

    expected_length = int(target_sr * duration)
    if len(audio) > expected_length:
        audio = audio[:expected_length]
    else:
        pad_width = expected_length - len(audio)
        audio = np.pad(audio, (0, pad_width), mode='constant')

    return audio

def extract_mfcc(audio, sr=Config.SAMPLE_RATE, n_mfcc=Config.N_MFCC,
                 n_fft=Config.N_FFT, hop_length=Config.HOP_LENGTH):
    """
    Extract MFCC features from a waveform.
    Returns a 2D numpy array of shape (time_steps, n_mfcc).
    """
    mfcc = librosa.feature.mfcc(y=audio, sr=sr, n_mfcc=n_mfcc,
                                 n_fft=n_fft, hop_length=hop_length)
    # Transpose so that time is the first dimension (required for CNN input)
    mfcc = mfcc.T  # shape: (time, coeff)
    return mfcc

def preprocess_dataset(df, max_samples=None):
    """
    Iterate over the metadata DataFrame, load and preprocess each audio clip.
    Returns:
        X: numpy array of MFCC features with shape (num_samples, time, n_mfcc, 1)
        y: numpy array of integer labels
        label_encoder: fitted LabelEncoder object (for later use in Android)
    """
    X_list = []
    y_list = []
    label_encoder = LabelEncoder()
    # Encode class names to integers
    df['label_encoded'] = label_encoder.fit_transform(df['class'])

    total = len(df) if max_samples is None else max_samples
    for idx, row in df.iterrows():
        if max_samples and idx >= max_samples:
            break
        if idx % 500 == 0:
            print(f"Processing {idx}/{total}")

        file_path = build_file_path(row)
        audio = load_audio(file_path)
        mfcc = extract_mfcc(audio)

        X_list.append(mfcc)
        y_list.append(row['label_encoded'])

    # Pad or truncate MFCC sequences to a fixed time dimension
    # For batching in TensorFlow.
    # We compute the median time length and pad/truncate to that value.
    time_lengths = [x.shape[0] for x in X_list]
    target_time = int(np.median(time_lengths))
    print(f"Median MFCC time steps: {target_time}. Using this as fixed length.")

    X_padded = []
    for mfcc in X_list:
        current_time = mfcc.shape[0]
        if current_time >= target_time:
            X_padded.append(mfcc[:target_time, :])
        else:
            pad = target_time - current_time
            mfcc = np.pad(mfcc, ((0, pad), (0, 0)), mode='constant')
            X_padded.append(mfcc)

    X = np.array(X_padded)
    y = np.array(y_list)

    # Add channel dimension for CNN: (samples, time, n_mfcc, 1)
    X = X[..., np.newaxis]
    return X, y, label_encoder

def split_data_by_fold(df, test_fold=Config.TEST_FOLD):
    """
    Split UrbanSound8K metadata into train and test sets according to a given fold.
    """
    train_df = df[df['fold'] != test_fold]
    test_df = df[df['fold'] == test_fold]
    return train_df, test_df

# ----------------------------------------------------------------------
# MODEL DEFINITION
# ----------------------------------------------------------------------
def build_model(input_shape, num_classes):
    """
    Create a CNN model for MFCC-based classification.
    (initial)Architecture:
      - 2 Conv2D layers with batch norm, max pooling, dropout
      - Global Average Pooling
      - Dense hidden layer + dropout
      - Output softmax layer
    """
    model = models.Sequential(name="UrbanSound8K_Classifier")

    # First convolutional block
    model.add(layers.Conv2D(32, (3, 3), activation='relu', padding='same',
                            kernel_regularizer=tf.keras.regularizers.l2(Config.L2_REG),
                            input_shape=input_shape))
    model.add(layers.BatchNormalization())
    model.add(layers.MaxPooling2D((2, 2)))
    model.add(layers.Dropout(0.2))

    # Second convolutional block
    model.add(layers.Conv2D(64, (3, 3), activation='relu', padding='same',
                            kernel_regularizer=tf.keras.regularizers.l2(Config.L2_REG)))
    model.add(layers.BatchNormalization())
    model.add(layers.MaxPooling2D((2, 2)))
    model.add(layers.Dropout(0.2))

    # Third convolutional block
    model.add(layers.Conv2D(128, (3, 3), activation='relu', padding='same',
                            kernel_regularizer=tf.keras.regularizers.l2(Config.L2_REG)))
    model.add(layers.BatchNormalization())
    model.add(layers.MaxPooling2D((2, 2)))
    model.add(layers.Dropout(0.2))

    # Global pooling and dense layers
    model.add(layers.GlobalAveragePooling2D())
    model.add(layers.Dense(64, activation='relu',
                           kernel_regularizer=tf.keras.regularizers.l2(Config.L2_REG)))
    model.add(layers.BatchNormalization())
    model.add(layers.Dropout(Config.DROPOUT_RATE))
    model.add(layers.Dense(num_classes, activation='softmax'))

    return model

# ----------------------------------------------------------------------
# TRAINING
# ----------------------------------------------------------------------
def train_model(X_train, y_train, X_val, y_val, num_classes):
    """
    Compile and train the model.
    Saves the best model checkpoint and training history.
    """
    input_shape = X_train.shape[1:]  # (time, n_mfcc, 1)
    model = build_model(input_shape, num_classes)

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=Config.LEARNING_RATE),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )

    model.summary()

    # Callbacks
    checkpoint = callbacks.ModelCheckpoint(
        filepath=os.path.join(Config.OUTPUT_MODEL_DIR, 'best_model.h5'),
        monitor='val_accuracy',
        save_best_only=True,
        save_weights_only=False,
        mode='max',
        verbose=1
    )
    early_stop = callbacks.EarlyStopping(
        monitor='val_loss',
        patience=10,
        restore_best_weights=True,
        verbose=1
    )
    reduce_lr = callbacks.ReduceLROnPlateau(
        monitor='val_loss',
        factor=0.5,
        patience=5,
        min_lr=1e-7,
        verbose=1
    )

    history = model.fit(
        X_train, y_train,
        batch_size=Config.BATCH_SIZE,
        epochs=Config.EPOCHS,
        validation_data=(X_val, y_val),
        callbacks=[checkpoint, early_stop, reduce_lr],
        verbose=1
    )

    # Save the final model (SavedModel format)
    model.export(Config.OUTPUT_MODEL_DIR)
    print(f"Model saved to {Config.OUTPUT_MODEL_DIR}")

    return model, history

# ----------------------------------------------------------------------
# EVALUATION
# ----------------------------------------------------------------------
def evaluate_model(model, X_test, y_test, label_encoder):
    """
    Evaluate on the test set and print classification report.
    """
    loss, acc = model.evaluate(X_test, y_test, verbose=0)
    print(f"Test accuracy: {acc:.4f}, Test loss: {loss:.4f}")

    # Predict and get classification report
    y_pred = model.predict(X_test)
    y_pred_classes = np.argmax(y_pred, axis=1)

    from sklearn.metrics import classification_report
    report = classification_report(y_test, y_pred_classes,
                                   target_names=label_encoder.classes_)
    print("\nClassification Report:\n", report)

    return y_pred_classes

# ----------------------------------------------------------------------
# TFLITE CONVERSION (with quantization)
# ----------------------------------------------------------------------
def convert_to_tflite(model, representative_dataset=None):
    """
    Convert a SavedModel to TFLite.
    If a representative dataset is provided, apply full integer quantization.
    """
    converter = tf.lite.TFLiteConverter.from_saved_model(Config.OUTPUT_MODEL_DIR)

    # Optimize for size (and optionally quantize)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]

    if representative_dataset:
        # Full integer quantization (int8) – required for EdgeTPU, good for size/speed
        converter.representative_dataset = representative_dataset
        converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
        converter.inference_input_type = tf.int8
        converter.inference_output_type = tf.int8
        print("Applying full integer quantization (int8).")
    else:
        # Fallback: dynamic range quantization (weights only int8, activations float)
        print("Applying dynamic range quantization.")

    tflite_model = converter.convert()

    # Save the TFLite model
    with open(Config.OUTPUT_TFLITE_PATH, 'wb') as f:
        f.write(tflite_model)
    print(f"TFLite model saved to {Config.OUTPUT_TFLITE_PATH}")

def representative_dataset_gen():
    """
    Generator that yields representative samples for quantization calibration.
    We use a small subset of the training data (MFCC features).
    This function is passed to the TFLite converter.
    """
    # We need to load a few samples from the training set.
    # Since the dataset is large, we pre-load a few hundred examples.
    # For simplicity, we reuse the already preprocessed X_train, but this requires
    # that this function is defined inside the main block where X_train is available.
    # In a refactored version, you would pass X_train as a closure.
    # Here we assume X_train is defined in the outer scope.
    for i in range(min(100, len(X_train))):
        # The model expects shape (1, time, n_mfcc, 1)
        sample = X_train[i:i+1].astype(np.float32)
        yield [sample]

# ----------------------------------------------------------------------
# MAIN PIPELINE
# ----------------------------------------------------------------------
def main(args):
    # 1. Create output directories
    os.makedirs(Config.OUTPUT_MODEL_DIR, exist_ok=True)

    # 2. Load metadata and split by fold
    print("Loading metadata...")
    df = load_metadata()
    train_df, test_df = split_data_by_fold(df, test_fold=args.test_fold)
    print(f"Train samples: {len(train_df)}, Test samples: {len(test_df)}")

    # 3. Preprocess datasets (MFCC extraction)
    #    Use a small subset for quick testing if --quick flag is set
    max_samples = 1000 if args.quick else None
    print("Preprocessing training data...")
    X_train, y_train, label_encoder = preprocess_dataset(train_df, max_samples=max_samples)
    print("Preprocessing test data...")
    X_test, y_test, _ = preprocess_dataset(test_df, max_samples=max_samples)

    # 4. Split training data into train/validation
    X_train, X_val, y_train, y_val = train_test_split(
        X_train, y_train,
        test_size=Config.VALIDATION_SPLIT,
        stratify=y_train,
        random_state=42
    )
    print(f"Training set: {X_train.shape}, Validation set: {X_val.shape}")

    # 5. Build and train model
    num_classes = len(label_encoder.classes_)
    model, history = train_model(X_train, y_train, X_val, y_val, num_classes)

    # 6. Evaluate on test set
    evaluate_model(model, X_test, y_test, label_encoder)

    # 7. Save label encoder (needed on Android to map predictions back to class names)
    with open(os.path.join(Config.OUTPUT_MODEL_DIR, 'label_encoder.pkl'), 'wb') as f:
        pickle.dump(label_encoder, f)
    print("Label encoder saved.")

    # 8. Convert to TFLite
    if args.quantize:
        # Use a subset of training data as representative dataset for full integer quantization
        # We need to pass a generator that yields batches.
        # To avoid re-preprocessing, we define the generator inside main with access to X_train.
        def rep_dataset():
            for i in range(min(100, len(X_train))):
                yield [X_train[i:i+1].astype(np.float32)]

        convert_to_tflite(model, representative_dataset=rep_dataset)
    else:
        convert_to_tflite(model, representative_dataset=None)

    print("Pipeline completed successfully.")

# ----------------------------------------------------------------------
# COMMAND-LINE INTERFACE
# ----------------------------------------------------------------------
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Train audio classifier on UrbanSound8K")
    parser.add_argument("--test_fold", type=int, default=Config.TEST_FOLD,
                        help="Fold number to use as test set (1-10)")
    parser.add_argument("--quick", action="store_true",
                        help="Use only 1000 samples for fast debugging")
    parser.add_argument("--no_quantize", dest="quantize", action="store_false",
                        help="Skip full integer quantization (use dynamic range only)")
    parser.set_defaults(quantize=True)
    args = parser.parse_args()

    # Ensure that when --quick is used, we also reduce epochs for speed
    if args.quick:
        Config.EPOCHS = 5
        print("QUICK MODE: using 5 epochs and 1000 samples.")

    main(args)