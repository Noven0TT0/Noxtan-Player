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
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxtan.player.preferences.AudioPreferences
import com.noxtan.player.preferences.preference.collectAsState
import org.koin.compose.koinInject

@Composable
fun VolumeLevelPill(
  volume: Int,
  mpvVolume: Int,
  range: ClosedRange<Int>,
  modifier: Modifier = Modifier
) {
  val audioPreferences = koinInject<AudioPreferences>()
  val boostCap by audioPreferences.volumeBoostCap.collectAsState()

  val boostVolume = mpvVolume - 100

  val progressFloat = if (boostVolume > 0 && boostCap > 0) {
    (boostVolume.toFloat() / boostCap.toFloat()).coerceIn(0f, 1f)
  } else {
    (volume.toFloat() / range.endInclusive.toFloat()).coerceIn(0f, 1f)
  }

  val percentage = ((volume.toFloat() / range.endInclusive.toFloat()).coerceIn(0f, 1f) * 100).toInt()

  val icon = when {
    boostVolume > 0 -> Icons.AutoMirrored.Default.VolumeUp
    percentage == 0 -> Icons.AutoMirrored.Default.VolumeOff
    percentage in 1..30 -> Icons.AutoMirrored.Default.VolumeMute
    percentage in 31..60 -> Icons.AutoMirrored.Default.VolumeDown
    else -> Icons.AutoMirrored.Default.VolumeUp
  }

  Row(
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .background(Color.Black.copy(alpha = 0.6f))
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(20.dp)
    )
    Spacer(modifier = Modifier.width(12.dp))
    LinearProgressIndicator(
      progress = { progressFloat },
      modifier = Modifier
        .width(120.dp)
        .height(4.dp)
        .clip(RoundedCornerShape(2.dp)),
      color = if (boostVolume > 0) Color(0xFFFFD700) else Color.White,
      trackColor = Color.White.copy(alpha = 0.3f)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = if (boostVolume > 0) "+$boostVolume" else "$percentage%",
      color = Color.White,
      fontSize = 11.sp
    )
  }
}
