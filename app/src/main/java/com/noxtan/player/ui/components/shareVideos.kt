package com.noxtan.player.ui.components

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

fun shareVideos(context: Context, videos: List<com.noxtan.player.data.local.db.VideoEntity>) {
  if (videos.isEmpty()) return

  val uris = ArrayList<Uri>()
  for (video in videos) {
    if (video.id > 0) {
      val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video.id)
      uris.add(contentUri)
    }
  }

  if (uris.isEmpty()) return

  val shareIntent = if (uris.size == 1) {
    Intent(Intent.ACTION_SEND).apply {
      type = "video/*"
      putExtra(Intent.EXTRA_STREAM, uris.first())
    }
  } else {
    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
      type = "video/*"
      putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    }
  }

  shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  context.startActivity(Intent.createChooser(shareIntent, "Share Video(s)"))
}
