package com.noxtan.player.ui.player.controls.components.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noxtan.player.R
import com.noxtan.player.preferences.GesturePreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.ui.player.EdgeTarget
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeProtectorPanel(
  isLandscape: Boolean,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier
) {
  val preferences = koinInject<GesturePreferences>()

  val portEnabled by preferences.edgeSwipePortraitEnabled.collectAsState()
  val landEnabled by preferences.edgeSwipeLandscapeEnabled.collectAsState()
  val enabled = if (isLandscape) landEnabled else portEnabled

  val target by preferences.edgeSwipeTarget.collectAsState()

  val leftPos by if (isLandscape) preferences.edgeSwipeLandscapeLeftPos.collectAsState() else preferences.edgeSwipePortraitLeftPos.collectAsState()
  val rightPos by if (isLandscape) preferences.edgeSwipeLandscapeRightPos.collectAsState() else preferences.edgeSwipePortraitRightPos.collectAsState()

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(0.75f).widthIn(max = 300.dp),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(
          alpha = if (com.noxtan.player.ui.utils.DevicePerformanceHelper.isLowEndDevice(androidx.compose.ui.platform.LocalContext.current)) 1f else 0.9f
        )
      )
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, start = 4.dp)
        ) {
          Text(
            text = stringResource(R.string.swipe_protector_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = enabled, onCheckedChange = {
              if (isLandscape) preferences.edgeSwipeLandscapeEnabled.set(it)
              else preferences.edgeSwipePortraitEnabled.set(it)
            })
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDismissRequest, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Close, contentDescription = stringResource(R.string.a11y_back_close))
            }
          }
        }

        if (enabled) {
          Text(stringResource(R.string.swipe_protector_target_edge), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TargetButton(stringResource(R.string.swipe_protector_left), target == EdgeTarget.Left) { preferences.edgeSwipeTarget.set(EdgeTarget.Left) }
            TargetButton(stringResource(R.string.swipe_protector_right), target == EdgeTarget.Right) { preferences.edgeSwipeTarget.set(EdgeTarget.Right) }
            TargetButton(stringResource(R.string.swipe_protector_both), target == EdgeTarget.Both) { preferences.edgeSwipeTarget.set(EdgeTarget.Both) }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (target == EdgeTarget.Left || target == EdgeTarget.Both) {
            Text(stringResource(R.string.swipe_protector_left_pos), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Slider(
              value = leftPos,
              onValueChange = { if (isLandscape) preferences.edgeSwipeLandscapeLeftPos.set(it) else preferences.edgeSwipePortraitLeftPos.set(it) },
              valueRange = 0f..1f,
              modifier = Modifier.fillMaxWidth()
            )
          }

          if (target == EdgeTarget.Right || target == EdgeTarget.Both) {
            Text(stringResource(R.string.swipe_protector_right_pos), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Slider(
              value = rightPos,
              onValueChange = { if (isLandscape) preferences.edgeSwipeLandscapeRightPos.set(it) else preferences.edgeSwipePortraitRightPos.set(it) },
              valueRange = 0f..1f,
              modifier = Modifier.fillMaxWidth()
            )
          }
        } else {
          Text(
            text = stringResource(R.string.swipe_protector_desc),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun RowScope.TargetButton(title: String, isSelected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .weight(1f)
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      .clickable { onClick() }
      .padding(vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
      color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
