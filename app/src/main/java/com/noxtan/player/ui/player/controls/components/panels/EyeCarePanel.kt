package com.noxtan.player.ui.player.controls.components.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxtan.player.R
import com.noxtan.player.ui.player.PlayerViewModel
import com.noxtan.player.ui.player.controls.PanelLayout

@Composable
fun EyeCarePanel(
  viewModel: PlayerViewModel,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  PanelLayout(modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, start = 4.dp)
      ) {
        Text(text = stringResource(R.string.eye_care_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        IconButton(onClick = onDismissRequest, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = stringResource(R.string.a11y_back_close))
        }
      }

      // 6 Items List
      Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val blueLight by viewModel.isBlueLightFilterEnabled.collectAsState()
        val antiGlare by viewModel.isAntiGlareEnabled.collectAsState()
        val antiStrobe by viewModel.isAntiStrobeEnabled.collectAsState()
        val autoDimming by viewModel.isAutoDimmingEnabled.collectAsState()
        val nightAudio by viewModel.isNightAudioEnabled.collectAsState()

        EyeCareSwitchItem(stringResource(R.string.eye_care_bluelight), stringResource(R.string.eye_care_bluelight_desc), Icons.Rounded.Nightlight, blueLight) { viewModel.toggleBlueLightFilter(it) }
        EyeCareSwitchItem(stringResource(R.string.eye_care_antiglare), stringResource(R.string.eye_care_antiglare_desc), Icons.Rounded.WbTwilight, antiGlare) { viewModel.toggleAntiGlareFilter(it) }
        EyeCareSwitchItem(stringResource(R.string.eye_care_antistrobe), stringResource(R.string.eye_care_antistrobe_desc), Icons.Rounded.FlashOff, antiStrobe) { viewModel.toggleAntiStrobeFilter(it) }
        EyeCareSwitchItem(stringResource(R.string.eye_care_autodim), stringResource(R.string.eye_care_autodim_desc), Icons.Rounded.AutoAwesome, autoDimming) { viewModel.toggleAutoDimmingFilter(it) }
        EyeCareSwitchItem(stringResource(R.string.eye_care_nightaudio), stringResource(R.string.eye_care_nightaudio_desc), Icons.Rounded.Hearing, nightAudio) { viewModel.toggleNightAudioFilter(it) }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun EyeCareSwitchItem(
  title: String,
  subtitle: String,
  icon: ImageVector,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
      .padding(horizontal = 12.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }

    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 16.sp
      )
    }

    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.scale(0.85f).clearAndSetSemantics {}
    )
  }
}
