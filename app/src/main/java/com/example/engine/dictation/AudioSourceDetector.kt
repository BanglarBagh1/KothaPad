package com.example.engine.dictation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.example.data.model.AudioSourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioSourceDetector(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentSource = MutableStateFlow(AudioSourceType.PHONE_MIC)
    val currentSource: StateFlow<AudioSourceType> = _currentSource.asStateFlow()

    private val audioReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            detectCurrentAudioSource()
        }
    }

    init {
        detectCurrentAudioSource()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        }
        try {
            context.registerReceiver(audioReceiver, filter)
        } catch (_: Exception) {
            // Register failed gracefully
        }
    }

    fun detectCurrentAudioSource() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val commDevice = audioManager.communicationDevice
                if (commDevice != null) {
                    when (commDevice.type) {
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        AudioDeviceInfo.TYPE_BLE_HEADSET -> {
                            _currentSource.value = AudioSourceType.BLUETOOTH_SCO
                            return
                        }
                        AudioDeviceInfo.TYPE_WIRED_HEADSET,
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                        AudioDeviceInfo.TYPE_USB_HEADSET -> {
                            _currentSource.value = AudioSourceType.WIRED_HEADSET
                            return
                        }
                        else -> {
                            _currentSource.value = AudioSourceType.PHONE_MIC
                            return
                        }
                    }
                }
            }

            // Fallback for older versions or if no commDevice is pinned
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            for (dev in devices) {
                when (dev.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLE_HEADSET -> {
                        _currentSource.value = AudioSourceType.BLUETOOTH_SCO
                        return
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        _currentSource.value = AudioSourceType.WIRED_HEADSET
                        return
                    }
                    else -> {}
                }
            }

            if (audioManager.isBluetoothScoOn) {
                _currentSource.value = AudioSourceType.BLUETOOTH_SCO
                return
            }

            if (audioManager.isWiredHeadsetOn) {
                _currentSource.value = AudioSourceType.WIRED_HEADSET
                return
            }

            _currentSource.value = AudioSourceType.PHONE_MIC
        } catch (_: Exception) {
            _currentSource.value = AudioSourceType.PHONE_MIC
        }
    }

    fun release() {
        try {
            context.unregisterReceiver(audioReceiver)
        } catch (_: Exception) {}
    }
}
