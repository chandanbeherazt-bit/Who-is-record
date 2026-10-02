package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.RecordingEntity
import com.example.data.repository.RecordingRepository
import com.example.playback.AudioPlayerManager
import com.example.playback.PlaybackState
import com.example.service.RecordingStateManager
import com.example.service.VoiceRecordingService
import com.example.util.AccessibilityUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecordingRepository
    val playerManager = AudioPlayerManager(application)

    val isRecording: StateFlow<Boolean> = RecordingStateManager.isRecording
    val elapsedDurationMs: StateFlow<Long> = RecordingStateManager.elapsedDurationMs
    val liveWaveform: StateFlow<List<Float>> = RecordingStateManager.liveWaveform
    val volumeUpHoldProgress: StateFlow<Float> = RecordingStateManager.volumeUpHoldProgress
    val isVolumeUpHeld: StateFlow<Boolean> = RecordingStateManager.isVolumeUpHeld
    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterOnlyFavorites = MutableStateFlow(false)
    val filterOnlyFavorites: StateFlow<Boolean> = _filterOnlyFavorites.asStateFlow()

    // Interactive button simulator for holding volume up button in UI
    private val _simulatedHoldProgress = MutableStateFlow(0f)
    val simulatedHoldProgress: StateFlow<Float> = _simulatedHoldProgress.asStateFlow()
    private var simulatedHoldJob: Job? = null

    // Dialog state management
    private val _editingRecording = MutableStateFlow<RecordingEntity?>(null)
    val editingRecording: StateFlow<RecordingEntity?> = _editingRecording.asStateFlow()

    private val _recordingToDelete = MutableStateFlow<RecordingEntity?>(null)
    val recordingToDelete: StateFlow<RecordingEntity?> = _recordingToDelete.asStateFlow()

    private val _recordingForDetails = MutableStateFlow<RecordingEntity?>(null)
    val recordingForDetails: StateFlow<RecordingEntity?> = _recordingForDetails.asStateFlow()

    private val _showGuideDialog = MutableStateFlow(false)
    val showGuideDialog: StateFlow<Boolean> = _showGuideDialog.asStateFlow()

    val recordingsList: StateFlow<List<RecordingEntity>>

    init {
        val db = AppDatabase.getInstance(application)
        repository = RecordingRepository(db.recordingDao())

        recordingsList = combine(
            repository.allRecordings,
            _searchQuery,
            _filterOnlyFavorites
        ) { list, query, favOnly ->
            list.filter { item ->
                val matchesQuery = query.isBlank() ||
                        item.title.contains(query, ignoreCase = true) ||
                        item.notes.contains(query, ignoreCase = true)
                val matchesFav = !favOnly || item.isFavorite
                matchesQuery && matchesFav
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        checkAccessibilityStatus()

        viewModelScope.launch {
            RecordingStateManager.isAccessibilityEnabled.collect {
                _isAccessibilityEnabled.value = it || AccessibilityUtils.isAccessibilityServiceEnabled(getApplication())
            }
        }
    }

    fun checkAccessibilityStatus() {
        _isAccessibilityEnabled.value = AccessibilityUtils.isAccessibilityServiceEnabled(getApplication())
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesFilter() {
        _filterOnlyFavorites.value = !_filterOnlyFavorites.value
    }

    // Toggle recording directly
    fun toggleRecording(source: String = "MANUAL_UI") {
        val app = getApplication<Application>()
        if (isRecording.value) {
            VoiceRecordingService.stop(app)
        } else {
            VoiceRecordingService.start(app, source)
        }
    }

    // Simulate Volume Up 3-second hold on screen or from hardware key in foreground
    fun startHoldSimulation() {
        simulatedHoldJob?.cancel()
        val app = getApplication<Application>()
        val startTime = System.currentTimeMillis()
        simulatedHoldJob = viewModelScope.launch {
            RecordingStateManager.setVolumeHold(true, 0f)
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / 3000f).coerceIn(0f, 1f)
                _simulatedHoldProgress.value = progress
                RecordingStateManager.setVolumeHold(true, progress)

                if (elapsed >= 3000L) {
                    toggleRecording("VOLUME_UP_SIMULATED_HOLD_3S")
                    _simulatedHoldProgress.value = 0f
                    RecordingStateManager.setVolumeHold(false, 0f)
                    break
                }
                delay(40L)
            }
        }
    }

    fun cancelHoldSimulation() {
        simulatedHoldJob?.cancel()
        simulatedHoldJob = null
        _simulatedHoldProgress.value = 0f
        RecordingStateManager.setVolumeHold(false, 0f)
    }

    // Player controls
    fun togglePlayPause(recording: RecordingEntity) {
        playerManager.togglePlayPause(recording)
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun skipForward() = playerManager.skipForward(5000L)
    fun skipBackward() = playerManager.skipBackward(5000L)
    fun setPlaybackSpeed(speed: Float) = playerManager.setSpeed(speed)
    fun stopPlayback() = playerManager.stop()

    // Database actions
    fun toggleFavorite(recording: RecordingEntity) {
        viewModelScope.launch {
            repository.setFavorite(recording.id, !recording.isFavorite)
        }
    }

    fun setEditingRecording(recording: RecordingEntity?) {
        _editingRecording.value = recording
    }

    fun saveRenamedRecording(id: Long, newTitle: String, notes: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) {
                repository.rename(id, newTitle.trim())
            }
            repository.updateNotes(id, notes.trim())
            _editingRecording.value = null
        }
    }

    fun setRecordingToDelete(recording: RecordingEntity?) {
        _recordingToDelete.value = recording
    }

    fun confirmDeleteRecording() {
        val rec = _recordingToDelete.value ?: return
        if (playbackState.value.currentRecording?.id == rec.id) {
            playerManager.stop()
        }
        viewModelScope.launch {
            repository.delete(rec)
            _recordingToDelete.value = null
        }
    }

    fun setRecordingForDetails(recording: RecordingEntity?) {
        _recordingForDetails.value = recording
    }

    fun setShowGuideDialog(show: Boolean) {
        _showGuideDialog.value = show
    }

    fun shareRecording(context: Context, recording: RecordingEntity) {
        try {
            val file = File(recording.filePath)
            if (!file.exists()) return

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, recording.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share voice recording"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
