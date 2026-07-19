package com.noxtan.player.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PlaybackStateEntity(
  @PrimaryKey val mediaTitle: String,
  val lastPosition: Int,
  val playbackSpeed: Double,
  val sid: Int,
  val subDelay: Int,
  val subSpeed: Double,
  val secondarySid: Int,
  val secondarySubDelay: Int,
  val aid: Int,
  val audioDelay: Int,
  val customSubUri: String = ""
)
