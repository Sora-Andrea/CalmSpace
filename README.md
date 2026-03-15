# CalmSpace 🌙

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Status](https://img.shields.io/badge/Status-Alpha-orange.svg)](https://github.com/Sora-Andrea/CalmSpace)

> An adaptive, privacy-focused sleep aid that automatically adjusts to your environment

## Introduction

### What is CalmSpace?
CalmSpace is an Android application designed to help urban dwellers and light sleepers achieve uninterrupted sleep by dynamically masking environmental noises. Unlike static white noise apps, CalmSpace monitors ambient sound levels and automatically adjusts its soothing soundscapes to maintain a consistent auditory environment throughout the night.

### Why is it Useful?
- **Urban Living Challenges:** Masks unpredictable noises from traffic, neighbors, and appliances
- **No Manual Intervention:** Adapts automatically without waking the user
- **Privacy-First Design:** All audio processing happens locally on your device

## Beta Features (Current Sprint)

The goal is that by the end of March, CalmSpace will include:

### Core Features
- **Ambient Noise Monitoring:** Continuous background monitoring of decibel levels
- **Ambient Noise Visualization:** Visualization that displays the current ambient noise decibel level and disruptiveness category.
- **Adaptive Sound Mixing:** Automatically adjusts volume of selected soundscapes based on decibel level and disruptiveness of type of sound.
- **Basic Sound Library:** 5+ high-quality soundscapes (rain, white noise, brown noise, forest, ocean)
- **External Audio File Importing:** Users will be able to import their own audio files for the app to play and automatically adjust.
- **Sleep Timer & Scheduling:** Set duration for scheduled sleep windows

### AI/ML Components
- **Pretrained Sound Classification Model:** YAMNet model that classifies common sounds (521 classes)
- **Audio Event Disruption Classification:** YAMNet outputs will be grouped into one of roughly a dozen buckets(10-20) by disruptiveness of type of sound.

### UI/UX
- **Introduction/Setup for new users** 
- **Simple one-tap sleep session start** 



## Technologies

### Core Development
- **Language:** Kotlin 1.8.0+
- **Target SDK:** Android API 36 (Android 16), min API 26
- **Architecture:** MVVM with Jetpack Compose
- **Dependency Injection:** Hilt
- **Background Processing:** WorkManager & Foreground Services

### Audio System
- **Capture:** Android AudioRecord API
- **Playback:** Android AudioTrack with custom mixing engine
- **Sound Library:** Local assets (≤50MB initial size)

### AI & Machine Learning
- **Framework:** TensorFlow 
- **Model:** YAMNet, a pretrained DNN for audio classification

### Data & Storage
- **Database:** Room Persistence Library (SQLite)
- **Stores:** User preferences, sleep sessions, sound profiles, adaptation logs
- **Preferences:** DataStore for settings

### Key Dependencies
- **UI:** Jetpack Compose, Material Design 3, Navigation
- **Async:** Kotlin Coroutines & Flow
- **ML:** TensorFlow Lite & Support libraries
- **Persistence:** Room, DataStore

### Development Environment
- **IDE:** Android Studio 2024.3.1+
- **Build:** Gradle 9.3.0 with Kotlin DSL
- **Version Control:** Git
- **Audio Analysis:** Sonic Visualiser (for sound file inspection)

### System Requirements & Constraints
- **Total Storage:** ≤100MB (app + data)
- **Runtime Memory:** ≤200MB during active session
- **ML Model:** ≤40MB (quantized)
- **Sound Library:** ≤50MB base sounds
- **Permissions:** Microphone access required



## Installation

### For End Users
1. **Download from Google Play** *(Link to be added when published)*
2. **Minimum Requirements:**
   - Android 8.0 (API level 26) or higher
   - 50MB free storage
   - Microphone permission (for ambient monitoring)
3. **First-Time Setup:**
   - Grant microphone permission when prompted
   - Complete the brief calibration wizard
   - Select your preferred base soundscape
   - Tap "Start Sleeping" to begin

### Usage Tips
- Place your phone within 3-5 feet of your bed for optimal monitoring
- Run the calibration during your typical sleep time for best results
- Use headphones for personal listening and to avoid audio feedback

## Development Setup

### Prerequisites
- Android Studio  (Android Studio Meerkat | 2024.3.1 or higher)
- Android SDK API level 26+
- Git
- Minimum 8GB RAM (16GB recommended for emulator)

### Building from Source
1. **Clone the repository:**
   ```bash
   git clone https://github.com/Sora-Andrea/CalmSpace.git
   cd CalmSpace 
   ```
## Contributors
- Colt Cruz @ColtCruz
- Sora Andrea @Sora-Andrea
- Nathaniel Rhodes @Developer-Nate
