package com.noxtan.player.data.logic

import androidx.room.withTransaction
import com.noxtan.player.data.local.db.NoxtanDatabase
import com.noxtan.player.data.source.MediaStoreScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

class VideoScannerManager(
  private val database: NoxtanDatabase,
  private val mediaScanner: MediaStoreScanner,
  private val thumbnailManager: ThumbnailManager
) {
  private val videoDao = database.videoDao()
  private val mutex = Mutex()

  suspend fun scan() {
    if (!mutex.tryLock()) {
      return
    }

    try {
      withContext(Dispatchers.IO) {
        val existingVideos = videoDao.getAllVideosList()
        val existingMap = existingVideos.associateBy { it.path } // Path ကို Key သုံးပြီး Map ဆောက်လိုက်ပါသည်
        val thumbMap = existingVideos.associate { it.path to it.thumbnailPath }
        val historyMap = existingVideos.associate { it.path to it.lastPlayedPosition }
        val timestampMap = existingVideos.associate { it.path to it.lastPlayedTimestamp }

        // mediaScanner ထဲကို existingMap ပါ လှမ်းပို့ပေးလိုက်ပါသည်
        val videoList = mediaScanner.scanVideos(existingMap, historyMap, thumbMap).map { video ->
          video.copy(lastPlayedTimestamp = timestampMap[video.path] ?: 0L)
        }

        database.withTransaction {
          val existingPaths = existingVideos.map { it.path }.toSet()
          val newPaths = videoList.map { it.path }.toSet()
          val pathsToDelete = existingPaths - newPaths

          if (pathsToDelete.isNotEmpty()) {
            videoDao.deleteVideosByPaths(pathsToDelete.toList())
          }
          videoDao.insertVideos(videoList)
        }
      }
    } finally {
      mutex.unlock()
    }
  }
}
