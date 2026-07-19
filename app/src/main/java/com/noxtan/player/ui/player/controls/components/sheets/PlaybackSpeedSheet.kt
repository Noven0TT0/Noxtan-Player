package com.noxtan.player.ui.player.controls.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun PlaybackSpeedSheet(
  speed: Float,
  speedPresets: List<Float>,
  onSpeedChange: (Float) -> Unit,
  onAddSpeedPreset: (Float) -> Unit,
  onRemoveSpeedPreset: (Float) -> Unit,
  onResetPresets: () -> Unit,
  onMakeDefault: (Float) -> Unit,
  onResetDefault: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.BottomCenter
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismissRequest
        )
    )

    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = Color.Black,
      shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp) // DNA: 12.dp corners [3]
    ) {
      Column(
        modifier = modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp) // DNA: Padding [3]
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
          Text(
            text = "Playback Speed",
            color = Color.Gray,
            style = MaterialTheme.typography.labelSmall
          )
          androidx.compose.material3.IconButton(onClick = onDismissRequest, modifier = Modifier.size(32.dp)) {
            androidx.compose.material3.Icon(
              imageVector = androidx.compose.material.icons.Icons.Default.Close,
              contentDescription = androidx.compose.ui.res.stringResource(com.noxtan.player.R.string.a11y_back_close),
              tint = Color.White
            )
          }
        }

        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)

        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(speeds) { speedOption ->
            val isSelected = speedOption == speed
            val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF222222)
            val contentColor = if (isSelected) Color.Black else Color.White

            Box(
              modifier = Modifier
                .width(72.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .clickable {
                  onSpeedChange(speedOption)
                  onDismissRequest()
                },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (speedOption == 1.0f) "Normal" else "${speedOption}x",
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

fun Float.toFixed(precision: Int = 1): Float {
  val factor = 10.0f.pow(precision)
  return (this * factor).roundToInt() / factor
}
