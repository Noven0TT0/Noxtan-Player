package com.noxtan.player.ui.Settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noxtan.player.R
import com.noxtan.player.preferences.PlayerPreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.presentation.Screen
import com.noxtan.player.presentation.components.CollapsingTopBar
import com.noxtan.player.ui.Settings.components.CustomListDialogItem
import com.noxtan.player.ui.Settings.components.CustomSettingsGroup
import com.noxtan.player.ui.Settings.components.CustomSliderItem
import com.noxtan.player.ui.Settings.components.CustomSwitchItem
import com.noxtan.player.ui.player.PlayerOrientation
import com.noxtan.player.ui.player.controls.components.sheets.toFixed
import com.noxtan.player.ui.utils.LocalBackStack
import com.noxtan.player.ui.utils.topAndBottomNoise
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object PlayerControlsScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val preferences = koinInject<PlayerPreferences>()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val tabs = listOf("Playback", "Gestures", "UI & Display")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
      modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .topAndBottomNoise(noiseAlpha = 0.15f, fadeFraction = 0.09f),
      topBar = {
        CollapsingTopBar(
          title = stringResource(id = R.string.pref_player),
          scrollBehavior = scrollBehavior,
          onBackClick = { backstack.removeLastOrNull() },
          actions = { _ ->
            TextButton(
              onClick = {
                preferences.orientation.delete()
                preferences.drawOverDisplayCutout.delete()
                preferences.autoPipMode.delete()
                preferences.savePositionOnQuit.delete()
                preferences.automaticBackgroundPlayback.delete()
                preferences.closeAfterReachingEndOfVideo.delete()
                preferences.rememberBrightness.delete()

                preferences.horizontalSeekGesture.delete()
                preferences.showSeekBarWhenSeeking.delete()
                preferences.preciseSeeking.delete()
                preferences.showDoubleTapOvals.delete()
                preferences.showSeekIcon.delete()
                preferences.showSeekTimeWhileSeeking.delete()
                preferences.brightnessGesture.delete()
                preferences.volumeGesture.delete()
                preferences.holdForMultipleSpeed.delete()

                preferences.displayVolumeAsPercentage.delete()
                preferences.swapVolumeAndBrightness.delete()
                preferences.showLoadingCircle.delete()
                preferences.showChaptersButton.delete()
                preferences.currentChaptersIndicator.delete()
                preferences.showSystemStatusBar.delete()
                preferences.reduceMotion.delete()
                preferences.playerTimeToDisappear.delete()
                preferences.panelTransparency.delete()
              },
              contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
              Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset", modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Reset", style = MaterialTheme.typography.labelLarge)
            }
          }
        )
      },
      containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
      Column(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {

        TabRow(
          selectedTabIndex = pagerState.currentPage,
          containerColor = Color.Transparent,
          contentColor = MaterialTheme.colorScheme.primary,
          divider = { },
          indicator = { tabPositions ->
            val currentPage = pagerState.currentPage
            val fraction = pagerState.currentPageOffsetFraction
            val currentTab = tabPositions[currentPage]
            val targetPage = if (fraction < 0) maxOf(0, currentPage - 1) else minOf(tabPositions.lastIndex, currentPage + 1)
            val targetTab = tabPositions[targetPage]

            val indicatorOffset = androidx.compose.ui.unit.lerp(currentTab.left, targetTab.left, kotlin.math.abs(fraction))
            val indicatorWidth = androidx.compose.ui.unit.lerp(currentTab.width, targetTab.width, kotlin.math.abs(fraction))

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.BottomStart)
                .offset(x = indicatorOffset)
                .width(indicatorWidth)
                .height(3.dp)
                .background(
                  color = MaterialTheme.colorScheme.primary,
                  shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                )
            )
          }
        ) {
          tabs.forEachIndexed { index, title ->
            Tab(
              selected = pagerState.currentPage == index,
              onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
              text = { Text(title, fontWeight = FontWeight.Bold) },
              unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        HorizontalPager(
          state = pagerState,
          modifier = Modifier.weight(1f)
        ) { page ->
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
          ) {
            Spacer(modifier = Modifier.height(12.dp))
            when (page) {
              0 -> PlaybackTabContent(preferences, context)
              1 -> GesturesTabContent(preferences, context)
              2 -> DisplayTabContent(preferences, context)
            }
            Spacer(modifier = Modifier.height(padding.calculateBottomPadding() + 32.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun PlaybackTabContent(preferences: PlayerPreferences, context: Context) {
  CustomSettingsGroup("PLAYBACK") {
    val orientation by preferences.orientation.collectAsState()
    CustomListDialogItem(
      title = stringResource(R.string.pref_player_orientation),
      summary = context.getString(orientation.titleRes),
      icon = Icons.Rounded.ScreenRotation,
      value = orientation,
      values = PlayerOrientation.entries.toList(),
      valueToText = { context.getString(it.titleRes) },
      onValueChange = { preferences.orientation.set(it) }
    )

    val drawOverDisplayCutout by preferences.drawOverDisplayCutout.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.pref_player_draw_over_cutout), summary = "", icon = Icons.Rounded.Fullscreen, checked = drawOverDisplayCutout, onCheckedChange = { preferences.drawOverDisplayCutout.set(it) })

    val autoPipMode by preferences.autoPipMode.collectAsState()
    CustomSwitchItem(
      title = "Auto Picture-in-Picture",
      summary = "Automatically enter PiP mode when leaving the app",
      icon = Icons.Rounded.PictureInPicture,
      checked = autoPipMode,
      onCheckedChange = { preferences.autoPipMode.set(it) }
    )

    val savePositionOnQuit by preferences.savePositionOnQuit.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.pref_player_save_position_on_quit), summary = "", icon = Icons.Rounded.Save, checked = savePositionOnQuit, onCheckedChange = { preferences.savePositionOnQuit.set(it) })

    val enterBackgroundPlaybackAutomatically by preferences.automaticBackgroundPlayback.collectAsState()
    CustomSwitchItem(title = "Background Playback", summary = "", icon = Icons.Rounded.Headphones, checked = enterBackgroundPlaybackAutomatically, onCheckedChange = { preferences.automaticBackgroundPlayback.set(it) })

    val closeAtEOF by preferences.closeAfterReachingEndOfVideo.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.pref_player_close_after_eof), summary = "", icon = Icons.Rounded.Close, checked = closeAtEOF, onCheckedChange = { preferences.closeAfterReachingEndOfVideo.set(it) })

    val rememberBrightness by preferences.rememberBrightness.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.pref_player_remember_brightness), summary = "", icon = Icons.Rounded.BrightnessAuto, checked = rememberBrightness, onCheckedChange = { preferences.rememberBrightness.set(it) })
  }
}

