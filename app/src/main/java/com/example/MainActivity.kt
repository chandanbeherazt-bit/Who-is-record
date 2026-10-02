package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.QuickVoiceTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var isVolumeUpHeldInForeground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickVoiceTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event?.repeatCount == 0 && !isVolumeUpHeldInForeground) {
                isVolumeUpHeldInForeground = true
                viewModel.startHoldSimulation()
            }
            // Consume key repeat to avoid default volume flooding during hold
            if (isVolumeUpHeldInForeground) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val wasHeld = isVolumeUpHeldInForeground
            isVolumeUpHeldInForeground = false
            viewModel.cancelHoldSimulation()
            if (wasHeld) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }
}
