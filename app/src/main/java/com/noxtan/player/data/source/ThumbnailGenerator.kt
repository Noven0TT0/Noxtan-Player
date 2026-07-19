package com.noxtan.player.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import com.noxtan.player.data.local.db.VideoEntity
import com.noxtan.player.engine.NoxtanEngine
import java.io.File
import java.io.FileOutputStream

class ThumbnailGenerator(
  private val context: Context,
  private val noxtanEngine: NoxtanEngine
) {

  fun generateThumbnailToFile(video: VideoEntity): File? {
    val thumbDir = File(context.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }
    val thumbFile = File(thumbDir, "thumb_${video.id}.jpg")

    if (thumbFile.exists() && thumbFile.length() > 0) {
      return thumbFile
    }

    try {
      val rotation = noxtanEngine.ffmpegGetRotation(video.path)

      val bitmapWidth = 320
      val bitmapHeight = 180
      var bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)

      val success = noxtanEngine.ffmpegGetVideoThumbnail(video.path, bitmap)

      if (success) {
        if (rotation == 90 || rotation == 270 || rotation == 180) {
          val matrix = Matrix()
          matrix.postRotate(rotation.toFloat())
          val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, matrix, true)
          bitmap.recycle()
          bitmap = rotatedBitmap
        }

        FileOutputStream(thumbFile).use { out ->
          val compressSuccess = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
          return thumbFile
        }
      } else { }
    } catch (e: Exception) { }
    return null
  }
}
