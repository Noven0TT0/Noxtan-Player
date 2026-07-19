package com.noxtan.player.ui.player.controls.components.panels

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noxtan.player.R
import com.noxtan.player.ui.player.Panels
import com.noxtan.player.ui.player.PlayerViewModel
import com.noxtan.player.ui.player.controls.PanelLayout
import com.noxtan.player.ui.player.controls.components.sheets.getTrackTitle
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

@Composable
fun AudioTracksPanel(
  viewModel: PlayerViewModel,
  onOpenPanel: (Panels) -> Unit,
  modifier: Modifier = Modifier,
) {
  val audioTracks by viewModel.audioTracks.collectAsState(persistentListOf())

  val audioPicker = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) viewModel.addAudio(uri)
  }

  PanelLayout(modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
      ) {
        Text(
          text = "Audio",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { onOpenPanel(Panels.None) }, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = androidx.compose.ui.res.stringResource(R.string.a11y_back_close))
        }
      }

      Column(modifier = Modifier.fillMaxWidth()) {
        audioTracks.forEach { track ->
          val isSelected = track.isSelected
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent)
              .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = {
                  if (MPVLib.getPropertyInt("aid") == track.id) {
                    MPVLib.setPropertyBoolean("aid", false)
                  } else {
                    MPVLib.setPropertyInt("aid", track.id)
                  }
                }
              )
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = isSelected,
              onClick = null,
              modifier = Modifier.size(24.dp).padding(end = 8.dp).clearAndSetSemantics {}
            )
            Text(
              text = getTrackTitle(track),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
        }

        TextButton(
          onClick = { audioPicker.launch(arrayOf("*/*")) },
          modifier = Modifier.padding(top = 4.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Add Audio File", style = MaterialTheme.typography.bodyMedium)
        }
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      val delay by MPVLib.propDouble["audio-delay"].collectAsState()
      val delayMs = ((delay ?: 0.0) * 1000).roundToInt()
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Audio Delay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          Text("${delayMs}ms", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
          value = delayMs.toFloat(),
          onValueChange = {
            val newDelay = it.roundToInt()
            MPVLib.setPropertyDouble("audio-delay", newDelay / 1000.0)
          },
          valueRange = -2000f..2000f,
          colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
          ),
          modifier = Modifier.fillMaxWidth()
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = { onOpenPanel(Panels.AudioAdvanced) },
        modifier = Modifier.fillMaxWidth().height(40.dp)
      ) {
        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Advanced Settings", style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}
