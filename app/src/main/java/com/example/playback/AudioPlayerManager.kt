package com.example.playback

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import com.example.data.model.RecordingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val currentRecording: RecordingEntity? = null,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f
)

class AudioPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun playRecording(recording: RecordingEntity) {
        val file = File(recording.filePath)
        if (!file.exists()) {
            return
        }

        // If clicking same recording that is paused, resume
        if (_playbackState.value.currentRecording?.id == recording.id && _playbackState.value.isPaused) {
            resume()
            return
        }

        // Stop current if any
        stop()

        try {
            val player = MediaPlayer()
            player.setDataSource(recording.filePath)
            player.prepare()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val params = PlaybackParams()
                    params.speed = _playbackState.value.playbackSpeed
                    player.playbackParams = params
                } catch (_: Exception) {
                }
            }

            player.setOnCompletionListener {
                stopProgressJob()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    isPaused = false,
                    currentPositionMs = 0L
                )
            }

            player.start()
            mediaPlayer = player

            _playbackState.value = _playbackState.value.copy(
                currentRecording = recording,
                isPlaying = true,
                isPaused = false,
                totalDurationMs = player.duration.toLong(),
                currentPositionMs = 0L
            )

            startProgressJob()
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    stopProgressJob()
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        isPaused = true,
                        currentPositionMs = it.currentPosition.toLong()
                    )
                }
            }
        } catch (_: Exception) {
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                it.start()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = true,
                    isPaused = false
                )
                startProgressJob()
            }
        } catch (_: Exception) {
        }
    }

    fun togglePlayPause(recording: RecordingEntity) {
        val current = _playbackState.value.currentRecording
        if (current?.id == recording.id) {
            if (_playbackState.value.isPlaying) {
                pause()
            } else {
                resume()
            }
        } else {
            playRecording(recording)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
        } catch (_: Exception) {
        }
    }

    fun skipForward(millis: Long = 5000L) {
        mediaPlayer?.let {
            val target = (it.currentPosition + millis).coerceAtMost(it.duration.toLong())
            seekTo(target)
        }
    }

    fun skipBackward(millis: Long = 5000L) {
        mediaPlayer?.let {
            val target = (it.currentPosition - millis).coerceAtLeast(0L)
            seekTo(target)
        }
    }

    fun setSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let {
                    val wasPlaying = it.isPlaying
                    val params = PlaybackParams()
                    params.speed = speed
                    it.playbackParams = params
                    if (!wasPlaying) {
                        it.pause()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun stop() {
        stopProgressJob()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {
        }
        mediaPlayer = null
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isPaused = false,
            currentPositionMs = 0L
        )
    }

    private fun startProgressJob() {
        stopProgressJob()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _playbackState.value = _playbackState.value.copy(
                                currentPositionMs = player.currentPosition.toLong()
                            )
                        }
                    } catch (_: Exception) {
                    }
                }
                delay(100L)
            }
        }
    }

    private fun stopProgressJob() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
