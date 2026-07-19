package com.noxtan.player.ui.player.controls.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noxtan.player.R
import com.noxtan.player.ui.player.Decoder
import com.noxtan.player.ui.player.controls.TopLeftPlayerControls
import com.noxtan.player.ui.player.controls.TopRightPlayerControls
import com.noxtan.player.ui.player.controls.playerControlsEnterAnimationSpec
import com.noxtan.player.ui.player.controls.playerControlsExitAnimationSpec

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerTopBar(
  controlsShown: Boolean,
  areControlsLocked: Boolean,
  reduceMotion: Boolean,
  mediaTitle: String,
  onBackPress: () -> Unit,
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
  backgroundPlayback: Boolean,
  onBackgroundPlaybackToggle: () -> Unit,
  autoPipMode: Boolean,
  onAutoPipModeToggle: (Boolean) -> Unit,
  sleepTimerTimeRemaining: Int,
  onSleepTimerClick: () -> Unit,
  statisticsPage: Int,
  onStatisticsClick: () -> Unit,
  onVideoFiltersClick: () -> Unit,
  onSwipeProtectorClick: () -> Unit,
  onEyeCareClick: () -> Unit,
  isCinematicMode: Boolean,
  onCinematicModeToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val configuration = LocalConfiguration.current
  val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
  AnimatedVisibility(
    visible = controlsShown && !areControlsLocked,
    enter = if (!reduceMotion) {
      slideInVertically(playerControlsEnterAnimationSpec()) { -it } +
        fadeIn(playerControlsEnterAnimationSpec())
    } else {
      fadeIn(playerControlsEnterAnimationSpec())
    },
    exit = if (!reduceMotion) {
      slideOutVertically(playerControlsExitAnimationSpec()) { -it } +
        fadeOut(playerControlsExitAnimationSpec())
    } else {
      fadeOut(playerControlsExitAnimationSpec())
    },
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TopLeftPlayerControls(
          mediaTitle = mediaTitle,
          onBackClick = onBackPress,
          modifier = Modifier.weight(1f).padding(end = 16.dp)
        )
        TopRightPlayerControls(
          decoder = decoder,
          onDecoderClick = onDecoderClick,
          onDecoderLongClick = onDecoderLongClick,
          isChaptersVisible = isChaptersVisible,
          onChaptersClick = onChaptersClick,
          onSubtitlesClick = onSubtitlesClick,
          onSubtitlesLongClick = onSubtitlesLongClick,
          onAudioClick = onAudioClick,
          onAudioLongClick = onAudioLongClick,
          isMoreExpanded = isMoreExpanded,
          onMoreClick = onMoreClick,
          onMoreLongClick = onMoreLongClick,
        )
      }

      AnimatedVisibility(
        visible = isMoreExpanded,
        enter = expandVertically(
          expandFrom = Alignment.Top,
          animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = 350)),
        exit = shrinkVertically(
          shrinkTowards = Alignment.Top,
          animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = 350)),
        modifier = Modifier.align(Alignment.End)
      ) {
        FlowRow(
          modifier = Modifier
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          maxItemsInEachRow = if (isPortrait) 4 else 8
        ) {
          // ၁။ Background Playback
          IconButton(
            onClick = onBackgroundPlaybackToggle,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = if (backgroundPlayback) Icons.Default.Headset else Icons.Default.HeadsetOff,
              contentDescription = stringResource(R.string.pref_enable_background_playback),
              tint = if (backgroundPlayback) MaterialTheme.colorScheme.primary else Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          // ၂။ Sleep Timer
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
              .height(36.dp)
              .widthIn(min = 36.dp)
              .clip(CircleShape)
              .clickable { onSleepTimerClick() }
              .padding(horizontal = if (sleepTimerTimeRemaining > 0) 12.dp else 0.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Timer,
              contentDescription = stringResource(R.string.timer_title),
              tint = if (sleepTimerTimeRemaining > 0) MaterialTheme.colorScheme.primary else Color.White,
              modifier = Modifier.size(20.dp)
            )
            if (sleepTimerTimeRemaining > 0) {
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = android.text.format.DateUtils.formatElapsedTime(sleepTimerTimeRemaining.toLong()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // ၃။ Statistics
          IconButton(
            onClick = onStatisticsClick,
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.QueryStats,
                contentDescription = stringResource(R.string.player_sheets_stats_page_title),
                tint = if (statisticsPage > 0) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(20.dp)
              )
              if (statisticsPage > 0) {
                Text(
                  text = statisticsPage.toString(),
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.Black,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.offset(y = (-2).dp)
                )
              }
            }
          }

          // ၄။ Video Filters
          IconButton(
            onClick = onVideoFiltersClick,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = stringResource(R.string.player_sheets_video_settings_title),
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          // ၅။ Swipe Protector
          IconButton(
            onClick = onSwipeProtectorClick,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Swipe,
              contentDescription = stringResource(R.string.swipe_protector_title),
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          // ၆။ Eye Care
          IconButton(
            onClick = onEyeCareClick,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = androidx.compose.material.icons.Icons.Rounded.RemoveRedEye,
              contentDescription = stringResource(R.string.eye_care_title),
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          IconButton(
            onClick = { onCinematicModeToggle(!isCinematicMode) },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = androidx.compose.material.icons.Icons.Rounded.AutoAwesome,
              contentDescription = "Cinematic Mode",
              tint = if (isCinematicMode) MaterialTheme.colorScheme.primary else Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          // ၈။ Auto PiP
          IconButton(
            onClick = { onAutoPipModeToggle(!autoPipMode) },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = androidx.compose.material.icons.Icons.Rounded.PictureInPicture,
              contentDescription = stringResource(R.string.a11y_auto_pip),
              tint = if (autoPipMode) MaterialTheme.colorScheme.primary else Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}
