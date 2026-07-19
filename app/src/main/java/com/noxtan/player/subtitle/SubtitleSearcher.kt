package com.noxtan.player.subtitle

object SubtitleSearcher {
  fun findNextTimestamp(currentPosMs: Long, subtitles: List<SubtitleItem>): Long? {
    return subtitles.firstOrNull { it.startTime > currentPosMs + 10 }?.startTime
  }

  fun findPreviousTimestamp(currentPosMs: Long, subtitles: List<SubtitleItem>): Long? {
    val activeIndex = subtitles.indexOfFirst { currentPosMs >= it.startTime && currentPosMs <= it.endTime }

    if (activeIndex != -1) {
      val targetIndex = activeIndex - 1
      return if (targetIndex >= 0) subtitles[targetIndex].startTime else null
    } else {
      val lastSub = subtitles.lastOrNull { it.startTime <= currentPosMs }
      return lastSub?.startTime
    }
  }
}
