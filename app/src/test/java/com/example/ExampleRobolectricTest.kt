package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.RecordingEntity
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("QuickVoice", appName)
    }

    @Test
    fun `formatDuration formats minutes and seconds correctly`() {
        val duration = 65000L // 1 min 5 sec
        val formatted = FormatUtils.formatDuration(duration)
        assertEquals("01:05", formatted)
    }

    @Test
    fun `formatDuration formats hours correctly`() {
        val duration = 3661000L // 1 hour 1 min 1 sec
        val formatted = FormatUtils.formatDuration(duration)
        assertEquals("01:01:01", formatted)
    }

    @Test
    fun `formatFileSize handles KB and MB`() {
        assertEquals("500 KB", FormatUtils.formatFileSize(512000L))
        assertEquals("2.0 MB", FormatUtils.formatFileSize(2097152L))
    }

    @Test
    fun `recordingEntity model creation`() {
        val entity = RecordingEntity(
            id = 1L,
            title = "Test Voice Note",
            filePath = "/path/to/test.m4a",
            durationMs = 5000L,
            fileSizeBytes = 1024L
        )
        assertNotNull(entity)
        assertEquals("Test Voice Note", entity.title)
        assertEquals(5000L, entity.durationMs)
    }
}
