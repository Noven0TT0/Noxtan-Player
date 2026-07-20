package com.noxtan.player.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.noxtan.player.engine.NoxtanEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FFmpegCoilDecoder(
  private val source: ImageSource,
  private val options: Options
) : Decoder {

  override suspend fun decode(): DecodeResult? = thumbnailMutex.withLock {
    withContext(Dispatchers.IO) {
      val videoPath = source.file().toString()
      val context = options.context
      val thumbDir = File(context.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }
      val thumbFile = File(thumbDir, "cached_${videoPath.hashCode()}.jpg")

      if (thumbFile.exists() && thumbFile.length() > 0) {
        val cachedBitmap = android.graphics.BitmapFactory.decodeFile(thumbFile.absolutePath)
        if (cachedBitmap != null) {
          return@withContext DecodeResult(
            image = cachedBitmap.asImage(),
            isSampled = true
          )
        }
      }

      var finalBitmap: Bitmap? = null
      val targetWidth = 320
      val targetHeight = 180

      try {
        if (NoxtanEngine.isAvailable) {
          val noxtanEngine = NoxtanEngine()
          val rotation = noxtanEngine.ffmpegGetRotation(videoPath)
          var bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

          val success = noxtanEngine.ffmpegGetVideoThumbnail(videoPath, bitmap)
          if (success) {
            finalBitmap = bitmap
            if (rotation == 90 || rotation == 270 || rotation == 180) {
              val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
              finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, targetWidth, targetHeight, matrix, true)
              if (finalBitmap != bitmap) {
                bitmap.recycle()
              }
            }
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }

      if (finalBitmap == null) {
        try {
          val retriever = android.media.MediaMetadataRetriever()
          retriever.setDataSource(videoPath)

          val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
          val isLowEnd = activityManager.isLowRamDevice
          val syncOption = if (isLowEnd) android.media.MediaMetadataRetriever.OPTION_PREVIOUS_SYNC else android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC

          val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(1000000L, syncOption, targetWidth, targetHeight)
          } else {
            retriever.getFrameAtTime(1000000L, syncOption)
          }

          rawBitmap?.let { bitmap ->
            val rotationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val rotation = rotationStr?.toInt() ?: 0
            finalBitmap = bitmap

            if (rotation != 0) {
              val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
              finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
              if (finalBitmap != bitmap) {
                bitmap.recycle()
              }
            }
          }
          retriever.release()
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }

      finalBitmap?.let { bitmap ->
        try {
          FileOutputStream(thumbFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
          }
        } catch (e: Exception) {
          e.printStackTrace()
        }

        return@withContext DecodeResult(
          image = bitmap.asImage(),
          isSampled = true
        )
      }

      null
    }
  }

  companion object {
    private val thumbnailMutex = Mutex()
  }

  class Factory : Decoder.Factory {
    override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
      val path = result.source.file().toString()
      val supported = setOf("mkv", "avi", "flv", "ts", "webm", "mp4", "mov", "wmv")
      return if (supported.contains(path.substringAfterLast('.').lowercase())) {
        FFmpegCoilDecoder(result.source, options)
      } else null
    }
  }
}
