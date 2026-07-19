package com.noxtan.player.subtitle

data class SubtitleItem(
  val index: Int,
  val startTime: Long,
  val endTime: Long,
  val text: String
)