@Composable
private fun GesturesTabContent(preferences: PlayerPreferences, context: Context) {
  val swapVolumeAndBrightness by preferences.swapVolumeAndBrightness.collectAsState()

  CustomSettingsGroup("SEEKING") {
    val horizontalSeekGesture by preferences.horizontalSeekGesture.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.pref_player_gestures_seek), summary = "", icon = Icons.Rounded.Swipe, checked = horizontalSeekGesture, onCheckedChange = { preferences.horizontalSeekGesture.set(it) })

    val showSeekbarWhenSeeking by preferences.showSeekBarWhenSeeking.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.pref_player_show_seekbar_when_seeking), summary = "", icon = Icons.Rounded.LinearScale, checked = showSeekbarWhenSeeking, onCheckedChange = { preferences.showSeekBarWhenSeeking.set(it) })

    val preciseSeeking by preferences.preciseSeeking.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.pref_player_precise_seeking_title), summary = stringResource(R.string.pref_player_precise_seeking_summary), icon = Icons.Rounded.AdsClick, checked = preciseSeeking, onCheckedChange = { preferences.preciseSeeking.set(it) })

    val showDoubleTapOvals by preferences.showDoubleTapOvals.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.show_splash_ovals_on_double_tap_to_seek), summary = "", icon = Icons.Rounded.WifiTethering, checked = showDoubleTapOvals, onCheckedChange = { preferences.showDoubleTapOvals.set(it) })

    val showSeekIcon by preferences.showSeekIcon.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.show_icon_on_double_tap_to_seek), summary = "", icon = Icons.Rounded.FastForward, checked = showSeekIcon, onCheckedChange = { preferences.showSeekIcon.set(it) })

    val showSeekTimeWhileSeeking by preferences.showSeekTimeWhileSeeking.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.show_time_on_double_tap_to_seek), summary = "", icon = Icons.Rounded.Timer, checked = showSeekTimeWhileSeeking, onCheckedChange = { preferences.showSeekTimeWhileSeeking.set(it) })
  }

  CustomSettingsGroup("GESTURES") {
    val brightnessSide = if (swapVolumeAndBrightness) stringResource(R.string.side_right) else stringResource(R.string.side_left)
    val volumeSide = if (swapVolumeAndBrightness) stringResource(R.string.side_left) else stringResource(R.string.side_right)

    val brightnessGesture by preferences.brightnessGesture.collectAsState()
    CustomSwitchItem(
      title = stringResource(R.string.pref_player_gestures_brightness, brightnessSide),
      summary = "",
      icon = Icons.Rounded.LightMode,
      checked = brightnessGesture,
      onCheckedChange = { preferences.brightnessGesture.set(it) }
    )

    val volumeGesture by preferences.volumeGesture.collectAsState()
    CustomSwitchItem(
      title = stringResource(R.string.pref_player_gestures_volume, volumeSide),
      summary = "",
      icon = Icons.Rounded.VolumeUp,
      checked = volumeGesture,
      onCheckedChange = { preferences.volumeGesture.set(it) }
    )

    val holdForMultipleSpeed by preferences.holdForMultipleSpeed.collectAsState()
    CustomSliderItem(
      title = stringResource(R.string.pref_player_gestures_hold_for_multiple_speed),
      summary = "",
      icon = Icons.Rounded.Speed,
      value = holdForMultipleSpeed,
      valueRange = 0f..6f,
      onValueChange = { preferences.holdForMultipleSpeed.set(it.toFixed(2)) },
      valueText = if (holdForMultipleSpeed == 0F) stringResource(R.string.generic_disabled) else String.format(java.util.Locale.US, "%.2fx", holdForMultipleSpeed)
    )
  }
}

