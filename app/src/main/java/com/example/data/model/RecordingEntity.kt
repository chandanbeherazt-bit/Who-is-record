package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val sampleAmplitudes: String = "", // Comma-separated normalized floats (0.0 to 1.0)
    val notes: String = "",
    val isFavorite: Boolean = false,
    val triggerSource: String = "VOLUME_KEY" // "VOLUME_KEY", "APP_BUTTON", "QUICK_SETTINGS"
)
