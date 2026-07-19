package com.noxtan.player.database.repository

import com.noxtan.player.database.PlaybackDatabase
import com.noxtan.player.database.entities.PlaybackStateEntity
import com.noxtan.player.domain.playbackstate.repository.PlaybackStateRepository

class PlaybackStateRepositoryImpl(
  private val database: PlaybackDatabase
) : PlaybackStateRepository {
  override suspend fun upsert(playbackState: PlaybackStateEntity) {
    database.videoDataDao().upsert(playbackState)
  }

  override suspend fun getVideoDataByTitle(mediaTitle: String): PlaybackStateEntity? {
    return database.videoDataDao().getVideoDataByTitle(mediaTitle)
  }

  override suspend fun clearAllPlaybackStates() {
    database.videoDataDao().clearAllPlaybackStates()
  }
}
