package com.noxtan.player.ui.player.controls.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpeedLevelPill(
  speed: Float,
  range: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier
) {
  val speedFloat = ((speed - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

  Row(
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .background(Color.Black.copy(alpha = 0.6f))
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Default.FastForward,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(20.dp)
    )
    Spacer(modifier = Modifier.width(12.dp))
    LinearProgressIndicator(
      progress = { speedFloat },
      modifier = Modifier
        .width(120.dp)
        .height(4.dp)
        .clip(RoundedCornerShape(2.dp)),
      color = Color.White,
      trackColor = Color.White.copy(alpha = 0.3f)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = String.format(java.util.Locale.US, "%.2fx", speed),
      color = Color.White,
      fontSize = 11.sp
    )
  }
}
