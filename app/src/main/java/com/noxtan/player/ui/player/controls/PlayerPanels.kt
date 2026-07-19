package com.noxtan.player.ui.player.controls

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.noxtan.player.ui.player.Panels
import com.noxtan.player.ui.player.PlayerViewModel
import com.noxtan.player.ui.player.controls.components.panels.AudioAdvancedPanel
import com.noxtan.player.ui.player.controls.components.panels.AudioTracksPanel
import com.noxtan.player.ui.player.controls.components.panels.EyeCarePanel
import com.noxtan.player.ui.player.controls.components.panels.SleepTimerPanel
import com.noxtan.player.ui.player.controls.components.panels.SubtitleAdvancedPanel
import com.noxtan.player.ui.player.controls.components.panels.SubtitleTracksPanel
import com.noxtan.player.ui.player.controls.components.panels.SwipeProtectorPanel
import com.noxtan.player.ui.player.controls.components.panels.VideoSettingsPanel
import com.noxtan.player.ui.utils.DevicePerformanceHelper

@Composable
fun PlayerPanels(
  panelShown: Panels,
  onDismissRequest: () -> Unit,
  viewModel: PlayerViewModel,
  onOpenPanel: (Panels) -> Unit,
  modifier: Modifier = Modifier,
) {
  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
  val isCinematicMode by viewModel.isCinematicModeEnabled.collectAsState()

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter
  ) {
    AnimatedVisibility(
      visible = panelShown != Panels.None,
      enter = if (isLandscape) slideInHorizontally { it } else slideInVertically { it },
      exit = if (isLandscape) slideOutHorizontally { it } else slideOutVertically { it }
    ) {
      AnimatedContent(
        targetState = panelShown,
        label = "panels_content",
        contentAlignment = Alignment.Center,
        contentKey = { it.name },
        transitionSpec = {
          if (isLandscape) {
            (fadeIn() + slideInHorizontally { it / 3 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 3 })
          } else {
            (fadeIn() + slideInVertically { it / 3 }) togetherWith (fadeOut() + slideOutVertically { -it / 3 })
          }
        }
      ) { currentPanel ->
        when (currentPanel) {
          Panels.None -> { Box(Modifier) }
          Panels.SubtitleTracks -> SubtitleTracksPanel(viewModel = viewModel, onOpenPanel = onOpenPanel)
          Panels.SubtitleAdvanced -> SubtitleAdvancedPanel(onBack = { onOpenPanel(Panels.SubtitleTracks) })
          Panels.AudioTracks -> AudioTracksPanel(viewModel = viewModel, onOpenPanel = onOpenPanel)
          Panels.AudioAdvanced -> AudioAdvancedPanel(onBack = { onOpenPanel(Panels.AudioTracks) })
          Panels.VideoFilters -> VideoSettingsPanel(
            onDismissRequest = onDismissRequest,
            isCinematicMode = isCinematicMode,
            onCinematicModeToggle = { viewModel.toggleCinematicMode(it) }
          )
          Panels.SleepTimer -> SleepTimerPanel(viewModel = viewModel, onDismissRequest = onDismissRequest)
          Panels.SwipeProtector -> SwipeProtectorPanel(isLandscape = isLandscape, onDismissRequest = onDismissRequest)
          Panels.EyeCare -> EyeCarePanel(viewModel = viewModel, onDismissRequest = onDismissRequest)
        }
      }
    }
  }
}

val CARDS_MAX_WIDTH = 480.dp
val panelCardsColors: @Composable () -> CardColors = {
  val playerPreferences = org.koin.compose.koinInject<com.noxtan.player.preferences.PlayerPreferences>()
  val context = LocalContext.current
  val isLowEnd = androidx.compose.runtime.remember(context) { DevicePerformanceHelper.isLowEndDevice(context) }

  val transparency = if (isLowEnd) 1f else playerPreferences.panelTransparency.get()

  val colors = CardDefaults.cardColors()
  colors.copy(
    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = transparency),
    disabledContainerColor = MaterialTheme.colorScheme.surfaceDim.copy(alpha = transparency),
  )
}

@Composable
fun PanelLayout(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

  Card(
    modifier = modifier.then(
      if (isLandscape) {
        Modifier
          .fillMaxHeight()
          .fillMaxWidth(0.45f)
          .widthIn(min = 380.dp, max = CARDS_MAX_WIDTH)
      } else {
        Modifier
          .fillMaxWidth()
          .fillMaxHeight(0.65f)
      }
    ),
    shape = if (isLandscape) {
      RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
    } else {
      RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    },
    colors = panelCardsColors(),
  ) {
    content()
  }
}
