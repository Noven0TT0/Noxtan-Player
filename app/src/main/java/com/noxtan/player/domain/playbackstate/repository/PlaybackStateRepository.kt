package com.noxtan.player.domain.playbackstate.repository

import com.noxtan.player.database.entities.PlaybackStateEntity

interface PlaybackStateRepository {

  suspend fun upsert(playbackState: PlaybackStateEntity)

  suspend fun getVideoDataByTitle(mediaTitle: String): PlaybackStateEntity?

  suspend fun clearAllPlaybackStates()
}
