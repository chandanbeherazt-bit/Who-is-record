package com.example.data.repository

import com.example.data.dao.RecordingDao
import com.example.data.model.RecordingEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

class RecordingRepository(private val recordingDao: RecordingDao) {

    val allRecordings: Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()
    val favoriteRecordings: Flow<List<RecordingEntity>> = recordingDao.getFavoriteRecordings()
    val recordingsCount: Flow<Int> = recordingDao.getRecordingsCount()

    fun getRecordingById(id: Long): Flow<RecordingEntity?> = recordingDao.getRecordingById(id)

    suspend fun insert(recording: RecordingEntity): Long = recordingDao.insertRecording(recording)

    suspend fun update(recording: RecordingEntity) = recordingDao.updateRecording(recording)

    suspend fun delete(recording: RecordingEntity) {
        // Also delete file on disk if exists
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
        recordingDao.deleteRecording(recording)
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) =
        recordingDao.setFavorite(id, isFavorite)

    suspend fun rename(id: Long, newTitle: String) =
        recordingDao.renameRecording(id, newTitle)

    suspend fun updateNotes(id: Long, notes: String) =
        recordingDao.updateNotes(id, notes)
}
