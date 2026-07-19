package com.noxtan.player.ui.player.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.noxtan.player.ui.player.Decoder
import com.noxtan.player.ui.player.controls.components.ControlsButton
import androidx.compose.ui.res.stringResource
import com.noxtan.player.R

@Composable
fun TopRightPlayerControls(
  decoder: Decoder,
  onDecoderClick: () -> Unit,
  onDecoderLongClick: () -> Unit,
  isChaptersVisible: Boolean,
  onChaptersClick: () -> Unit,
  onSubtitlesClick: () -> Unit,
  onSubtitlesLongClick: () -> Unit,
  onAudioClick: () -> Unit,
  onAudioLongClick: () -> Unit,
  isMoreExpanded: Boolean,
  onMoreClick: () -> Unit,
  onMoreLongClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    ControlsButton(
      decoder.title,
      onClick = onDecoderClick,
      onLongClick = onDecoderLongClick,
    )
    if (isChaptersVisible) {
      ControlsButton(
        icon = Icons.Default.Bookmarks,
        onClick = onChaptersClick,
        title = stringResource(R.string.a11y_player_chapters)
      )
    }
    ControlsButton(
      icon = Icons.Default.Subtitles,
      onClick = onSubtitlesClick,
      onLongClick = onSubtitlesLongClick,
      title = stringResource(R.string.a11y_player_subtitles)
    )
    ControlsButton(
      icon = Icons.Default.Audiotrack,
      onClick = onAudioClick,
      onLongClick = onAudioLongClick,
      title = stringResource(R.string.a11y_player_audio)
    )
    ControlsButton(
      icon = if (isMoreExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.MoreVert,
      onClick = onMoreClick,
      onLongClick = onMoreLongClick,
      title = stringResource(R.string.a11y_player_more)
    )
  }
}
