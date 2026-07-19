package com.noxtan.player.engine

import android.graphics.Bitmap

class NoxtanEngine {
  companion object {
    var isAvailable: Boolean = false
      private set

    init {
      try {
        System.loadLibrary("noxtanplayer_engine")
        isAvailable = true
      } catch (e: UnsatisfiedLinkError) {
        isAvailable = false
      }
    }
  }

  external fun ffmpegGetDuration(path: String): Long
  external fun ffmpegGetRotation(path: String): Int
  external fun ffmpegGetVideoThumbnail(videoPath: String, bitmap: Bitmap): Boolean
  external fun getFFmpegVersion(): String

  external fun ffmpegGetResolution(path: String): String
}
