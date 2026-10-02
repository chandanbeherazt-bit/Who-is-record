package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.RecordingStateManager
import com.example.ui.components.ActiveRecordingCard
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EditRecordingDialog
import com.example.ui.components.GuideDialog
import com.example.ui.components.RecordingDetailsDialog
import com.example.ui.components.RecordingItemCard
import com.example.ui.components.StickyAudioPlayer
import com.example.ui.components.VolumeHoldHeroCard
import com.example.ui.theme.AudioBlue
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.RecordingRed
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val elapsedDurationMs by viewModel.elapsedDurationMs.collectAsStateWithLifecycle()
    val liveWaveform by viewModel.liveWaveform.collectAsStateWithLifecycle()
    val volumeUpHoldProgress by viewModel.volumeUpHoldProgress.collectAsStateWithLifecycle()
    val isVolumeUpHeld by viewModel.isVolumeUpHeld.collectAsStateWithLifecycle()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsStateWithLifecycle()
    val simulatedHoldProgress by viewModel.simulatedHoldProgress.collectAsStateWithLifecycle()
    val recordingsList by viewModel.recordingsList.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterFavorites by viewModel.filterOnlyFavorites.collectAsStateWithLifecycle()

    val editingRecording by viewModel.editingRecording.collectAsStateWithLifecycle()
    val recordingToDelete by viewModel.recordingToDelete.collectAsStateWithLifecycle()
    val recordingForDetails by viewModel.recordingForDetails.collectAsStateWithLifecycle()
    val showGuideDialog by viewModel.showGuideDialog.collectAsStateWithLifecycle()

    // Permissions check
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = permissions[Manifest.permission.POST_NOTIFICATIONS] == true ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Refresh accessibility status on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkAccessibilityStatus()
                hasAudioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Collect Toast/Snackbar messages
    LaunchedEffect(Unit) {
        RecordingStateManager.toastEvent.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_screen"),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(AudioBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "QuickVoice",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setShowGuideDialog(true) },
                        modifier = Modifier.testTag("topbar_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "How it works",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        bottomBar = {
            StickyAudioPlayer(
                playbackState = playbackState,
                onTogglePlayPause = { viewModel.togglePlayPause(it) },
                onSeekTo = { viewModel.seekTo(it) },
                onSkipBackward = { viewModel.skipBackward() },
                onSkipForward = { viewModel.skipForward() },
                onSetSpeed = { viewModel.setPlaybackSpeed(it) },
                onShare = { viewModel.shareRecording(context, it) },
                onClosePlayer = { viewModel.stopPlayback() }
            )
        },
        floatingActionButton = {
            if (playbackState.currentRecording == null) {
                FloatingActionButton(
                    onClick = {
                        if (!hasAudioPermission) {
                            val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                arrayOf(Manifest.permission.RECORD_AUDIO)
                            }
                            permissionLauncher.launch(perms)
                        } else {
                            viewModel.toggleRecording("FAB_CLICK")
                        }
                    },
                    containerColor = if (isRecording) RecordingRed else AudioBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(60.dp)
                        .testTag("fab_record_toggle")
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop Recording" else "Record Audio",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Banner (if missing audio or notification)
            if (!hasAudioPermission || !hasNotificationPermission) {
                item(key = "permission_banner") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = WarningOrange
                                )
                                Column {
                                    Text(
                                        text = "Permission Required",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = WarningOrange
                                        )
                                    )
                                    Text(
                                        text = "Microphone is required for background voice recording.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        arrayOf(Manifest.permission.RECORD_AUDIO)
                                    }
                                    permissionLauncher.launch(perms)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                            ) {
                                Text("Grant", color = Color.Black)
                            }
                        }
                    }
                }
            }

            // 1. Volume Up 3-second hold Hero Card
            item(key = "volume_hold_hero") {
                VolumeHoldHeroCard(
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    isRecording = isRecording,
                    volumeUpHoldProgress = volumeUpHoldProgress,
                    isVolumeUpHeld = isVolumeUpHeld,
                    simulatedHoldProgress = simulatedHoldProgress,
                    onStartSimulatedHold = { viewModel.startHoldSimulation() },
                    onCancelSimulatedHold = { viewModel.cancelHoldSimulation() },
                    onOpenGuide = { viewModel.setShowGuideDialog(true) }
                )
            }

            // 2. Live Recording Banner (when active)
            item(key = "active_recording_card") {
                ActiveRecordingCard(
                    isRecording = isRecording,
                    elapsedMs = elapsedDurationMs,
                    liveWaveform = liveWaveform,
                    onStopRecording = { viewModel.toggleRecording("ACTIVE_CARD_STOP") }
                )
            }

            // 3. Search & Filter Bar
            item(key = "search_filter_bar") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search recordings…") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_recordings_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = AudioBlue,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recordings (${recordingsList.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        FilterChip(
                            selected = filterFavorites,
                            onClick = { viewModel.toggleFavoritesFilter() },
                            label = { Text("Favorites") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (filterFavorites) Color(0xFFFFD60A) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AudioBlue.copy(alpha = 0.2f),
                                selectedLabelColor = AudioBlue
                            )
                        )
                    }
                }
            }

            // 4. Recordings List / Empty State
            if (recordingsList.isEmpty()) {
                item(key = "empty_recordings_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp)
                            .testTag("empty_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardVoice,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = if (searchQuery.isNotEmpty() || filterFavorites) "No matching recordings" else "No voice recordings yet",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty() || filterFavorites)
                                    "Try clearing search filters."
                                else
                                    "Hold your physical Volume Up button for 3s\nanywhere to record in background!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            } else {
                items(
                    items = recordingsList,
                    key = { it.id }
                ) { recording ->
                    RecordingItemCard(
                        recording = recording,
                        playbackState = playbackState,
                        onPlayPause = { viewModel.togglePlayPause(recording) },
                        onToggleFavorite = { viewModel.toggleFavorite(recording) },
                        onRename = { viewModel.setEditingRecording(recording) },
                        onViewDetails = { viewModel.setRecordingForDetails(recording) },
                        onShare = { viewModel.shareRecording(context, recording) },
                        onDelete = { viewModel.setRecordingToDelete(recording) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showGuideDialog) {
        GuideDialog(
            isAccessibilityEnabled = isAccessibilityEnabled,
            onDismiss = { viewModel.setShowGuideDialog(false) }
        )
    }

    editingRecording?.let { rec ->
        EditRecordingDialog(
            recording = rec,
            onDismiss = { viewModel.setEditingRecording(null) },
            onSave = { newTitle, notes ->
                viewModel.saveRenamedRecording(rec.id, newTitle, notes)
            }
        )
    }

    recordingToDelete?.let { rec ->
        DeleteConfirmDialog(
            recording = rec,
            onDismiss = { viewModel.setRecordingToDelete(null) },
            onConfirmDelete = { viewModel.confirmDeleteRecording() }
        )
    }

    recordingForDetails?.let { rec ->
        RecordingDetailsDialog(
            recording = rec,
            onDismiss = { viewModel.setRecordingForDetails(null) }
        )
    }
}
