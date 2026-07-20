package com.noxtan.player.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noxtan.player.R

enum class MarkState(@StringRes val labelRes: Int) {
  NEW(R.string.mark_as_new),
  PLAYED(R.string.mark_as_played),
  UNPLAYED(R.string.mark_as_unplayed),
  NONE(R.string.mark_as_none)
}

@Composable
fun SelectionBottomBar(
  visible: Boolean,
  selectedCount: Int,
  showRename: Boolean = true,
  showMove: Boolean = true,
  onShareClick: () -> Unit,
  onRenameClick: () -> Unit,
  onMoveClick: () -> Unit,
  onMarkAsSelected: (MarkState) -> Unit,
  onInfoClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showMarkAsMenu by remember { mutableStateOf(false) }
  AnimatedVisibility(
    visible = visible,
    enter = slideInVertically(initialOffsetY = { it * 2 }),
    exit = slideOutVertically(targetOffsetY = { it * 2 }),
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 16.dp, vertical = 16.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(100),
      color = MaterialTheme.colorScheme.surfaceVariant,
      tonalElevation = 6.dp,
      shadowElevation = 4.dp
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onShareClick, enabled = selectedCount > 0) {
          Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.generic_share))
        }

        Box {
          IconButton(onClick = { showMarkAsMenu = true }, enabled = selectedCount > 0) {
            Icon(Icons.Rounded.Label, contentDescription = stringResource(R.string.a11y_mark_as))
          }

          DropdownMenu(
            expanded = showMarkAsMenu,
            onDismissRequest = { showMarkAsMenu = false }
          ) {
            MarkState.entries.forEach { state ->
              DropdownMenuItem(
                text = { Text(stringResource(state.labelRes)) },
                onClick = {
                  showMarkAsMenu = false
                  onMarkAsSelected(state)
                }
              )
            }
          }
        }

        if (showRename) {
          IconButton(onClick = onRenameClick, enabled = selectedCount == 1) {
            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.a11y_rename))
          }
        }

        if (showMove) {
          IconButton(onClick = onMoveClick, enabled = selectedCount == 1) {
            Icon(Icons.Rounded.DriveFileMove, contentDescription = stringResource(R.string.a11y_move))
          }
        }

        IconButton(onClick = onInfoClick, enabled = selectedCount > 0) {
          Icon(Icons.Rounded.Info, contentDescription = stringResource(R.string.a11y_info))
        }

        IconButton(onClick = onDeleteClick, enabled = selectedCount > 0) {
          val deleteTint = if (selectedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
          Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.a11y_delete), tint = deleteTint)
        }
      }
    }
  }
}
