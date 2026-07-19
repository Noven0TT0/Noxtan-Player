package com.noxtan.player.ui.player.controls

import android.net.Uri
import androidx.compose.runtime.Composable
import com.noxtan.player.ui.player.Decoder
import com.noxtan.player.ui.player.Panels
import com.noxtan.player.ui.player.Sheets
import com.noxtan.player.ui.player.TrackNode
import com.noxtan.player.ui.player.controls.components.sheets.ChaptersSheet
import com.noxtan.player.ui.player.controls.components.sheets.DecodersSheet
import com.noxtan.player.ui.player.controls.components.sheets.MoreSheet
import com.noxtan.player.ui.player.controls.components.sheets.PlaybackSpeedSheet
import dev.vivvvek.seeker.Segment
import kotlinx.collections.immutable.ImmutableList

@Composable
fun PlayerSheets(
  sheetShown: Sheets,

  subtitles: ImmutableList<TrackNode>,
  onAddSubtitle: (Uri) -> Unit,
  onSelectSubtitle: (Int) -> Unit,
  audioTracks: ImmutableList<TrackNode>,
  onAddAudio: (Uri) -> Unit,
  onSelectAudio: (TrackNode) -> Unit,
  chapter: Segment?,
  chapters: ImmutableList<Segment>,
  onSeekToChapter: (Int) -> Unit,
  decoder: Decoder,
  onUpdateDecoder: (Decoder) -> Unit,
  speed: Float,
  speedPresets: List<Float>,
  onSpeedChange: (Float) -> Unit,
  onAddSpeedPreset: (Float) -> Unit,
  onRemoveSpeedPreset: (Float) -> Unit,
  onResetSpeedPresets: () -> Unit,
  onMakeDefaultSpeed: (Float) -> Unit,
  onResetDefaultSpeed: () -> Unit,
  sleepTimerTimeRemaining: Int,
  onStartSleepTimer: (Int) -> Unit,

  onOpenPanel: (Panels) -> Unit,
  onDismissRequest: () -> Unit,
) {
  when (sheetShown) {
    Sheets.None -> {}

    Sheets.Chapters -> {
      if (chapter == null) return
      ChaptersSheet(
        chapters,
        currentChapter = chapter,
        onClick = { onSeekToChapter(chapters.indexOf(it)) },
        onDismissRequest,
      )
    }

    Sheets.Decoders -> {
      DecodersSheet(
        selectedDecoder = decoder,
        onSelect = onUpdateDecoder,
        onDismissRequest,
      )
    }

    Sheets.More -> {
      MoreSheet(
        remainingTime = sleepTimerTimeRemaining,
        onStartTimer = onStartSleepTimer,
        onDismissRequest = onDismissRequest,
        onEnterFiltersPanel = { onOpenPanel(Panels.VideoFilters) },
        onEnterSwipeProtector = { onOpenPanel(Panels.SwipeProtector) },
        onEnterEyeCare = { onOpenPanel(Panels.EyeCare) }
      )
    }

    Sheets.PlaybackSpeed -> {
      PlaybackSpeedSheet(
        speed,
        onSpeedChange = onSpeedChange,
        speedPresets = speedPresets,
        onAddSpeedPreset = onAddSpeedPreset,
        onRemoveSpeedPreset = onRemoveSpeedPreset,
        onResetPresets = onResetSpeedPresets,
        onMakeDefault = onMakeDefaultSpeed,
        onResetDefault = onResetDefaultSpeed,
        onDismissRequest = onDismissRequest,
      )
    }
  }
}
