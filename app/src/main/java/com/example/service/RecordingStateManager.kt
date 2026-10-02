package com.example.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.model.RecordingEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object RecordingStateManager {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsedDurationMs = MutableStateFlow(0L)
    val elapsedDurationMs: StateFlow<Long> = _elapsedDurationMs.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0)
    val currentAmplitude: StateFlow<Int> = _currentAmplitude.asStateFlow()

    private val _liveWaveform = MutableStateFlow<List<Float>>(emptyList())
    val liveWaveform: StateFlow<List<Float>> = _liveWaveform.asStateFlow()

    // 3-second Volume Up hold state (0.0 to 1.0)
    private val _volumeUpHoldProgress = MutableStateFlow(0f)
    val volumeUpHoldProgress: StateFlow<Float> = _volumeUpHoldProgress.asStateFlow()

    private val _isVolumeUpHeld = MutableStateFlow(false)
    val isVolumeUpHeld: StateFlow<Boolean> = _isVolumeUpHeld.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _latestSavedRecording = MutableSharedFlow<RecordingEntity>(replay = 1)
    val latestSavedRecording: SharedFlow<RecordingEntity> = _latestSavedRecording.asSharedFlow()

    private val _toastEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun setRecording(recording: Boolean) {
        _isRecording.value = recording
        if (!recording) {
            _elapsedDurationMs.value = 0L
            _currentAmplitude.value = 0
            _liveWaveform.value = emptyList()
        }
    }

    fun updateDuration(durationMs: Long) {
        _elapsedDurationMs.value = durationMs
    }

    fun updateAmplitude(amplitude: Int) {
        _currentAmplitude.value = amplitude
        val normalized = (amplitude.toFloat() / 32767f).coerceIn(0.05f, 1f)
        val currentList = _liveWaveform.value.toMutableList()
        if (currentList.size >= 35) {
            currentList.removeAt(0)
        }
        currentList.add(normalized)
        _liveWaveform.value = currentList
    }

    fun setVolumeHold(held: Boolean, progress: Float) {
        _isVolumeUpHeld.value = held
        _volumeUpHoldProgress.value = progress.coerceIn(0f, 1f)
    }

    fun setAccessibilityEnabled(enabled: Boolean) {
        _isAccessibilityEnabled.value = enabled
    }

    suspend fun emitSavedRecording(recording: RecordingEntity) {
        _latestSavedRecording.emit(recording)
    }

    fun postToast(message: String) {
        _toastEvent.tryEmit(message)
    }

    fun vibratePattern(context: Context, pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val amplitudes = IntArray(pattern.size) { index ->
                        if (index % 2 == 0) 0 else 255
                    }
                    val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {
        }
    }

    // Two short buzzes when recording starts
    fun vibrateStart(context: Context) {
        vibratePattern(context, longArrayOf(0, 120, 80, 140))
    }

    // One long buzz when recording stops
    fun vibrateStop(context: Context) {
        vibratePattern(context, longArrayOf(0, 300))
    }
}
