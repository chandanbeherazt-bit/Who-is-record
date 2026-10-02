package com.example.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AudioBlue
import com.example.ui.theme.AudioTeal
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.RecordingRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange

@Composable
fun VolumeHoldHeroCard(
    isAccessibilityEnabled: Boolean,
    isRecording: Boolean,
    volumeUpHoldProgress: Float,
    isVolumeUpHeld: Boolean,
    simulatedHoldProgress: Float,
    onStartSimulatedHold: () -> Unit,
    onCancelSimulatedHold: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val combinedProgress = if (simulatedHoldProgress > 0f) simulatedHoldProgress else volumeUpHoldProgress
    val animatedProgress by animateFloatAsState(targetValue = combinedProgress, label = "holdProgress")

    val isHoldingActive = isVolumeUpHeld || simulatedHoldProgress > 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("volume_hold_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Status badge & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AudioBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Volume Up Trigger",
                            tint = AudioBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Volume Up 3s Hold",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Background Audio Trigger",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Status pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAccessibilityEnabled) SuccessGreen.copy(alpha = 0.15f) else WarningOrange.copy(alpha = 0.15f),
                    border = null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isAccessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isAccessibilityEnabled) SuccessGreen else WarningOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isAccessibilityEnabled) "Active" else "Setup Needed",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isAccessibilityEnabled) SuccessGreen else WarningOrange
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Explanation & hold trigger visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hold [Vol Up] for 3 seconds",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHoldingActive) {
                                "Holding Volume Up… ${(animatedProgress * 3.0f).let { String.format("%.1fs / 3.0s", it) }}"
                            } else if (isRecording) {
                                "Recording in background! Hold 3s again to stop."
                            } else {
                                "Works globally in any app, home screen, or background."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isHoldingActive) AudioTeal else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Circular hold progress visualizer
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(64.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(64.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 5.dp
                        )
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(64.dp),
                            color = if (isRecording) RecordingRed else AudioTeal,
                            strokeWidth = 5.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (animatedProgress > 0f) "${(animatedProgress * 100).toInt()}%" else "3s",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (animatedProgress > 0f) AudioTeal else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action row: On-Screen Hold Simulator + Accessibility / Guide Setup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Interactive Hold Simulator button (Touch & Hold for 3s)
                Surface(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onStartSimulatedHold()
                                    tryAwaitRelease()
                                    onCancelSimulatedHold()
                                }
                            )
                        }
                        .testTag("hold_test_button"),
                    color = if (isHoldingActive) AudioBlue.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = null
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardVoice,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isHoldingActive) "Holding… (Keep Pressed)" else "Touch & Hold (3s Test)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // If not enabled, show button to enable accessibility service
                if (!isAccessibilityEnabled) {
                    FilledTonalButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("enable_accessibility_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = WarningOrange.copy(alpha = 0.2f),
                            contentColor = WarningOrange
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Enable",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onOpenGuide,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(46.dp)
                            .testTag("how_it_works_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Guide")
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !isAccessibilityEnabled) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "💡 Tip: To detect Volume Up while app is in background, enable 'QuickVoice' in Accessibility Settings > Installed Apps.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = WarningOrange.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
