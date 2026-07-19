package com.noxtan.player.ui.player.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.noxtan.player.ui.player.controls.components.ControlsButton
import androidx.compose.ui.res.stringResource
import com.noxtan.player.R

@Composable
fun TopLeftPlayerControls(
  mediaTitle: String,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    ControlsButton(
      icon = Icons.AutoMirrored.Default.ArrowBack,
      onClick = onBackClick,
      title = stringResource(R.string.a11y_back_close)
    )
    Text(
      mediaTitle,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      color = Color.White,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.weight(1f)
    )
  }
}
