package com.noxtan.player.ui.player.controls

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.noxtan.player.ui.player.controls.components.ControlsButton

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("NewApi")
@Composable
fun BottomRightPlayerControls(
  onAspectClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(modifier) {

    ControlsButton(
      Icons.Default.AspectRatio,
      onClick = onAspectClick,
    )
  }
}
