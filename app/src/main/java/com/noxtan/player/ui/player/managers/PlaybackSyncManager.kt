package com.noxtan.player.ui.player.managers

import com.noxtan.player.data.repository.VideoRepository
import com.noxtan.player.database.entities.PlaybackStateEntity
import com.noxtan.player.domain.playbackstate.repository.PlaybackStateRepository
import com.noxtan.player.preferences.AudioPreferences
import com.noxtan.player.preferences.PlayerPreferences
import com.noxtan.player.preferences.SubtitlesPreferences
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaybackSyncManager(
  private val playbackStateRepository: PlaybackStateRepository,
  private val videoRepository: VideoRepository,
  private val playerPreferences: PlayerPreferences,
  private val subtitlesPreferences: SubtitlesPreferences,
  private val audioPreferences: AudioPreferences
) {

  suspend fun savePlaybackState(
    mediaTitle: String,
    videoPath: String,
    currentPos: Int,
    duration: Int,
    lastLoadedTimestamp: Long,
    playbackSpeed: Double,
    sid: Int,
    subDelay: Int,
    subSpeed: Double,
    secondarySid: Int,
    secondarySubDelay: Int,
    aid: Int,
    audioDelay: Int,
    customSubUri: String
  ) = withContext(Dispatchers.IO) {
    if (mediaTitle.isBlank()) return@withContext

    val timeLoaded = System.currentTimeMillis() - lastLoadedTimestamp
    val isIntermediateTransition = timeLoaded < 1500

    val oldState = playbackStateRepository.getVideoDataByTitle(mediaTitle)
    val targetPosition = if (playerPreferences.savePositionOnQuit.get()) {
      if (isIntermediateTransition) oldState?.lastPosition ?: 0
      else if (currentPos >= duration - 5 && duration > 0) 0
      else if (currentPos > 3) currentPos else 0
    } else oldState?.lastPosition ?: 0

    playbackStateRepository.upsert(
      PlaybackStateEntity(
        mediaTitle = mediaTitle,
        lastPosition = targetPosition,
        playbackSpeed = playbackSpeed,
        sid = sid,
        subDelay = subDelay,
        subSpeed = subSpeed,
        secondarySid = secondarySid,
        secondarySubDelay = secondarySubDelay,
        aid = aid,
        audioDelay = audioDelay,
        customSubUri = customSubUri
      )
    )

    if (videoPath.isNotBlank()) {
      val dbPath = videoPath.removePrefix("file://")
      videoRepository.savePlaybackStatus(dbPath, targetPosition * 1000L)
    }
  }

  suspend fun loadPlaybackState(mediaTitle: String, applyState: (PlaybackStateEntity?) -> Unit) = withContext(Dispatchers.IO) {
    if (mediaTitle.isBlank()) return@withContext
    val state = playbackStateRepository.getVideoDataByTitle(mediaTitle)

    val getDelay: (Int, Int?) -> Double = { preferenceDelay, stateDelay -> (stateDelay ?: preferenceDelay) / 1000.0 }
    val subDelay = getDelay(subtitlesPreferences.defaultSubDelay.get(), state?.subDelay)
    val secondarySubDelay = getDelay(subtitlesPreferences.defaultSecondarySubDelay.get(), state?.secondarySubDelay)
    val audioDelay = getDelay(audioPreferences.defaultAudioDelay.get(), state?.audioDelay)

    withContext(Dispatchers.Main) {
      applyState(state)
      MPVLib.setPropertyDouble("sub-delay", subDelay)
      MPVLib.setPropertyDouble("secondary-sub-delay", secondarySubDelay)
      MPVLib.setPropertyDouble("audio-delay", audioDelay)

      state?.let {
        MPVLib.setPropertyDouble("sub-speed", it.subSpeed)
        MPVLib.setPropertyDouble("speed", it.playbackSpeed)
      }

      if (playerPreferences.savePositionOnQuit.get()) {
        state?.lastPosition?.let {
          if (it > 0) MPVLib.setPropertyDouble("time-pos", it.toDouble())
        }
      }
    }
  }
}
