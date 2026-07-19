package com.noxtan.player.ui.player.managers

import android.content.Context
import android.net.Uri
import com.noxtan.player.subtitle.SrtParser
import com.noxtan.player.subtitle.SubtitleItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubtitleSyncManager(
  private val context: Context,
  private val coroutineScope: CoroutineScope,
  private val getCurrentPositionMs: () -> Long
) {
  private var externalSubtitles = mutableListOf<SubtitleItem>()
  private var subtitleSyncJob: Job? = null

  private val _cues = MutableStateFlow<List<String>>(emptyList())
  val cues = _cues.asStateFlow()

  var subtitleDelayMs: Long = 0L

  fun loadManualSubtitle(uri: Uri) {
    if (uri.toString() == "off") return
    coroutineScope.launch(Dispatchers.IO) {
      try {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (content != null) {
          val items = SrtParser().parse(content)

          externalSubtitles.clear()
          externalSubtitles.addAll(items)
          startSubtitleSyncLoop()
        }
      } catch (e: Exception) { }
    }
  }

  private fun startSubtitleSyncLoop() {
    subtitleSyncJob?.cancel()
    subtitleSyncJob = coroutineScope.launch(Dispatchers.Default) {
      var lastShownText: String? = null

      while (true) {
        if (externalSubtitles.isNotEmpty()) {
          val currentPos = getCurrentPositionMs()
          val targetTime = currentPos + subtitleDelayMs

          val activeItems = externalSubtitles.filter {
            targetTime >= it.startTime && targetTime <= it.endTime
          }

          val currentText = activeItems.joinToString("\n") { it.text }

          if (currentText != lastShownText) {
            lastShownText = currentText
            _cues.value = activeItems.map { it.text }
          }
        }
        delay(100)
      }
    }
  }

  fun stopAndClear() {
    subtitleSyncJob?.cancel()
    externalSubtitles.clear()
    _cues.value = emptyList()
  }

  fun getSubtitles(): List<SubtitleItem> {
    return externalSubtitles.toList()
  }
}
