package com.noxtan.player.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FFmpegCoilDecoder(
  private val source: ImageSource,
  private val options: Options
) : Decoder {

  override suspend fun decode(): DecodeResult? = withContext(Dispatchers.IO) {
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

    try {
      val retriever = MediaMetadataRetriever()
      retriever.setDataSource(videoPath)

      val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
      val isLowEnd = activityManager.isLowRamDevice

      val syncOption = if (isLowEnd) MediaMetadataRetriever.OPTION_PREVIOUS_SYNC else MediaMetadataRetriever.OPTION_CLOSEST_SYNC
      val quality = if (isLowEnd) 70 else 85

      var rawBitmap = retriever.getFrameAtTime(1000000L, syncOption)
        ?: retriever.frameAtTime

      rawBitmap?.let { bitmap ->
        val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        val rotation = rotationStr?.toInt() ?: 0
        var finalBitmap = bitmap

        if (rotation != 0) {
          val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
          finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
          if (finalBitmap != bitmap) bitmap.recycle()
        }

        try {
          FileOutputStream(thumbFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
          }
        } catch (e: Exception) {
          e.printStackTrace()
        }

        retriever.release()

        return@withContext DecodeResult(
          image = finalBitmap.asImage(),
          isSampled = true
        )
      }
      retriever.release()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    null
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
