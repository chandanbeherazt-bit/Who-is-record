package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VolumeHoldAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var holdJob: Job? = null

    private var isHolding = false
    private var holdTriggered = false
    private var holdStartTime = 0L

    companion object {
        const val HOLD_DURATION_MS = 3000L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info

        RecordingStateManager.setAccessibilityEnabled(true)
        RecordingStateManager.postToast("QuickVoice 3-second volume hold service active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used, we only filter key events
    }

    override fun onInterrupt() {
        cancelHold()
        RecordingStateManager.setAccessibilityEnabled(false)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cancelHold()
        RecordingStateManager.setAccessibilityEnabled(false)
        return super.onUnbind(intent)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false

        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    // Only start tracking on the first initial press, not on system auto-repeat
                    if (event.repeatCount == 0 && !isHolding) {
                        isHolding = true
                        holdTriggered = false
                        holdStartTime = System.currentTimeMillis()
                        startHoldCountdown()
                    }
                    if (holdTriggered) {
                        return true // Consume repeated events after hold triggered
                    }
                }
                KeyEvent.ACTION_UP -> {
                    val wasTriggered = holdTriggered
                    cancelHold()
                    if (wasTriggered) {
                        return true // Consume release event if 3-second hold fired
                    }
                    // If released before 3 seconds, let the event pass through to adjust volume
                    return false
                }
            }
        }

        return super.onKeyEvent(event)
    }

    private fun startHoldCountdown() {
        holdJob?.cancel()
        holdJob = serviceScope.launch {
            RecordingStateManager.setVolumeHold(true, 0f)

            while (isActive) {
                val elapsed = System.currentTimeMillis() - holdStartTime
                val progress = (elapsed.toFloat() / HOLD_DURATION_MS.toFloat()).coerceIn(0f, 1f)
                RecordingStateManager.setVolumeHold(true, progress)

                if (elapsed >= HOLD_DURATION_MS) {
                    // 3 seconds reached!
                    holdTriggered = true
                    triggerVoiceRecording()
                    RecordingStateManager.setVolumeHold(false, 1f)
                    break
                }
                delay(40L)
            }
        }
    }

    private fun triggerVoiceRecording() {
        if (RecordingStateManager.isRecording.value) {
            // Already recording -> stop
            VoiceRecordingService.stop(this)
            RecordingStateManager.postToast("Volume Hold (3s): Voice recording stopped")
        } else {
            // Not recording -> start
            VoiceRecordingService.start(this, "VOLUME_KEY_HOLD_3S")
            RecordingStateManager.postToast("Volume Hold (3s): Voice recording started in background!")
        }
    }

    private fun cancelHold() {
        holdJob?.cancel()
        holdJob = null
        isHolding = false
        holdTriggered = false
        RecordingStateManager.setVolumeHold(false, 0f)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelHold()
        RecordingStateManager.setAccessibilityEnabled(false)
        serviceScope.cancel()
    }
}