@Composable
private fun DisplayTabContent(preferences: PlayerPreferences, context: Context) {
  CustomSettingsGroup("CONTROLS") {

    val displayVolumeAsPercentage by preferences.displayVolumeAsPercentage.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.pref_player_controls_display_volume_as_percentage), summary = "", icon = Icons.Rounded.Percent, checked = displayVolumeAsPercentage, onCheckedChange = { preferences.displayVolumeAsPercentage.set(it) })

    val swapVolumeAndBrightness by preferences.swapVolumeAndBrightness.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.swap_the_volume_and_brightness_slider), summary = "", icon = Icons.Rounded.SwapHoriz, checked = swapVolumeAndBrightness, onCheckedChange = { preferences.swapVolumeAndBrightness.set(it) })

    val showLoadingCircle by preferences.showLoadingCircle.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.pref_player_controls_show_loading_circle), summary = "", icon = Icons.Rounded.Loop, checked = showLoadingCircle, onCheckedChange = { preferences.showLoadingCircle.set(it) })

    val showChaptersButton by preferences.showChaptersButton.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.pref_player_controls_show_chapters_button), summary = stringResource(R.string.pref_player_controls_show_chapters_summary), icon = Icons.Rounded.Bookmarks, checked = showChaptersButton, onCheckedChange = { preferences.showChaptersButton.set(it) })

    val showChapterIndicator by preferences.currentChaptersIndicator.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.pref_player_controls_show_chapter_indicator), summary = stringResource(R.string.pref_player_controls_show_chapters_summary), icon = Icons.Rounded.Subtitles, checked = showChapterIndicator, onCheckedChange = { preferences.currentChaptersIndicator.set(it) })
  }

  CustomSettingsGroup("DISPLAY") {
    val showSystemStatusBar by preferences.showSystemStatusBar.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.pref_player_display_show_status_bar), summary = "", icon = Icons.Rounded.SpaceBar, checked = showSystemStatusBar, onCheckedChange = { preferences.showSystemStatusBar.set(it) })

    val reduceMotion by preferences.reduceMotion.collectAsState()
    CustomSwitchItem(title = stringResource(R.string.pref_player_display_reduce_player_animation), summary = "", icon = Icons.Rounded.MotionPhotosOff, checked = reduceMotion, onCheckedChange = { preferences.reduceMotion.set(it) })

    val playerTimeToDisappear by preferences.playerTimeToDisappear.collectAsState()
    val disappearTimes = listOf(500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000)
    CustomListDialogItem(
      title = stringResource(R.string.pref_player_display_hide_player_control_time),
      summary = "$playerTimeToDisappear ms",
      icon = Icons.Rounded.VisibilityOff,
      value = playerTimeToDisappear,
      values = disappearTimes,
      valueToText = { "$it ms" },
      onValueChange = { preferences.playerTimeToDisappear.set(it) }
    )

    val panelTransparency by preferences.panelTransparency.collectAsState()
    CustomSliderItem(
      title = stringResource(R.string.pref_player_display_panel_opacity),
      summary = "",
      icon = Icons.Rounded.Opacity,
      value = panelTransparency,
      valueRange = 0f..1f,
      onValueChange = { preferences.panelTransparency.set(it) },
      valueText = "${(panelTransparency * 100).toInt()}%"
    )
  }
}
