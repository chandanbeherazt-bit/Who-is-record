package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.model.RecordingEntity
import com.example.util.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoiceRecordingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordingJob: Job? = null

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var startTimeMs: Long = 0L
    private var triggerSource: String = "VOLUME_KEY"
    private var wakeLock: PowerManager.WakeLock? = null

    private val collectedAmplitudes = mutableListOf<Float>()

    companion object {
        const val CHANNEL_ID = "voice_recording_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.action.START_RECORDING"
        const val ACTION_STOP = "com.example.action.STOP_RECORDING"
        const val ACTION_TOGGLE = "com.example.action.TOGGLE_RECORDING"
        const val EXTRA_SOURCE = "extra_trigger_source"

        fun start(context: Context, source: String = "VOLUME_KEY") {
            val intent = Intent(context, VoiceRecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SOURCE, source)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceRecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun toggle(context: Context, source: String = "VOLUME_KEY") {
            if (RecordingStateManager.isRecording.value) {
                stop(context)
            } else {
                start(context, source)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        triggerSource = intent?.getStringExtra(EXTRA_SOURCE) ?: "VOLUME_KEY"

        when (action) {
            ACTION_START -> {
                if (!RecordingStateManager.isRecording.value) {
                    startRecording()
                }
            }
            ACTION_STOP -> {
                stopRecording()
            }
            ACTION_TOGGLE -> {
                if (RecordingStateManager.isRecording.value) {
                    stopRecording()
                } else {
                    startRecording()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startRecording() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QuickVoice:RecordingWakeLock").apply {
                acquire(10 * 60 * 1000L /*10 minutes safety timeout*/)
            }

            // Create recordings directory
            val recordingsDir = File(filesDir, "recordings").apply {
                if (!exists()) mkdirs()
            }
            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val audioFile = File(recordingsDir, "REC_$timestampStr.m4a")
            currentOutputFile = audioFile
            collectedAmplitudes.clear()

            // Initialize MediaRecorder
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            startTimeMs = System.currentTimeMillis()
            RecordingStateManager.setRecording(true)
            RecordingStateManager.vibrateStart(this)

            // Start foreground with notification
            val notification = buildRecordingNotification(0L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            // Launch tracking job for duration, amplitudes, and notification updates
            recordingJob?.cancel()
            recordingJob = serviceScope.launch {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                var lastNotificationUpdate = 0L

                while (isActive) {
                    val elapsed = System.currentTimeMillis() - startTimeMs
                    RecordingStateManager.updateDuration(elapsed)

                    // Sample amplitude
                    val amp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (_: Exception) {
                        0
                    }
                    RecordingStateManager.updateAmplitude(amp)
                    val normAmp = (amp.toFloat() / 32767f).coerceIn(0.05f, 1f)
                    collectedAmplitudes.add(normAmp)

                    // Update notification every second
                    if (elapsed - lastNotificationUpdate >= 1000L) {
                        lastNotificationUpdate = elapsed
                        notificationManager.notify(NOTIFICATION_ID, buildRecordingNotification(elapsed))
                    }

                    delay(100L)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            RecordingStateManager.postToast("Recording failed to start: ${e.localizedMessage ?: "Unknown error"}")
            cleanup()
            stopSelf()
        }
    }

    private fun buildRecordingNotification(elapsedMs: Long): Notification {
        val durationFormatted = FormatUtils.formatDuration(elapsedMs)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VoiceRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔴 QuickVoice Recording ($durationFormatted)")
            .setContentText("Triggered via $triggerSource • Tap to open app")
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_mic, getString(R.string.stop_recording), stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null

        val finalDuration = System.currentTimeMillis() - startTimeMs
        val outputFile = currentOutputFile

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {
                }
                release()
            }
        } catch (_: Exception) {
        }
        mediaRecorder = null

        RecordingStateManager.setRecording(false)
        RecordingStateManager.vibrateStop(this)

        if (outputFile != null && outputFile.exists() && finalDuration >= 800L) {
            // Save recording to Room Database
            serviceScope.launch {
                val db = AppDatabase.getInstance(this@VoiceRecordingService)
                val timeFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                val defaultTitle = "Voice Note " + timeFormat.format(Date(startTimeMs))

                // Downsample amplitudes to 40 data points
                val sampledPoints = downsampleAmplitudes(collectedAmplitudes, 40)
                val amplitudesString = sampledPoints.joinToString(",") { String.format(Locale.US, "%.2f", it) }

                val entity = RecordingEntity(
                    title = defaultTitle,
                    filePath = outputFile.absolutePath,
                    timestamp = startTimeMs,
                    durationMs = finalDuration,
                    fileSizeBytes = outputFile.length(),
                    sampleAmplitudes = amplitudesString,
                    triggerSource = triggerSource
                )

                val id = db.recordingDao().insertRecording(entity)
                val saved = entity.copy(id = id)
                RecordingStateManager.emitSavedRecording(saved)
                RecordingStateManager.postToast("Voice recording saved (${FormatUtils.formatDuration(finalDuration)})")
            }
        } else {
            // If recording was less than 800ms, consider it cancelled
            outputFile?.delete()
            RecordingStateManager.postToast("Recording too short, discarded")
        }

        cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun downsampleAmplitudes(data: List<Float>, targetCount: Int): List<Float> {
        if (data.isEmpty()) return List(targetCount) { 0.2f }
        if (data.size <= targetCount) return data

        val step = data.size.toDouble() / targetCount.toDouble()
        val result = mutableListOf<Float>()
        for (i in 0 until targetCount) {
            val idx = (i * step).toInt().coerceIn(0, data.lastIndex)
            result.add(data[idx])
        }
        return result
    }

    private fun cleanup() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        wakeLock = null
        currentOutputFile = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
        serviceScope.cancel()
    }
}
