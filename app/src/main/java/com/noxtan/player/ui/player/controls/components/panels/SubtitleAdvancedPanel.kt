package com.noxtan.player.ui.player.controls.components.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noxtan.player.preferences.SubtitlesPreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.ui.player.controls.PanelLayout
import `is`.xyz.mpv.MPVLib
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun SubtitleAdvancedPanel(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val preferences = koinInject<SubtitlesPreferences>()

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
        Text("Typography", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

        val fontSize by MPVLib.propInt["sub-font-size"].collectAsState()
        val currentFontSize = fontSize ?: preferences.fontSize.get()
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Font Size", style = MaterialTheme.typography.bodyMedium)
            Text(currentFontSize.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
          }
          Slider(
            value = currentFontSize.toFloat(),
            onValueChange = {
              val newSize = it.roundToInt()
              preferences.fontSize.set(newSize)
              MPVLib.setPropertyInt("sub-font-size", newSize)
            },
            valueRange = 10f..100f,
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }

        val subScale by MPVLib.propFloat["sub-scale"].collectAsState()
        val currentScale = subScale ?: 1f
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtitle Scale", style = MaterialTheme.typography.bodyMedium)
            Text(String.format(java.util.Locale.US, "%.2f", currentScale), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
          }
          Slider(
            value = currentScale,
            onValueChange = {
              preferences.subScale.set(it)
              MPVLib.setPropertyFloat("sub-scale", it)
            },
            valueRange = 0.5f..3.0f,
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          val isBold by MPVLib.propBoolean["sub-bold"].collectAsState()
          Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Bold", style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = isBold == true,
              onCheckedChange = {
                preferences.bold.set(it)
                MPVLib.setPropertyBoolean("sub-bold", it)
              },
              modifier = Modifier.scale(0.85f)
            )
          }

          val isItalic by MPVLib.propBoolean["sub-italic"].collectAsState()
          Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Italic", style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = isItalic == true,
              onCheckedChange = {
                preferences.italic.set(it)
                MPVLib.setPropertyBoolean("sub-italic", it)
              },
              modifier = Modifier.scale(0.85f)
            )
          }
        }

        val borderStyle by MPVLib.propString["sub-border-style"].collectAsState()
        val isBgOff = borderStyle == "outline-and-shadow" || borderStyle == null

        AnimatedVisibility(visible = isBgOff) {
          Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Text("Style & Align", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

            val borderSize by MPVLib.propInt["sub-outline-size"].collectAsState()
            val currentBorderSize = borderSize ?: preferences.borderSize.get()
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Border Size")
                Text(currentBorderSize.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
              }
              Slider(
                value = currentBorderSize.toFloat(),
                onValueChange = {
                  val newSize = it.roundToInt()
                  preferences.borderSize.set(newSize)
                  MPVLib.setPropertyInt("sub-outline-size", newSize)
                },
                valueRange = 0f..12f,
                colors = SliderDefaults.colors(
                  thumbColor = MaterialTheme.colorScheme.primary,
                  activeTrackColor = MaterialTheme.colorScheme.primary,
                  inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
              )
            }

            val shadowOffset by MPVLib.propInt["sub-shadow-offset"].collectAsState()
            val currentShadowOffset = shadowOffset ?: preferences.shadowOffset.get()
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Shadow Offset")
                Text(currentShadowOffset.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
              }
              Slider(
                value = currentShadowOffset.toFloat(),
                onValueChange = {
                  val newOffset = it.roundToInt()
                  preferences.shadowOffset.set(newOffset)
                  MPVLib.setPropertyInt("sub-shadow-offset", newOffset)
                },
                valueRange = 0f..10f,
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

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        Text("Colors", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

        val colorPresets = listOf(
          Color.White, Color.Yellow, Color(0xFF00FF00), Color(0xFF00FFFF),
          Color.Red, Color.Gray, Color.Black
        )

        val textColorInt by preferences.textColor.collectAsState()
        val currentTextColor = Color(textColorInt)

        Column(modifier = Modifier.fillMaxWidth()) {
          Text("Text Color", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            colorPresets.forEach { color ->
              val isSelected = color.toArgb() == currentTextColor.toArgb()
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(color)
                  .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                    shape = CircleShape
                  )
                  .clickable {
                    val argb = color.toArgb()
                    val hex = "#" + String.format("%08X", argb)
                    preferences.textColor.set(argb)
                    MPVLib.setPropertyString("sub-color", hex)
                  }
              )
            }
          }
        }

        AnimatedVisibility(visible = isBgOff) {
          val borderColorInt by preferences.borderColor.collectAsState()
          val currentBorderColor = Color(borderColorInt)

          Column(modifier = Modifier.fillMaxWidth()) {
            Text("Border Color", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              colorPresets.forEach { color ->
                val isSelected = color.toArgb() == currentBorderColor.toArgb()
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                      width = if (isSelected) 3.dp else 1.dp,
                      color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                      shape = CircleShape
                    )
                    .clickable {
                      val argb = color.toArgb()
                      val hex = "#" + String.format("%08X", argb)
                      preferences.borderColor.set(argb)
                      MPVLib.setPropertyString("sub-border-color", hex)
                    }
                )
              }
            }
          }
        }
      }
    }
  }
}
