package com.noxtan.player.subtitle

import java.util.regex.Pattern

class SrtParser {
  fun parse(srtContent: String): List<SubtitleItem> {
    val subtitleList = mutableListOf<SubtitleItem>()
    val blocks = srtContent.split(Regex("(\\r?\\n){2,}"))

    for (block in blocks) {
      val lines = block.trim().lines()
      if (lines.size >= 3) {
        try {
          val index = lines[0].trim().toInt()
          val timeLine = lines[1]
          val text = lines.drop(2).joinToString("\n")

          val times = timeLine.split(" --> ")
          if (times.size == 2) {
            val startTime = parseTimeToMs(times[0].trim())
            val endTime = parseTimeToMs(times[1].trim())
            subtitleList.add(SubtitleItem(index, startTime, endTime, text))
          }
        } catch (e: Exception) { e.printStackTrace() }
      }
    }
    return subtitleList
  }

  private fun parseTimeToMs(timeString: String): Long {
    val pattern = Pattern.compile("(\\d+):(\\d+):(\\d+),(\\d+)")
    val matcher = pattern.matcher(timeString)
    if (matcher.find()) {
      val hours = matcher.group(1)!!.toLong()
      val minutes = matcher.group(2)!!.toLong()
      val seconds = matcher.group(3)!!.toLong()
      val millis = matcher.group(4)!!.toLong()
      return (hours * 3600000) + (minutes * 60000) + (seconds * 1000) + millis
    }
    return 0
  }
}
