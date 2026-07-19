package com.noxtan.player.ui.player.controls.components.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noxtan.player.preferences.AudioChannels
import com.noxtan.player.preferences.AudioPreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.ui.player.controls.PanelLayout
import `is`.xyz.mpv.MPVLib
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun AudioAdvancedPanel(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val preferences = koinInject<AudioPreferences>()

  PanelLayout(modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Advanced Settings",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text("Preferences", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

        val pitchCorrection by preferences.audioPitchCorrection.collectAsState()
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Pitch Correction", style = MaterialTheme.typography.bodyMedium)
            Text("Correct pitch when changing speed", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
          }
          Switch(
            checked = pitchCorrection,
            onCheckedChange = {
              preferences.audioPitchCorrection.set(it)
              MPVLib.setPropertyBoolean("audio-pitch-correction", it)
            },
            modifier = Modifier.scale(0.85f)
          )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        Text("Channels", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        val activeChannel by preferences.audioChannels.collectAsState()

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          AudioChannels.entries.forEach { channel ->
            val isSelected = activeChannel == channel
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent)
                .selectable(
                  selected = isSelected,
                  role = Role.RadioButton,
                  onClick = {
                    preferences.audioChannels.set(channel)
                    if (channel == AudioChannels.ReverseStereo) {
                      MPVLib.setPropertyString(AudioChannels.AutoSafe.property, AudioChannels.AutoSafe.value)
                    } else {
                      MPVLib.setPropertyString(AudioChannels.ReverseStereo.property, "")
                    }
                    MPVLib.setPropertyString(channel.property, channel.value)
                  }
                )
                .padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = isSelected,
                onClick = null,
                modifier = Modifier.size(24.dp).padding(end = 8.dp).clearAndSetSemantics {}
              )
              Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              )
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        Text("Volume Boost", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        val volumeBoostCap by preferences.volumeBoostCap.collectAsState()

        Column(modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Boost Cap", style = MaterialTheme.typography.bodyMedium)
            Text(if (volumeBoostCap == 0) "Disabled" else "$volumeBoostCap%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
          }
          Slider(
            value = volumeBoostCap.toFloat(),
            onValueChange = {
              val cap = it.roundToInt()
              preferences.volumeBoostCap.set(cap)
              MPVLib.setPropertyInt("volume-max", cap + 100)
            },
            valueRange = 0f..200f,
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }
  }
}
