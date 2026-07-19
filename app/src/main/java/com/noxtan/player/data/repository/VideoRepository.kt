package com.noxtan.player.data.repository

import android.content.Context
import com.noxtan.player.data.local.db.NoxtanDatabase
import com.noxtan.player.data.local.db.VideoDao
import com.noxtan.player.data.logic.MediaObserver
import com.noxtan.player.data.logic.ThumbnailManager
import com.noxtan.player.data.logic.VideoScannerManager
import com.noxtan.player.data.source.MediaStoreScanner
import com.noxtan.player.data.source.ThumbnailGenerator
import com.noxtan.player.engine.NoxtanEngine
import kotlinx.coroutines.flow.Flow

class VideoRepository(private val context: Context, private val database: NoxtanDatabase) {

  val videoDao: VideoDao = database.videoDao()
  private val mediaObserver = MediaObserver(context)

  private val noxtanEngine = NoxtanEngine()

  private val thumbnailManager = ThumbnailManager(
    ThumbnailGenerator(context, noxtanEngine),
    videoDao
  )

  private val videoScanner = VideoScannerManager(
    database,
    MediaStoreScanner(context, noxtanEngine),
    thumbnailManager
  )

  val allVideos = videoDao.getAllVideos()

  fun searchVideos(query: String) = videoDao.searchVideos(query)

  suspend fun savePlaybackPosition(path: String, position: Long) =
    videoDao.updatePlaybackPosition(path, position)

  suspend fun getLastPlayedPosition(path: String) =
    videoDao.getLastPlayedPosition(path) ?: 0L

  suspend fun savePlaybackStatus(path: String, position: Long) {
    videoDao.updatePlaybackStatus(path, position, System.currentTimeMillis())
  }

  fun getMediaStoreChanges(): Flow<Unit> = mediaObserver.observeChanges()

  suspend fun scanDeviceForVideos() = videoScanner.scan()

  fun getGlobalRecentVideo() = videoDao.getGlobalRecentVideo()

  fun getRecentVideoInFolder(folderPath: String) = videoDao.getRecentVideoInFolder(folderPath)

  suspend fun clearAllPlaybackHistory() = videoDao.clearAllPlaybackHistory()

  suspend fun generateMissingThumbnails(videos: List<com.noxtan.player.data.local.db.VideoEntity>) {
    thumbnailManager.generateMissing(videos)
  }
}
