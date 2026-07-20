package com.noxtan.player.data.source

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.noxtan.player.data.local.db.VideoEntity
import com.noxtan.player.engine.NoxtanEngine
import java.io.File

class MediaStoreScanner(
  private val context: Context,
  private val noxtanEngine: NoxtanEngine
) {

  fun scanVideos(
    existingMap: Map<String, VideoEntity>,
    historyMap: Map<String, Long>,
    thumbMap: Map<String, String?>
  ): List<VideoEntity> {
    val videoList = mutableListOf<VideoEntity>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    val projection = arrayOf(
      MediaStore.Video.Media._ID,
      MediaStore.Video.Media.DISPLAY_NAME,
      MediaStore.Video.Media.DATA,
      MediaStore.Video.Media.DURATION,
      MediaStore.Video.Media.SIZE,
      MediaStore.Video.Media.DATE_ADDED,
      MediaStore.Video.Media.MIME_TYPE,
      MediaStore.Video.Media.WIDTH,
      MediaStore.Video.Media.HEIGHT
    )

    val cursor: Cursor? = context.contentResolver.query(
      collection,
      projection,
      null,
      null,
      "${MediaStore.Video.Media.DATE_ADDED} DESC"
    )

    cursor?.use { c ->
      val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
      val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
      val pathCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
      val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
      val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
      val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
      val mimeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
      val wCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
      val hCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

      while (c.moveToNext()) {
        val path = c.getString(pathCol) ?: ""
        if (path.isNotBlank()) {

          val existing = existingMap[path]
          if (existing != null) {
            videoList.add(
              existing.copy(
                id = c.getLong(idCol),
                lastPlayedPosition = historyMap[path] ?: existing.lastPlayedPosition,
                thumbnailPath = thumbMap[path] ?: existing.thumbnailPath
              )
            )
            continue
          }

          if (File(path).exists()) {
            var duration = c.getLong(durCol)

            if (duration <= 0) {
              try {
                duration = noxtanEngine.ffmpegGetDuration(path)
              } catch (e: Exception) {
                duration = 0L
              }
            }

            val w = c.getInt(wCol)
            val h = c.getInt(hCol)
            var resolutionStr = "${w}x${h}"

            if (w <= 0 || h <= 0) {
              runCatching {
                val ffmpegRes = noxtanEngine.ffmpegGetResolution(path)
                if (ffmpegRes != "0x0") resolutionStr = ffmpegRes
              }
            }

            videoList.add(
              VideoEntity(
                id = c.getLong(idCol),
                title = c.getString(nameCol) ?: "Unknown",
                path = path,
                duration = duration,
                size = c.getLong(sizeCol),
                dateAdded = c.getLong(dateCol),
                mimeType = c.getString(mimeCol) ?: "video/mp4",
                resolution = resolutionStr,
                lastPlayedPosition = historyMap[path] ?: 0L,
                lastPlayedTimestamp = 0L,
                thumbnailPath = thumbMap[path]
              )
            )
          }
        }
      }
    }
    return videoList
  }
}
