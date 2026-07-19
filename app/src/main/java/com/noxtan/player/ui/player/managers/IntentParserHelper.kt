package com.noxtan.player.ui.player.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.net.toUri
import com.noxtan.player.ui.player.openContentFd
import com.noxtan.player.ui.player.resolveUri
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils

class IntentParserHelper(private val context: Context) {

  fun getPlayableUri(intent: Intent): String? {
    val uri = parsePathFromIntent(intent)
    return if (uri?.startsWith("content://") == true) uri.toUri().openContentFd(context) else uri
  }

  fun parsePathFromIntent(intent: Intent): String? {
    return when (intent.action) {
      Intent.ACTION_VIEW -> intent.data?.resolveUri(context) ?: intent.getStringExtra("uri")
      Intent.ACTION_SEND -> {
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
          intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!.resolveUri(context)
        } else {
          intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            val uri = it.trim().toUri()
            if (uri.isHierarchical && !uri.isRelative) uri.resolveUri(context) else null
          }
        }
      }
      else -> intent.getStringExtra("uri")
    }
  }

  fun getFileName(intent: Intent): String {
    val uri = if (intent.type == "text/plain") {
      intent.getStringExtra(Intent.EXTRA_TEXT)!!.toUri()
    } else {
      (intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM))
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && uri != null) {
      val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null)
      if (cursor?.moveToFirst() == true) return cursor.getString(0).also { cursor.close() }
    }
    return uri?.lastPathSegment?.substringAfterLast("/") ?: uri?.path ?: ""
  }

  fun setIntentExtras(extras: Bundle?) {
    if (extras == null) return
    extras.getString("title")?.let { MPVLib.setPropertyString("force-media-title", it) }
    MPVLib.setPropertyInt("time-pos", extras.getInt("position", 0) / 1000)

    if (extras.containsKey("subs")) {
      val subList = Utils.getParcelableArray<Uri>(extras, "subs")
      val subsToEnable = Utils.getParcelableArray<Uri>(extras, "subs.enable")
      for (suburi in subList) {
        val subfile = suburi.resolveUri(context) ?: continue
        val flag = if (subsToEnable.any { it == suburi }) "select" else "auto"
        MPVLib.command("sub-add", subfile, flag)
      }
    }

    extras.getStringArray("headers")?.let { headers ->
      if (headers[0].startsWith("User-Agent", true)) MPVLib.setPropertyString("user-agent", headers[1])
      val headersString = headers.asSequence().drop(2).chunked(2).associate { it[0] to it[1] }
        .map { "${it.key}: ${it.value.replace(",", "\\,")}" }.joinToString(",")
      MPVLib.setPropertyString("http-header-fields", headersString)
    }
  }
}
