package com.noxtan.player.ui.player.controls.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.noxtan.player.R
import com.noxtan.player.ui.player.controls.playerControlsEnterAnimationSpec
import com.noxtan.player.ui.player.controls.playerControlsExitAnimationSpec
import `is`.xyz.mpv.Utils
import kotlin.math.abs

@Composable
fun PlayerCenterControls(
  controlsShown: Boolean,
  areControlsLocked: Boolean,
  gestureSeekAmount: Pair<Int, Int>?,
  pausedForCache: Boolean,
  showLoadingCircle: Boolean,
  paused: Boolean,
  onPauseUnpause: () -> Unit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = (controlsShown && !areControlsLocked || gestureSeekAmount != null) || pausedForCache,
    enter = fadeIn(playerControlsEnterAnimationSpec()),
    exit = fadeOut(playerControlsExitAnimationSpec()),
    modifier = modifier
  ) {
    when {
      gestureSeekAmount != null -> {
        val indicatorText = stringResource(
          R.string.player_gesture_seek_indicator,
          if (gestureSeekAmount.second >= 0) '+' else '-',
          Utils.prettyTime(abs(gestureSeekAmount.second)),
          Utils.prettyTime(gestureSeekAmount.first + gestureSeekAmount.second),
        ).replace('၀', '0').replace('၁', '1').replace('၂', '2')
          .replace('၃', '3').replace('၄', '4').replace('၅', '5')
          .replace('၆', '6').replace('၇', '7').replace('၈', '8')
          .replace('၉', '9')

        Text(
          text = indicatorText,
          style = MaterialTheme.typography.headlineMedium.copy(
            shadow = Shadow(Color.Black, blurRadius = 5f),
          ),
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
      }

      pausedForCache && showLoadingCircle -> {
        CircularProgressIndicator(
          Modifier.size(96.dp),
          strokeWidth = 6.dp,
        )
      }

      controlsShown && !areControlsLocked -> {
        val playPauseInteraction = remember { MutableInteractionSource() }
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
              interactionSource = playPauseInteraction,
              indication = ripple(bounded = true, radius = 40.dp),
              onClick = onPauseUnpause,
            ),
          contentAlignment = Alignment.Center,
        ) {
          AnimatedContent(
            targetState = paused,
            transitionSpec = {
              (scaleIn(animationSpec = tween(150)) + fadeIn(animationSpec = tween(150)))
                .togetherWith(scaleOut(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)))
            },
            label = "rebranded_play_pause"
          ) { isPaused ->
            Icon(
              imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
              contentDescription = stringResource(if (isPaused) R.string.a11y_action_play else R.string.a11y_action_pause),
              tint = Color.White,
              modifier = Modifier.size(40.dp)
            )
          }
        }
      }
    }
  }
}
