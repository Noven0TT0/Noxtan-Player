package com.noxtan.player.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
  @PrimaryKey val id: Long,
  val title: String,
  val path: String,
  val duration: Long,
  val size: Long,
  val dateAdded: Long,
  val mimeType: String,
  val resolution: String,
  val lastPlayedPosition: Long = 0L,
  val lastPlayedTimestamp: Long = 0L,
  val thumbnailPath: String? = null,
  val markState: String = "NONE"
)
