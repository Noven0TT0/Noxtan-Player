package com.noxtan.player.ui.utils

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.random.Random

fun Modifier.topAndBottomNoise(
  noiseAlpha: Float = 0.12f,
  fadeFraction: Float = 0.09f,
  edgeDarkenAlpha: Float = 1f,
  fadeColor: Color = Color.Unspecified
): Modifier = composed {
  val noiseImage = remember { createNoisePattern() }

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

    val maskBrush = Brush.verticalGradient(
      0.0f to Color.Black,
      fadeFraction to Color.Transparent,
      (1f - fadeFraction) to Color.Transparent,
      1.0f to Color.Black
    )

    val noisePaint = Paint().apply {
      shader = ImageShader(noiseImage, TileMode.Repeated, TileMode.Repeated)
      this.alpha = noiseAlpha
    }

    val layerPaint = Paint().apply {
      blendMode = BlendMode.Softlight
    }

    onDrawWithContent {
      drawContent()
      drawRect(brush = darkenBrush)
      drawContext.canvas.saveLayer(size.toRect(), layerPaint)
      drawContext.canvas.drawRect(size.toRect(), noisePaint)
      drawRect(
        brush = maskBrush,
        blendMode = BlendMode.DstIn
      )
      drawContext.canvas.restore()
    }
  }
}

private fun createNoisePattern(): androidx.compose.ui.graphics.ImageBitmap {
  val size = 256
  val pixels = IntArray(size * size)
  for (i in pixels.indices) {
    val gray = Random.nextInt(80) + 88
    pixels[i] = android.graphics.Color.argb(255, gray, gray, gray)
  }
  return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}
