package com.noxtan.player.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noxtan.player.R
import com.noxtan.player.ui.player.Decoder
import com.noxtan.player.ui.theme.spacing
import kotlinx.collections.immutable.toImmutableList

@Composable
fun DecodersSheet(
  selectedDecoder: Decoder,
  onSelect: (Decoder) -> Unit,
  onDismissRequest: () -> Unit,
) {
  GenericTracksSheet(
    tracks = Decoder.entries.minusElement(Decoder.Auto).toImmutableList(),
    header = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Text(
          text = stringResource(R.string.pref_decoder),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        androidx.compose.material3.IconButton(onClick = onDismissRequest) {
          androidx.compose.material3.Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.a11y_back_close)
          )
        }
      }
    },
    track = {
      AudioTrackRow(
        title = stringResource(R.string.player_sheets_decoder_formatted, it.title, it.value),
        isSelected = selectedDecoder == it,
        onClick = { onSelect(it) }
      )
    },
    onDismissRequest = onDismissRequest
  )
}

@Composable
fun AudioTrackRow(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
      .padding(start = MaterialTheme.spacing.smaller, end = MaterialTheme.spacing.medium),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
  ) {
    RadioButton(
      selected = isSelected,
      onClick = null,
      modifier = Modifier.clearAndSetSemantics {}
    )
    Text(
      title,
      fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
      fontStyle = if (isSelected) FontStyle.Italic else FontStyle.Normal,
    )
  }
}
