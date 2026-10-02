package com.example.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class QuickRecordTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var observerJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(RecordingStateManager.isRecording.value)

        observerJob?.cancel()
        observerJob = serviceScope.launch {
            RecordingStateManager.isRecording.collect { isRec ->
                updateTileState(isRec)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        observerJob?.cancel()
        observerJob = null
    }

    override fun onClick() {
        super.onClick()
        VoiceRecordingService.toggle(this, "QUICK_SETTINGS_TILE")
    }

    private fun updateTileState(isRecording: Boolean) {
        val tile = qsTile ?: return
        if (isRecording) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Recording…"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Tap to Stop"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Quick Record"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Tap to Start"
            }
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
