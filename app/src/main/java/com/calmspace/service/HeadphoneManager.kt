package com.calmspace.service

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ─────────────────────────────────────────────────────────────────────
// Headphone Manager
//
// Detects wired and Bluetooth headphone/headset connection via
// AudioDeviceCallback. Exposes isConnected as a StateFlow.
//
// Also provides getHeadsetMicDevice() so the service can route
// AudioRecord to the headset microphone when one is available —
// giving better ambient noise detection from closer to the user's mouth.
//
// onConnected  — called when headphones are plugged/paired
// onDisconnected — called when headphones are removed mid-session
// ─────────────────────────────────────────────────────────────────────

class HeadphoneManager(
    private val audioManager: AudioManager,
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit
) {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private fun AudioDeviceInfo.isHeadphoneOutputType(): Boolean =
        type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
        type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
        type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP

    // Returns the headset microphone device if one is currently connected.
    // TYPE_WIRED_HEADSET appears in both input and output device lists.
    // TYPE_BLUETOOTH_SCO is the Bluetooth mic source.
    fun getHeadsetMicDevice(): AudioDeviceInfo? =
        audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }

    // Returns the headphone/headset output device so AudioTrack can route to it explicitly.
    fun getHeadphoneOutputDevice(): AudioDeviceInfo? =
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
            it.isHeadphoneOutputType()
        }

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            if (addedDevices.any { it.isHeadphoneOutputType() }) {
                _isConnected.value = true
                onConnected()
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (removedDevices.any { it.isHeadphoneOutputType() }) {
                val stillConnected = audioManager
                    .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    .any { it.isHeadphoneOutputType() }
                _isConnected.value = stillConnected
                if (!stillConnected) onDisconnected()
            }
        }
    }

    fun start() {
        _isConnected.value = audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .any { it.isHeadphoneOutputType() }
        audioManager.registerAudioDeviceCallback(callback, null)
    }

    fun stop() {
        audioManager.unregisterAudioDeviceCallback(callback)
    }
}
