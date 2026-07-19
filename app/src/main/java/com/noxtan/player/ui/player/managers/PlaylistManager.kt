package com.noxtan.player.ui.player.managers

import android.net.Uri
import com.noxtan.player.data.repository.VideoRepository
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PlaylistManager(
  private val videoRepository: VideoRepository
) {

  suspend fun setupPlaylist(currentPath: String, playFallback: (String) -> Unit) {
    withContext(Dispatchers.IO) {
      try {
        val allVideos = videoRepository.videoDao.getAllVideosList()
        val parentPath = File(currentPath).parent ?: ""

        if (parentPath.isBlank()) {
          withContext(Dispatchers.Main) { playFallback(currentPath) }
          return@withContext
        }

        val siblings = allVideos.filter {
          val parent = File(it.path).parent
          parent != null && parent == parentPath
        }.sortedBy { it.title.lowercase() }

        if (siblings.isEmpty()) {
          withContext(Dispatchers.Main) { playFallback(currentPath) }
          return@withContext
        }

        val currentIndex = siblings.indexOfFirst { it.path == currentPath }
        if (currentIndex == -1) {
          withContext(Dispatchers.Main) { playFallback(currentPath) }
          return@withContext
        }

        withContext(Dispatchers.Main) {
          MPVLib.command("playlist-clear")
          val firstSibling = siblings[0]
          val firstPath = if (firstSibling.path.startsWith("content://") || firstSibling.path.startsWith("file://")) {
            firstSibling.path
          } else {
            Uri.fromFile(File(firstSibling.path)).toString()
          }
          MPVLib.command("loadfile", firstPath, "replace")

          for (i in 1 until siblings.size) {
            val video = siblings[i]
            val formattedPath = if (video.path.startsWith("content://") || video.path.startsWith("file://")) {
              video.path
            } else {
              Uri.fromFile(File(video.path)).toString()
            }
            MPVLib.command("loadfile", formattedPath, "append")
          }

          if (currentIndex > 0) {
            MPVLib.command("playlist-play-index", currentIndex.toString())
          }
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) { playFallback(currentPath) }
      }
    }
  }
}
