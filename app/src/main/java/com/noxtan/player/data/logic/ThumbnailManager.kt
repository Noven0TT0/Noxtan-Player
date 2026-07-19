package com.noxtan.player.data.logic

import android.os.Process
import com.noxtan.player.data.local.db.VideoDao
import com.noxtan.player.data.local.db.VideoEntity
import com.noxtan.player.data.source.ThumbnailGenerator
import kotlinx.coroutines.yield
import java.io.File

class ThumbnailManager(
  private val thumbnailGenerator: ThumbnailGenerator,
  private val videoDao: VideoDao
) {
  suspend fun generateMissing(videos: List<VideoEntity>) {
    try {
      Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
    } catch (_: Exception) {}

    val missingVideos = videos.filter { it.thumbnailPath == null || !File(it.thumbnailPath).exists() }

    if (missingVideos.isEmpty()) return

    missingVideos.forEach { video ->
      val thumbFile = thumbnailGenerator.generateThumbnailToFile(video)
      if (thumbFile != null) {
        videoDao.updateThumbnailPath(video.path, thumbFile.absolutePath)
      }
      yield()
      kotlinx.coroutines.delay(200)
    }
  }
}
