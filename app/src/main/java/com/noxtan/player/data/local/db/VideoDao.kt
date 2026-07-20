package com.noxtan.player.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
  @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
  fun getAllVideos(): Flow<List<VideoEntity>>

  @Query("SELECT * FROM videos")
  suspend fun getAllVideosList(): List<VideoEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVideos(videos: List<VideoEntity>)

  @Query("DELETE FROM videos")
  suspend fun clearAll()

  @Query("SELECT * FROM videos WHERE title LIKE '%' || :query || '%'")
  fun searchVideos(query: String): Flow<List<VideoEntity>>

  @Query("UPDATE videos SET lastPlayedPosition = :position, markState = CASE WHEN markState = 'NEW' THEN 'NONE' ELSE markState END WHERE path = :path")
  suspend fun updatePlaybackPosition(path: String, position: Long)

  @Query("SELECT lastPlayedPosition FROM videos WHERE path = :path")
  suspend fun getLastPlayedPosition(path: String): Long?

  @Query("UPDATE videos SET thumbnailPath = :thumbPath WHERE path = :videoPath")
  suspend fun updateThumbnailPath(videoPath: String, thumbPath: String)

  @Query("SELECT * FROM videos WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 1")
  fun getGlobalRecentVideo(): Flow<VideoEntity?>

  @Query("SELECT * FROM videos WHERE path LIKE :folderPath || '/%' AND lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 1")
  fun getRecentVideoInFolder(folderPath: String): Flow<VideoEntity?>

  @Query("UPDATE videos SET lastPlayedPosition = :position, lastPlayedTimestamp = :timestamp, markState = CASE WHEN markState = 'NEW' THEN 'NONE' ELSE markState END WHERE path = :path")
  suspend fun updatePlaybackStatus(path: String, position: Long, timestamp: Long)

  @Query("DELETE FROM videos WHERE path IN (:paths)")
  suspend fun deleteVideosByPaths(paths: List<String>)

  @Query("UPDATE videos SET lastPlayedPosition = 0, lastPlayedTimestamp = 0")
  suspend fun clearAllPlaybackHistory()

  @Query("UPDATE videos SET markState = :state WHERE path IN (:paths)")
  suspend fun updateMarkState(paths: List<String>, state: String)

  @Query("UPDATE videos SET markState = :state, lastPlayedPosition = 0, lastPlayedTimestamp = 0 WHERE path IN (:paths)")
  suspend fun updateMarkStateAndResetProgress(paths: List<String>, state: String)
}
