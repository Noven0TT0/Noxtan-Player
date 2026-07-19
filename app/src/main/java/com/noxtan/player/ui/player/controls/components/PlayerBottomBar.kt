package com.noxtan.player.ui.player.controls.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noxtan.player.R
import com.noxtan.player.ui.player.VideoAspect
import com.noxtan.player.ui.player.controls.playerControlsEnterAnimationSpec
import com.noxtan.player.ui.player.controls.playerControlsExitAnimationSpec
import dev.vivvvek.seeker.Segment
import kotlinx.collections.immutable.ImmutableList

@Composable
fun PlayerBottomBar(
  controlsShown: Boolean,
  areControlsLocked: Boolean,
  seekBarShown: Boolean,
  reduceMotion: Boolean,
  isLeftHanded: Boolean,
  isLandscape: Boolean,
  position: Float,
  duration: Float,
  remaining: Float,
  readAhead: Float,
  preciseSeeking: Boolean,
  invertDuration: Boolean,
  onInvertDurationClick: () -> Unit,
  chapters: ImmutableList<Segment>,
  onSeekTo: (Float) -> Unit,
  onSeekChangeFinished: () -> Unit,
  onLockControls: () -> Unit,
  onPrevious: () -> Unit,
  onPauseUnpause: () -> Unit,
  onNext: () -> Unit,
  playbackSpeed: Float,
  onSpeedClick: () -> Unit,
  onCycleRotation: () -> Unit,
  aspectRatio: VideoAspect,
  onChangeAspectRatio: (VideoAspect) -> Unit,
  onToggleHandedness: () -> Unit,
  paused: Boolean,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = (controlsShown || seekBarShown) && !areControlsLocked,
    enter = if (!reduceMotion) {
      slideInVertically(playerControlsEnterAnimationSpec()) { it } +
        fadeIn(playerControlsEnterAnimationSpec())
    } else {
      fadeIn(playerControlsEnterAnimationSpec())
    },
    exit = if (!reduceMotion) {
      slideOutVertically(playerControlsExitAnimationSpec()) { it } +
        fadeOut(playerControlsExitAnimationSpec())
    } else {
      fadeOut(playerControlsExitAnimationSpec())
    },
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {

      Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(modifier = Modifier.weight(1f)) {
          SeekbarWithTimers(
            position = position,
            duration = duration,
            remaining = remaining,
            readAheadValue = readAhead,
            onValueChange = onSeekTo,
            onValueChangeFinished = onSeekChangeFinished,
            timersInverted = Pair(false, invertDuration),
            durationTimerOnCLick = onInvertDurationClick,
            positionTimerOnClick = {},
            chapters = chapters,
          )
        }
      }

      AnimatedVisibility(
        visible = controlsShown,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Spacer(modifier = Modifier.height(4.dp))

          if (isLandscape) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (isLeftHanded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  ControlsButton(icon = Icons.Default.SkipPrevious, onClick = onPrevious, title = stringResource(R.string.a11y_player_previous))
                  ControlsButton(icon = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, onClick = onPauseUnpause, title = stringResource(if (paused) R.string.a11y_action_play else R.string.a11y_action_pause))
                  ControlsButton(icon = Icons.Default.SkipNext, onClick = onNext, title = stringResource(R.string.a11y_player_next))
                  ControlsButton(text = String.format(java.util.Locale.US, "%.2fx", playbackSpeed), onClick = onSpeedClick)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  ControlsButton(icon = Icons.Default.LockOpen, onClick = onLockControls, title = stringResource(R.string.a11y_player_lock))
                  ControlsButton(icon = Icons.Default.ScreenRotation, onClick = onCycleRotation, title = stringResource(R.string.a11y_player_rotation))
                  ControlsButton(icon = Icons.Default.AspectRatio, onClick = { onChangeAspectRatio(aspectRatio) }, title = stringResource(R.string.a11y_player_aspect_ratio))
                  ControlsButton(icon = Icons.AutoMirrored.Filled.ArrowForward, onClick = onToggleHandedness, title = stringResource(R.string.a11y_player_handedness))
                }
              } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  ControlsButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onToggleHandedness, title = stringResource(R.string.a11y_player_handedness))
                  ControlsButton(icon = Icons.Default.AspectRatio, onClick = { onChangeAspectRatio(aspectRatio) }, title = stringResource(R.string.a11y_player_aspect_ratio))
                  ControlsButton(icon = Icons.Default.ScreenRotation, onClick = onCycleRotation, title = stringResource(R.string.a11y_player_rotation))
                  ControlsButton(icon = Icons.Default.LockOpen, onClick = onLockControls, title = stringResource(R.string.a11y_player_lock))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  ControlsButton(text = String.format(java.util.Locale.US, "%.2fx", playbackSpeed), onClick = onSpeedClick, title = if (playbackSpeed == 1.0f) stringResource(R.string.a11y_speed_normal) else stringResource(R.string.a11y_speed_custom, "${playbackSpeed}x"))
                  ControlsButton(icon = Icons.Default.SkipPrevious, onClick = onPrevious, title = stringResource(R.string.a11y_player_previous))
                  ControlsButton(icon = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, onClick = onPauseUnpause, title = stringResource(if (paused) R.string.a11y_action_play else R.string.a11y_action_pause))
                  ControlsButton(icon = Icons.Default.SkipNext, onClick = onNext, title = stringResource(R.string.a11y_player_next))
                }
              }
            }
          } else {
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (isLeftHanded) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    ControlsButton(text = String.format(java.util.Locale.US, "%.2fx", playbackSpeed), onClick = onSpeedClick, title = if (playbackSpeed == 1.0f) stringResource(R.string.a11y_speed_normal) else stringResource(R.string.a11y_speed_custom, "${playbackSpeed}x"))
                    ControlsButton(icon = Icons.Default.ScreenRotation, onClick = onCycleRotation, title = stringResource(R.string.a11y_player_rotation))
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    ControlsButton(icon = Icons.Default.AspectRatio, onClick = { onChangeAspectRatio(aspectRatio) }, title = stringResource(R.string.a11y_player_aspect_ratio))
                    ControlsButton(icon = Icons.Default.LockOpen, onClick = onLockControls, title = stringResource(R.string.a11y_player_lock))
                  }
                } else {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    ControlsButton(icon = Icons.Default.LockOpen, onClick = onLockControls, title = stringResource(R.string.a11y_player_lock))
                    ControlsButton(icon = Icons.Default.AspectRatio, onClick = { onChangeAspectRatio(aspectRatio) }, title = stringResource(R.string.a11y_player_aspect_ratio))
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    ControlsButton(icon = Icons.Default.ScreenRotation, onClick = onCycleRotation, title = stringResource(R.string.a11y_player_rotation))
                    ControlsButton(text = String.format(java.util.Locale.US, "%.2fx", playbackSpeed), onClick = onSpeedClick)
                  }
                }
              }

              Spacer(modifier = Modifier.height(4.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                ControlsButton(icon = Icons.Default.SkipPrevious, onClick = onPrevious, title = stringResource(R.string.a11y_player_previous))
                Spacer(modifier = Modifier.width(16.dp))
                ControlsButton(icon = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, onClick = onPauseUnpause, title = stringResource(if (paused) R.string.a11y_action_play else R.string.a11y_action_pause))
                Spacer(modifier = Modifier.width(16.dp))
                ControlsButton(icon = Icons.Default.SkipNext, onClick = onNext, title = stringResource(R.string.a11y_player_next))
              }
            }
          }
        }
      }
    }
  }
}
