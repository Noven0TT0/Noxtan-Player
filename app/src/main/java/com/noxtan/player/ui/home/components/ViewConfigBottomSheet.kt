package com.noxtan.player.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noxtan.player.features.local.viewmodel.SortType
import com.noxtan.player.features.local.viewmodel.ViewConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewConfigBottomSheet(
  config: ViewConfig,
  onConfigChange: (ViewConfig) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 8.dp)
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      Text("View Options", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

      // --- 1. Layout Style ---
      ConfigSection("Layout") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = config.gridCount == 1,
            onClick = { onConfigChange(config.copy(gridCount = 1)) },
            label = { Text("List") },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.ViewList, null) }
          )
          FilterChip(
            selected = config.gridCount == 2,
            onClick = { onConfigChange(config.copy(gridCount = 2)) },
            label = { Text("Grid") },
            leadingIcon = { Icon(Icons.Rounded.GridView, null) }
          )
        }
      }

      // --- 2. Sort By ---
      ConfigSection("Sort By") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          SortChip(config, SortType.NAME, "Name", onConfigChange)
          SortChip(config, SortType.DATE, "Date", onConfigChange)
          SortChip(config, SortType.SIZE, "Size", onConfigChange)
          SortChip(config, SortType.DURATION, "Duration", onConfigChange)
        }
      }

      // --- 3. Order ---
      ConfigSection("Order") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = config.isAscending,
            onClick = { onConfigChange(config.copy(isAscending = true)) },
            label = { Text(if (config.sortBy == SortType.NAME) "A to Z" else "Oldest / Smallest") },
            leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null) }
          )
          FilterChip(
            selected = !config.isAscending,
            onClick = { onConfigChange(config.copy(isAscending = false)) },
            label = { Text(if (config.sortBy == SortType.NAME) "Z to A" else "Newest / Largest") },
            leadingIcon = { Icon(Icons.Rounded.ArrowDownward, null) }
          )
        }
      }

      // --- 4. Display Info ---
      ConfigSection("Display Info") {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SwitchRow("Size", config.showSize, Modifier.weight(1f)) { onConfigChange(config.copy(showSize = it)) }
            SwitchRow("Duration", config.showDuration, Modifier.weight(1f)) { onConfigChange(config.copy(showDuration = it)) }
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SwitchRow("Resolution", config.showResolution, Modifier.weight(1f)) { onConfigChange(config.copy(showResolution = it)) }
            SwitchRow("Format", config.showExtension, Modifier.weight(1f)) { onConfigChange(config.copy(showExtension = it)) }
          }
        }
      }
    }
  }
}

@Composable
private fun ConfigSection(title: String, content: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    content()
  }
}

@Composable
private fun SortChip(config: ViewConfig, type: SortType, label: String, onConfigChange: (ViewConfig) -> Unit) {
  FilterChip(
    selected = config.sortBy == type,
    onClick = { onConfigChange(config.copy(sortBy = type)) },
    label = { Text(label) }
  )
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit) {
  Row(
    modifier = modifier
      .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 1,
      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Switch(
      checked = checked,
      onCheckedChange = null,
      modifier = Modifier
        .scale(0.8f)
        .clearAndSetSemantics {}
    )
  }
}
