package com.noxtan.player.ui.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

fun Modifier.topAndBottomNoise(
  noiseAlpha: Float = 0.12f,
  fadeFraction: Float = 0.09f,
  edgeDarkenAlpha: Float = 1f,
  fadeColor: Color = Color.Unspecified
): Modifier = composed {
  val context = LocalContext.current

  val isLowEnd = remember(context) { DevicePerformanceHelper.isLowEndDevice(context) }

  if (isLowEnd) {
    return@composed this
  }

  val resolvedFadeColor = if (fadeColor == Color.Unspecified) {
    MaterialTheme.colorScheme.background
  } else {
    fadeColor
  }

  this.drawWithCache {
    val darkenBrush = Brush.verticalGradient(
      0.0f to resolvedFadeColor.copy(alpha = edgeDarkenAlpha),
      fadeFraction to Color.Transparent,
      (1f - fadeFraction) to Color.Transparent,
      1.0f to resolvedFadeColor.copy(alpha = edgeDarkenAlpha)
    )

    onDrawWithContent {
      drawContent()
      drawRect(brush = darkenBrush)
    }
  }
}
