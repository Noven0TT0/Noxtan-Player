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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noxtan.player.R
import com.noxtan.player.preferences.SubtitlesBorderStyle
import com.noxtan.player.preferences.SubtitlesPreferences
import com.noxtan.player.presentation.components.RepeatingIconButton
import com.noxtan.player.ui.player.Panels
import com.noxtan.player.ui.player.PlayerViewModel
import com.noxtan.player.ui.player.controls.PanelLayout
import com.noxtan.player.ui.player.controls.components.sheets.getTrackTitle
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun SubtitleTracksPanel(
  viewModel: PlayerViewModel,
  onOpenPanel: (Panels) -> Unit,
  modifier: Modifier = Modifier,
) {
  val subtitles by viewModel.subtitleTracks.collectAsState(persistentListOf())
  val subtitlesPreferences = koinInject<SubtitlesPreferences>()

  val context = androidx.compose.ui.platform.LocalContext.current

  val subtitlesPicker = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      try {
        context.contentResolver.takePersistableUriPermission(
          uri,
          android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      } catch (e: Exception) { }
      viewModel.addSubtitle(uri)
    }
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
          text = "Subtitles",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { onOpenPanel(Panels.None) }, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = androidx.compose.ui.res.stringResource(R.string.a11y_back_close))
        }
      }

      Column(modifier = Modifier.fillMaxWidth()) {
        if (subtitles.isEmpty()) {
          Text(
            text = "No subtitles found",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
          )
        } else {
          subtitles.forEach { track ->
            val isSelected = track.isSelected
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent)
                .selectable(selected = isSelected, onClick = { viewModel.selectSub(track.id) }, role = Role.RadioButton)
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
        }

        TextButton(
          onClick = { subtitlesPicker.launch(arrayOf("*/*")) },
          modifier = Modifier.padding(top = 4.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Add Subtitle File", style = MaterialTheme.typography.bodyMedium)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        val delay by MPVLib.propDouble["sub-delay"].collectAsState()
        var localDelayMs by remember { mutableIntStateOf(((MPVLib.getPropertyDouble("sub-delay") ?: 0.0) * 1000).roundToInt()) }

        LaunchedEffect(delay) {
          val newDelay = ((delay ?: 0.0) * 1000).roundToInt()
          if (kotlin.math.abs(localDelayMs - newDelay) > 50) {
            localDelayMs = newDelay
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Delay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            RepeatingIconButton(
              onClick = {
                localDelayMs -= 100
                MPVLib.setPropertyDouble("sub-delay", localDelayMs / 1000.0)
              },
              modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            ) {
              Icon(Icons.Default.Remove, contentDescription = "-", tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }

            Text(
              text = "${localDelayMs}ms",
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.width(64.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            RepeatingIconButton(
              onClick = {
                localDelayMs += 100
                MPVLib.setPropertyDouble("sub-delay", localDelayMs / 1000.0)
              },
              modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            ) {
              Icon(Icons.Default.Add, contentDescription = "+", tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val subPos by MPVLib.propInt["sub-pos"].collectAsState()
        val currentPos = subPos ?: 100
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Position", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(currentPos.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
          }
          Slider(
            value = currentPos.toFloat(),
            onValueChange = {
              val newPos = it.roundToInt()
              subtitlesPreferences.subPos.set(newPos)
              MPVLib.setPropertyInt("sub-pos", newPos)
            },
            valueRange = 0f..150f,
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val borderStyle by MPVLib.propString["sub-border-style"].collectAsState()
        val hasBg = borderStyle == "opaque-box" || borderStyle == "background-box"
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Background", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          Switch(
            checked = hasBg,
            onCheckedChange = {
              val newStyle = if (it) "opaque-box" else "outline-and-shadow"
              MPVLib.setPropertyString("sub-border-style", newStyle)
              subtitlesPreferences.borderStyle.set(
                if (it) SubtitlesBorderStyle.OpaqueBox else SubtitlesBorderStyle.OutlineAndShadow
              )
            }
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = { onOpenPanel(Panels.SubtitleAdvanced) },
          modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
          Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Advanced Settings", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}
