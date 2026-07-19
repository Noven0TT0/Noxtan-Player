package com.noxtan.player.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.noxtan.player.R

@Composable
fun RecentPlayFab(
  visible: Boolean,
  hasRecentVideo: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = visible && hasRecentVideo,
    enter = scaleIn(),
    exit = scaleOut(),
    modifier = modifier
  ) {
    FloatingActionButton(onClick = onClick) {
      Icon(
        imageVector = Icons.Default.PlayArrow,
        contentDescription = stringResource(R.string.a11y_resume_recent)
      )
    }
  }
}
