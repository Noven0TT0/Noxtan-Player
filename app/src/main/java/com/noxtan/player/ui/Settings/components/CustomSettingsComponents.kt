package com.noxtan.player.ui.Settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomSettingsGroup(title: String, content: @Composable () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp
    ) {
      Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
    }
  }
}

@Composable
fun CustomSwitchItem(title: String, summary: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch).padding(horizontal = 20.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    SettingsIcon(icon)
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
      if (summary.isNotBlank()) {
        Text(text = summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
      }
    }
    Spacer(modifier = Modifier.width(8.dp))
    Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.scale(0.85f).clearAndSetSemantics {})
  }
}

@Composable
fun CustomSliderItem(title: String, summary: String, icon: ImageVector, value: Float, valueRange: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit, valueText: String) {
  Column(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(horizontal = 20.dp, vertical = 14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      SettingsIcon(icon)
      Spacer(modifier = Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
        if (summary.isNotBlank()) {
          Text(text = summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
        }
      }
      Text(text = valueText, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = valueRange,
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
  }
}

@Composable
fun <T> CustomListDialogItem(title: String, summary: String, icon: ImageVector, value: T, values: List<T>, valueToText: (T) -> String, onValueChange: (T) -> Unit) {
  var showDialog by remember { mutableStateOf(false) }
  Row(
    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { showDialog = true }.padding(horizontal = 20.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    SettingsIcon(icon)
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
      if (summary.isNotBlank()) {
        Text(text = summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
      }
    }
  }
  if (showDialog) {
    AlertDialog(
      onDismissRequest = { showDialog = false },
      title = { Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
      text = {
        LazyColumn {
          items(values) { item ->
            Row(
              modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).selectable(selected = item == value, onClick = { onValueChange(item); showDialog = false }, role = Role.RadioButton).padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(selected = item == value, onClick = null, modifier = Modifier.clearAndSetSemantics {})
              Spacer(modifier = Modifier.width(16.dp))
              Text(text = valueToText(item), style = MaterialTheme.typography.bodyLarge)
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = { showDialog = false }) { Text("Close") } }
    )
  }
}

@Composable
fun CustomInputDialogItem(title: String, summary: String, icon: ImageVector, value: String, onValueChange: (String) -> Unit) {
  var showDialog by remember { mutableStateOf(false) }
  var textValue by remember(value) { mutableStateOf(value) }
  Row(
    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { showDialog = true }.padding(horizontal = 20.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    SettingsIcon(icon)
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
      if (summary.isNotBlank()) {
        Text(text = summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
      }
    }
  }
  if (showDialog) {
    AlertDialog(
      onDismissRequest = { showDialog = false },
      title = { Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
      text = {
        OutlinedTextField(
          value = textValue,
          onValueChange = { textValue = it },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(12.dp)
        )
      },
      confirmButton = { TextButton(onClick = { onValueChange(textValue); showDialog = false }) { Text("Save") } },
      dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
    )
  }
}

@Composable
private fun SettingsIcon(icon: ImageVector) {
  Box(
    modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    contentAlignment = Alignment.Center
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
  }
}
