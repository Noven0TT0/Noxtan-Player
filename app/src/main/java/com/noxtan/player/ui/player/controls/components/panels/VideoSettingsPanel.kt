package com.noxtan.player.ui.player.controls.components.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxtan.player.R
import com.noxtan.player.preferences.DecoderPreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.preferences.preference.deleteAndGet
import com.noxtan.player.ui.player.DebandSettings
import com.noxtan.player.ui.player.Debanding
import com.noxtan.player.ui.player.VideoFilters
import com.noxtan.player.ui.player.controls.PanelLayout
import `is`.xyz.mpv.MPVLib
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun VideoSettingsPanel(
  onDismissRequest: () -> Unit,
  isCinematicMode: Boolean,
  onCinematicModeToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val decoderPreferences = koinInject<DecoderPreferences>()

  PanelLayout(modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, start = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = stringResource(R.string.player_sheets_video_settings_title),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Black
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          TextButton(
            onClick = {
              decoderPreferences.debanding.set(Debanding.None)
              MPVLib.setOptionString("deband", "no")
              MPVLib.command("vf", "remove", "@deband")
              DebandSettings.entries.forEach { MPVLib.setPropertyInt(it.mpvProperty, it.preference(decoderPreferences).deleteAndGet()) }
              VideoFilters.entries.forEach { MPVLib.setPropertyInt(it.mpvProperty, it.preference(decoderPreferences).deleteAndGet()) }
            },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
          ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset", style = MaterialTheme.typography.labelLarge)
          }
          IconButton(onClick = onDismissRequest, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.a11y_back_close))
          }
        }
      }

      Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
      ) {
        androidx.compose.material3.Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
          shape = RoundedCornerShape(16.dp),
          colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color.Transparent
          )
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                  colors = if (isCinematicMode) {
                    listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                  } else {
                    listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                  }
                )
              )
              .padding(16.dp)
          ) {
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = androidx.compose.material.icons.Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  tint = if (isCinematicMode) Color.White else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = stringResource(R.string.cinematic_mode_title),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Black,
                  color = if (isCinematicMode) Color.White else MaterialTheme.colorScheme.primary
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = stringResource(R.string.cinematic_mode_desc),
                style = MaterialTheme.typography.bodySmall,
                color = if (isCinematicMode) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
              )
              Spacer(modifier = Modifier.height(12.dp))

              androidx.compose.material3.Button(
                onClick = { onCinematicModeToggle(!isCinematicMode) },
                colors = buttonColors(
                  containerColor = if (isCinematicMode) Color.White else MaterialTheme.colorScheme.primary,
                  contentColor = if (isCinematicMode) Color(0xFF0072FF) else Color.White
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = stringResource(if (isCinematicMode) R.string.cinematic_mode_disable else R.string.cinematic_mode_enable),
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        PanelGroup(title = "Color Adjustments", icon = Icons.Rounded.Palette) {
          VideoFilters.entries.forEach { filter ->
            val value by filter.preference(decoderPreferences).collectAsState()
            val filterIcon = when (filter) {
              VideoFilters.BRIGHTNESS -> Icons.Rounded.Brightness6
              VideoFilters.CONTRAST -> Icons.Rounded.Contrast
              VideoFilters.SATURATION -> Icons.Rounded.ColorLens
              VideoFilters.GAMMA -> Icons.Rounded.SettingsBrightness
              VideoFilters.HUE -> Icons.Rounded.InvertColors
            }
            StyledSliderItem(
              title = stringResource(filter.titleRes),
              icon = filterIcon,
              value = value.toFloat(),
              range = -100f..100f,
              onValueChange = {
                val intVal = it.roundToInt()
                filter.preference(decoderPreferences).set(intVal)
                MPVLib.setPropertyInt(filter.mpvProperty, intVal)
              }
            )
          }
          if (!decoderPreferences.gpuNext.get()) {
            Text(
              text = stringResource(id = R.string.player_sheets_filters_warning),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
          }
        }

        PanelGroup(title = "Debanding (Smooth Gradients)", icon = Icons.Rounded.Gradient) {
          val deband by decoderPreferences.debanding.collectAsState()
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            DebandingModeButton(title = "None", icon = Icons.Rounded.Block, isSelected = deband == Debanding.None, modifier = Modifier.weight(1f)) {
              decoderPreferences.debanding.set(Debanding.None)
              MPVLib.setOptionString("deband", "no")
              MPVLib.command("vf", "remove", "@deband")
            }
            DebandingModeButton(title = "CPU", icon = Icons.Rounded.Memory, isSelected = deband == Debanding.CPU, modifier = Modifier.weight(1f)) {
              decoderPreferences.debanding.set(Debanding.CPU)
              MPVLib.setOptionString("deband", "no")
              MPVLib.command("vf", "add", "@deband:gradfun=radius=12")
            }
            DebandingModeButton(title = "GPU", icon = Icons.Rounded.Speed, isSelected = deband == Debanding.GPU, modifier = Modifier.weight(1f)) {
              decoderPreferences.debanding.set(Debanding.GPU)
              MPVLib.setOptionString("deband", "yes")
              MPVLib.command("vf", "remove", "@deband")
            }
          }

          if (deband != Debanding.None) {
            Spacer(modifier = Modifier.height(8.dp))
            DebandSettings.entries.forEach { debandSetting ->
              val value by debandSetting.preference(decoderPreferences).collectAsState()
              val debandIcon = when (debandSetting) {
                DebandSettings.Iterations -> Icons.Rounded.Repeat
                DebandSettings.Threshold -> Icons.Rounded.Tune
                DebandSettings.Range -> Icons.Rounded.CompareArrows
                DebandSettings.Grain -> Icons.Rounded.Grain
              }
              StyledSliderItem(
                title = stringResource(debandSetting.titleRes),
                icon = debandIcon,
                value = value.toFloat(),
                range = debandSetting.start.toFloat()..debandSetting.end.toFloat(),
                onValueChange = {
                  val intVal = it.roundToInt()
                  debandSetting.preference(decoderPreferences).set(intVal)
                  MPVLib.setPropertyInt(debandSetting.mpvProperty, intVal)
                }
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun PanelGroup(title: String, icon: ImageVector, content: @Composable () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)) {
      Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(vertical = 12.dp)) { content() }
    }
  }
}

@Composable
private fun StyledSliderItem(title: String, icon: ImageVector, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(12.dp))
      Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
      Text(text = value.roundToInt().toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
    Slider(
      value = value, onValueChange = onValueChange, valueRange = range,
      colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
      modifier = Modifier.fillMaxWidth().height(36.dp)
    )
  }
}

@Composable
private fun DebandingModeButton(title: String, icon: ImageVector, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
  val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
  val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
  Column(
    modifier = modifier.clip(RoundedCornerShape(12.dp)).background(bgColor).clickable { onClick() }.padding(vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = contentColor, letterSpacing = 0.5.sp)
  }
}
