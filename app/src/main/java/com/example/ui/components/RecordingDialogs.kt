package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecordingEntity
import com.example.ui.theme.RecordingRed
import com.example.util.FormatUtils

@Composable
fun EditRecordingDialog(
    recording: RecordingEntity,
    onDismiss: () -> Unit,
    onSave: (newTitle: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf(recording.title) }
    var notes by remember { mutableStateOf(recording.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("edit_recording_dialog"),
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Text("Edit Recording")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_title_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, notes) },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_edit_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    recording: RecordingEntity,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("delete_confirm_dialog"),
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = RecordingRed)
                Text("Delete Recording?")
            }
        },
        text = {
            Text("Are you sure you want to permanently delete \"${recording.title}\"? The audio file will be removed from your device.")
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = RecordingRed),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RecordingDetailsDialog(
    recording: RecordingEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("recording_details_dialog"),
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, contentDescription = null)
                Text("Recording Details")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("Title", recording.title)
                DetailRow("Recorded At", FormatUtils.formatDate(recording.timestamp))
                DetailRow("Duration", FormatUtils.formatDuration(recording.durationMs))
                DetailRow("File Size", FormatUtils.formatFileSize(recording.fileSizeBytes))
                DetailRow("Trigger Source", recording.triggerSource)
                DetailRow("File Path", recording.filePath, isMonospace = true)
                if (recording.notes.isNotBlank()) {
                    DetailRow("Notes", recording.notes)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String, isMonospace: Boolean = false) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                fontSize = if (isMonospace) 11.sp else 14.sp
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}
