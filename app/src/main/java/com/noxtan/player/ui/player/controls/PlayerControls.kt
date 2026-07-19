package com.noxtan.player.ui.player.controls

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.noxtan.player.R
import com.noxtan.player.preferences.AdvancedPreferences
import com.noxtan.player.preferences.PlayerPreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.preferences.preference.deleteAndGet
import com.noxtan.player.preferences.preference.minusAssign
import com.noxtan.player.preferences.preference.plusAssign
import com.noxtan.player.ui.player.Decoder.Companion.getDecoderFromValue
import com.noxtan.player.ui.player.Panels
import com.noxtan.player.ui.player.PlayerUpdates
import com.noxtan.player.ui.player.PlayerViewModel
import com.noxtan.player.ui.player.Sheets
import com.noxtan.player.ui.player.VideoAspect
import com.noxtan.player.ui.player.controls.components.BrightnessLevelPill
import com.noxtan.player.ui.player.controls.components.ControlsButton
import com.noxtan.player.ui.player.controls.components.MultipleSpeedPlayerUpdate
import com.noxtan.player.ui.player.controls.components.PlayerBottomBar
import com.noxtan.player.ui.player.controls.components.PlayerCenterControls
import com.noxtan.player.ui.player.controls.components.PlayerTopBar
import com.noxtan.player.ui.player.controls.components.SpeedLevelPill
import com.noxtan.player.ui.player.controls.components.SubtitleOverlay
import com.noxtan.player.ui.player.controls.components.TextPlayerUpdate
import com.noxtan.player.ui.player.controls.components.VolumeLevelPill
import com.noxtan.player.ui.player.controls.components.sheets.TimePickerDialog
import com.noxtan.player.ui.player.controls.components.sheets.toFixed
import com.noxtan.player.ui.theme.playerRippleConfiguration
import com.noxtan.player.ui.theme.spacing
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import org.koin.compose.koinInject

@Suppress("CompositionLocalAllowlist")
val LocalPlayerButtonsClickEvent = staticCompositionLocalOf { {} }

@Composable
@Suppress("CyclomaticComplexMethod", "ViewModelForwarding")
fun PlayerControls(
  viewModel: PlayerViewModel,
  onBackPress: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val spacing = MaterialTheme.spacing
  val view = androidx.compose.ui.platform.LocalView.current
  val density = LocalDensity.current
  val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current

  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

  val initialInsets = remember(view) {
    val rootInsets = view.rootWindowInsets
    if (rootInsets != null) {
      val compat = androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat(rootInsets)
      val systemBars = compat.getInsetsIgnoringVisibility(
        androidx.core.view.WindowInsetsCompat.Type.systemBars() or
          androidx.core.view.WindowInsetsCompat.Type.displayCutout()
      )
      Triple(
        with(density) { systemBars.bottom.toDp() },
        with(density) { systemBars.left.toDp() },
        with(density) { systemBars.right.toDp() }
      )
    } else {
      null
    }
  }

  var fixedBottomPadding by remember { mutableStateOf(initialInsets?.first ?: 0.dp) }
  var fixedLeftPadding by remember { mutableStateOf(initialInsets?.second ?: 0.dp) }
  var fixedRightPadding by remember { mutableStateOf(initialInsets?.third ?: 0.dp) }

  LaunchedEffect(view) {
    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
      val systemBars = insets.getInsetsIgnoringVisibility(
        androidx.core.view.WindowInsetsCompat.Type.systemBars() or
          androidx.core.view.WindowInsetsCompat.Type.displayCutout()
      )
      fixedBottomPadding = with(density) { systemBars.bottom.toDp() }
      fixedLeftPadding = with(density) { systemBars.left.toDp() }
      fixedRightPadding = with(density) { systemBars.right.toDp() }
      insets
    }
    view.requestApplyInsets()
  }

  val isThreeButtonNav = fixedBottomPadding > 30.dp

  val extraBottomPadding = if (isLandscape) {
    if (isThreeButtonNav) {
      0.dp
    } else {
      6.dp
    }
  } else {
    if (isThreeButtonNav) {
      4.dp
    } else {
      24.dp
    }
  }

  val playerPreferences = koinInject<PlayerPreferences>()

  val isLowEnd = remember(view.context) { com.noxtan.player.ui.utils.DevicePerformanceHelper.isLowEndDevice(view.context) }
  val reduceMotionPref by playerPreferences.reduceMotion.collectAsState()
  val reduceMotion = reduceMotionPref || isLowEnd

  val interactionSource = remember { MutableInteractionSource() }
  val controlsShown by viewModel.controlsShown.collectAsState()
  val areControlsLocked by viewModel.areControlsLocked.collectAsState()
  val seekBarShown by viewModel.seekBarShown.collectAsState()
  val pausedForCache by MPVLib.propBoolean["paused-for-cache"].collectAsState()
  val paused by MPVLib.propBoolean["pause"].collectAsState()
  val duration by MPVLib.propInt["duration"].collectAsState()
  val position by MPVLib.propInt["time-pos"].collectAsState()
  val playbackSpeed by MPVLib.propFloat["speed"].collectAsState()
  val gestureSeekAmount by viewModel.gestureSeekAmount.collectAsState()
  val doubleTapSeekAmount by viewModel.doubleTapSeekAmount.collectAsState()
  val showDoubleTapOvals by playerPreferences.showDoubleTapOvals.collectAsState()
  val showSeekIcon by playerPreferences.showSeekIcon.collectAsState()
  val showSeekTime by playerPreferences.showSeekTimeWhileSeeking.collectAsState()
  var isSeeking by remember { mutableStateOf(false) }
  var resetControls by remember { mutableStateOf(true) }
  val seekText by viewModel.seekText.collectAsState()
  val currentChapter by MPVLib.propInt["chapter"].collectAsState()
  val mpvDecoder by MPVLib.propString["hwdec-current"].collectAsState()
  val decoder by remember { derivedStateOf { getDecoderFromValue(mpvDecoder ?: "auto") } }
  val playerTimeToDisappear by playerPreferences.playerTimeToDisappear.collectAsState()
  val chapters by viewModel.chapters.collectAsState(persistentListOf())

  val customCues by viewModel.customCues.collectAsState()
  val isCustomSubActive by viewModel.isCustomSubActive.collectAsState()
  val customSubVerticalPosition by viewModel.customSubVerticalPosition.collectAsState()
  val customSubBackgroundEnabled by viewModel.customSubBackgroundEnabled.collectAsState()

  val customSubFontSize by viewModel.customSubFontSize.collectAsState()
  val customSubScale by viewModel.customSubScale.collectAsState()
  val customSubBold by viewModel.customSubBold.collectAsState()
  val customSubItalic by viewModel.customSubItalic.collectAsState()
  val customSubTextColor by viewModel.customSubTextColor.collectAsState()
  val customSubBorderColor by viewModel.customSubBorderColor.collectAsState()
  val customSubBorderSize by viewModel.customSubBorderSize.collectAsState()
  val customSubShadowOffset by viewModel.customSubShadowOffset.collectAsState()

  val safeBottomLimit = 0.70f
  val targetSubPos = if (controlsShown) {
    if (customSubVerticalPosition > safeBottomLimit) safeBottomLimit else customSubVerticalPosition
  } else {
    customSubVerticalPosition
  }

  var isMoreExpanded by remember { mutableStateOf(false) }
  var isSleepTimerDialogShown by remember { mutableStateOf(false) }
  val advancedPreferences = koinInject<AdvancedPreferences>()
  val sleepTimerTimeRemaining by viewModel.remainingTime.collectAsState()
  val backgroundPlayback by playerPreferences.automaticBackgroundPlayback.collectAsState()
  val autoPipMode by playerPreferences.autoPipMode.collectAsState()
  val statisticsPage by advancedPreferences.enabledStatisticsPage.collectAsState()
  val isCinematicMode by viewModel.isCinematicModeEnabled.collectAsState()

  val mediaTitle by viewModel.mediaTitle.collectAsState()
  val showChaptersButton by playerPreferences.showChaptersButton.collectAsState()
  val isVideoReadyToReveal by viewModel.isVideoReadyToReveal.collectAsState()

  val onOpenSheet: (Sheets) -> Unit = {
    viewModel.sheetShown.update { _ -> it }
    if (it == Sheets.None) {
      viewModel.showControls()
    } else {
      viewModel.hideControls()
      viewModel.panelShown.update { Panels.None }
    }
  }
  val onOpenPanel: (Panels) -> Unit = {
    viewModel.panelShown.update { _ -> it }
    if (it == Panels.None) {
      viewModel.showControls()
    } else {
      viewModel.hideControls()
      viewModel.sheetShown.update { Sheets.None }
    }
  }

  LaunchedEffect(
    controlsShown,
    paused,
    isSeeking,
    resetControls,
  ) {
    if (controlsShown && paused == false && !isSeeking) {
      delay(playerTimeToDisappear.toLong())
      viewModel.hideControls()
    }
  }

  val panelShownState by viewModel.panelShown.collectAsState()
  LaunchedEffect(panelShownState) {
    if (panelShownState != Panels.None) {
      isMoreExpanded = false
    }
  }

  val targetOverlayAlpha = if (controlsShown && !areControlsLocked) 0.8f else 0f
  val transparentOverlay = if (reduceMotion) {
    targetOverlayAlpha
  } else {
    androidx.compose.animation.core.animateFloatAsState(
      targetValue = targetOverlayAlpha,
      animationSpec = playerControlsExitAnimationSpec(),
      label = "controls_transparent_overlay",
    ).value
  }

  GestureHandler(
    viewModel = viewModel,
    interactionSource = interactionSource,
  )
  DoubleTapToSeekOvals(doubleTapSeekAmount, seekText, showDoubleTapOvals, showSeekIcon, showSeekTime, interactionSource)
  CompositionLocalProvider(
    LocalRippleConfiguration provides playerRippleConfiguration,
    LocalPlayerButtonsClickEvent provides { resetControls = !resetControls },
    LocalContentColor provides Color.White,
  ) {
    val gesturePreferences = koinInject<com.noxtan.player.preferences.GesturePreferences>()

    val portEnabled by gesturePreferences.edgeSwipePortraitEnabled.collectAsState()
    val landEnabled by gesturePreferences.edgeSwipeLandscapeEnabled.collectAsState()

    val edgeSwipeTarget by gesturePreferences.edgeSwipeTarget.collectAsState()

    val landLeft by gesturePreferences.edgeSwipeLandscapeLeftPos.collectAsState()
    val landRight by gesturePreferences.edgeSwipeLandscapeRightPos.collectAsState()
    val portLeft by gesturePreferences.edgeSwipePortraitLeftPos.collectAsState()
    val portRight by gesturePreferences.edgeSwipePortraitRightPos.collectAsState()

    val activeEnabled = if (isLandscape) landEnabled else portEnabled
    val activeLeftPos = if (isLandscape) landLeft else portLeft
    val activeRightPos = if (isLandscape) landRight else portRight

    Box(modifier = modifier.fillMaxSize()) {
      ConstraintLayout(
        modifier = Modifier
          .fillMaxSize()
          .systemGestureExclusion(
            enabled = activeEnabled,
            target = edgeSwipeTarget,
            leftFraction = activeLeftPos,
            rightFraction = activeRightPos
          )
          .background(
            Brush.verticalGradient(
              Pair(0f, Color.Black),
              Pair(.2f, Color.Transparent),
              Pair(.7f, Color.Transparent),
              Pair(1f, Color.Black),
            ),
            alpha = transparentOverlay,
          )
      ) {
        val topBar = createRef()
        val (volumeSlider, brightnessSlider, speedSlider) = createRefs()
        val unlockControlsButton = createRef()
        val seekbar = createRef()
        val playerPauseButton = createRef()
        val playerUpdates = createRef()
        val customSubtitleOverlay = createRef()

        if (panelShownState == Panels.SwipeProtector && activeEnabled) {
          Box(modifier = Modifier.fillMaxSize()) {
            val shadowBrushLeft = Brush.horizontalGradient(listOf(Color.Red.copy(alpha = 0.5f), Color.Transparent))
            val shadowBrushRight = Brush.horizontalGradient(listOf(Color.Transparent, Color.Red.copy(alpha = 0.5f)))

            val maxExclusionHeightDp = 200.dp
            val availableHeightDp = LocalConfiguration.current.screenHeightDp.dp - maxExclusionHeightDp

            if (edgeSwipeTarget == com.noxtan.player.ui.player.EdgeTarget.Left || edgeSwipeTarget == com.noxtan.player.ui.player.EdgeTarget.Both) {
              Box(modifier = Modifier
                .fillMaxWidth(0.1f)
                .height(maxExclusionHeightDp)
                .align(Alignment.TopStart)
                .offset(y = availableHeightDp * activeLeftPos)
                .background(shadowBrushLeft)
              )
            }

            if (edgeSwipeTarget == com.noxtan.player.ui.player.EdgeTarget.Right || edgeSwipeTarget == com.noxtan.player.ui.player.EdgeTarget.Both) {
              Box(modifier = Modifier
                .fillMaxWidth(0.1f)
                .height(maxExclusionHeightDp)
                .align(Alignment.TopEnd)
                .offset(y = availableHeightDp * activeRightPos)
                .background(shadowBrushRight)
              )
            }
          }
        }

        if (isCustomSubActive) {
          Box(modifier = Modifier.constrainAs(customSubtitleOverlay) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
          }.fillMaxSize()) {
            SubtitleOverlay(
              cues = customCues,
              verticalPosition = targetSubPos,
              isBackgroundEnabled = customSubBackgroundEnabled,
              fontSize = customSubFontSize,
              scale = customSubScale,
              isBold = customSubBold,
              isItalic = customSubItalic,
              textColor = customSubTextColor,
              borderColor = customSubBorderColor,
              borderSize = customSubBorderSize,
              shadowOffset = customSubShadowOffset,
              onNextSubtitle = { viewModel.skipToNextSubtitle() },
              onPreviousSubtitle = { viewModel.skipToPreviousSubtitle() }
            )
          }
        }

        val isBrightnessSliderShown by viewModel.isBrightnessSliderShown.collectAsState()
        val isVolumeSliderShown by viewModel.isVolumeSliderShown.collectAsState()
        val isSpeedSliderShown by viewModel.isSpeedSliderShown.collectAsState()
        val doubleTapSeekAmount by viewModel.doubleTapSeekAmount.collectAsState()

        LaunchedEffect(isBrightnessSliderShown, isVolumeSliderShown, isSpeedSliderShown, doubleTapSeekAmount) {
          if (isBrightnessSliderShown || isVolumeSliderShown || isSpeedSliderShown || doubleTapSeekAmount != 0) {
            viewModel.hideControls()
          }
        }

        val brightness by viewModel.currentBrightness.collectAsState()
        val volume by viewModel.currentVolume.collectAsState()
        val gestureSpeed by viewModel.currentGestureSpeed.collectAsState()
        val mpvVolume by MPVLib.propInt["volume"].collectAsState()

        LaunchedEffect(volume, mpvVolume, isVolumeSliderShown) {
          delay(2000)
          if (isVolumeSliderShown) viewModel.isVolumeSliderShown.update { false }
        }
        LaunchedEffect(brightness, isBrightnessSliderShown) {
          delay(2000)
          if (isBrightnessSliderShown) viewModel.isBrightnessSliderShown.update { false }
        }
        LaunchedEffect(isSpeedSliderShown) {
          if (isSpeedSliderShown) {
            viewModel.isVolumeSliderShown.update { false }
            viewModel.isBrightnessSliderShown.update { false }
          }
        }

        val showAnyPill = isBrightnessSliderShown || isVolumeSliderShown || isSpeedSliderShown

        var activePill by remember { mutableIntStateOf(1) }
        LaunchedEffect(isBrightnessSliderShown, isVolumeSliderShown, isSpeedSliderShown) {
          if (isVolumeSliderShown) activePill = 2
          else if (isBrightnessSliderShown) activePill = 1
          else if (isSpeedSliderShown) activePill = 3
        }

        AnimatedVisibility(
          visible = showAnyPill,
          enter = if (!reduceMotion) {
            slideInVertically(playerControlsEnterAnimationSpec()) { -it } +
              fadeIn(playerControlsEnterAnimationSpec())
          } else {
            fadeIn(playerControlsEnterAnimationSpec())
          },
          exit = if (!reduceMotion) {
            slideOutVertically(playerControlsExitAnimationSpec()) { -it } +
              fadeOut(playerControlsExitAnimationSpec())
          } else {
            fadeOut(playerControlsExitAnimationSpec())
          },
          modifier = Modifier.constrainAs(volumeSlider) {
            top.linkTo(parent.top, margin = 16.dp)
            linkTo(parent.start, parent.end)
          },
        ) {
          AnimatedContent(
            targetState = activePill,
            transitionSpec = {
              fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
            },
            contentAlignment = Alignment.Center,
            label = "PillCrossfade"
          ) { target ->
            when (target) {
              1 -> BrightnessLevelPill(brightness = brightness, range = 0f..1f)
              2 -> VolumeLevelPill(volume = volume, mpvVolume = mpvVolume ?: 100, range = 0..viewModel.maxVolume)
              3 -> SpeedLevelPill(speed = gestureSpeed, range = 0.25f..4.0f)
            }
          }
        }

        val holdForMultipleSpeed by playerPreferences.holdForMultipleSpeed.collectAsState()
        val currentPlayerUpdate by viewModel.playerUpdate.collectAsState()
        val aspectRatio by playerPreferences.videoAspect.collectAsState()
        LaunchedEffect(currentPlayerUpdate, aspectRatio) {
          if (currentPlayerUpdate is PlayerUpdates.MultipleSpeed || currentPlayerUpdate is PlayerUpdates.None) {
            return@LaunchedEffect
          }
          delay(2000)
          viewModel.playerUpdate.update { PlayerUpdates.None }
        }
        AnimatedVisibility(
          currentPlayerUpdate !is PlayerUpdates.None,
          enter = fadeIn(playerControlsEnterAnimationSpec()),
          exit = fadeOut(playerControlsExitAnimationSpec()),
          modifier = Modifier.constrainAs(playerUpdates) {
            linkTo(parent.start, parent.end)
            linkTo(parent.top, parent.bottom, bias = 0.2f)
          },
        ) {
          when (currentPlayerUpdate) {
            is PlayerUpdates.MultipleSpeed -> MultipleSpeedPlayerUpdate(currentSpeed = holdForMultipleSpeed)
            is PlayerUpdates.AspectRatio -> TextPlayerUpdate(stringResource(aspectRatio.titleRes))
            is PlayerUpdates.ShowText -> TextPlayerUpdate((currentPlayerUpdate as PlayerUpdates.ShowText).value)
            else -> {}
          }
        }

        AnimatedVisibility(
          controlsShown && areControlsLocked,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.constrainAs(unlockControlsButton) {
            top.linkTo(parent.top, spacing.medium)
            start.linkTo(parent.start, spacing.medium)
          },
        ) {
          ControlsButton(
            icon = Icons.Filled.Lock,
            onClick = { viewModel.unlockControls() },
            title = stringResource(R.string.a11y_player_unlock)
          )
        }

        val showLoadingCircle by playerPreferences.showLoadingCircle.collectAsState()
        PlayerCenterControls(
          controlsShown = controlsShown,
          areControlsLocked = areControlsLocked,
          gestureSeekAmount = gestureSeekAmount,
          pausedForCache = pausedForCache == true,
          showLoadingCircle = showLoadingCircle,
          paused = paused == true,
          onPauseUnpause = viewModel::pauseUnpause,
          modifier = Modifier.constrainAs(playerPauseButton) {
            end.linkTo(parent.absoluteRight)
            start.linkTo(parent.absoluteLeft)
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
          }
        )

        PlayerTopBar(
          controlsShown = controlsShown,
          areControlsLocked = areControlsLocked,
          reduceMotion = reduceMotion,
          mediaTitle = mediaTitle,
          onBackPress = onBackPress,
          decoder = decoder,
          onDecoderClick = { viewModel.cycleDecoders() },
          onDecoderLongClick = { onOpenSheet(Sheets.Decoders) },
          isChaptersVisible = showChaptersButton && chapters.isNotEmpty(),
          onChaptersClick = { onOpenSheet(Sheets.Chapters) },
          onSubtitlesClick = { onOpenPanel(Panels.SubtitleTracks) },
          onSubtitlesLongClick = { onOpenPanel(Panels.SubtitleAdvanced) },
          onAudioClick = { onOpenPanel(Panels.AudioTracks) },
          onAudioLongClick = { onOpenPanel(Panels.AudioAdvanced) },
          isMoreExpanded = isMoreExpanded,
          onMoreClick = { isMoreExpanded = !isMoreExpanded },
          onMoreLongClick = { onOpenPanel(Panels.VideoFilters) },
          backgroundPlayback = backgroundPlayback,
          onBackgroundPlaybackToggle = { playerPreferences.automaticBackgroundPlayback.set(!backgroundPlayback) },
          autoPipMode = autoPipMode,
          onAutoPipModeToggle = { playerPreferences.autoPipMode.set(it) },
          sleepTimerTimeRemaining = sleepTimerTimeRemaining,
          onSleepTimerClick = { onOpenPanel(Panels.SleepTimer) },
          statisticsPage = statisticsPage,
          onStatisticsClick = {
            val nextPage = (statisticsPage + 1) % 6
            if ((nextPage == 0) xor (statisticsPage == 0)) MPVLib.command("script-binding", "stats/display-stats-toggle")
            if (nextPage != 0) MPVLib.command("script-binding", "stats/display-page-$nextPage")
            advancedPreferences.enabledStatisticsPage.set(nextPage)
          },
          onVideoFiltersClick = { onOpenPanel(Panels.VideoFilters) },
          onSwipeProtectorClick = { onOpenPanel(Panels.SwipeProtector) },
          onEyeCareClick = { onOpenPanel(Panels.EyeCare) },
          isCinematicMode = isCinematicMode,
          onCinematicModeToggle = { viewModel.toggleCinematicMode(it) },
          modifier = Modifier
            .constrainAs(topBar) {
              top.linkTo(parent.top, spacing.medium)
              start.linkTo(parent.start)
              end.linkTo(parent.end)
            }
            .padding(start = fixedLeftPadding, end = fixedRightPadding)
        )

        val isLeftHanded by viewModel.isLeftHanded.collectAsState()

        val invertDuration by playerPreferences.invertDuration.collectAsState()
        val readAhead by MPVLib.propFloat["demuxer-cache-time"].collectAsState()
        val remaining by MPVLib.propFloat["playtime-remaining"].collectAsState()
        val preciseSeeking by playerPreferences.preciseSeeking.collectAsState()

        PlayerBottomBar(
          controlsShown = controlsShown,
          areControlsLocked = areControlsLocked,
          seekBarShown = seekBarShown,
          reduceMotion = reduceMotion,
          isLeftHanded = isLeftHanded,
          isLandscape = isLandscape,
          position = position?.toFloat() ?: 0f,
          duration = duration?.toFloat() ?: 0f,
          remaining = remaining?.toFloat() ?: 0f,
          readAhead = readAhead ?: 0f,
          preciseSeeking = preciseSeeking,
          invertDuration = invertDuration,
          onInvertDurationClick = { playerPreferences.invertDuration.set(!invertDuration) },
          chapters = chapters,
          onSeekTo = {
            isSeeking = true
            viewModel.seekTo(it.toInt(), preciseSeeking)
          },
          onSeekChangeFinished = { isSeeking = false },
          onLockControls = { viewModel.lockControls() },
          onPrevious = { viewModel.handlePrevious() },
          onPauseUnpause = viewModel::pauseUnpause,
          onNext = { viewModel.handleNext() },
          playbackSpeed = playbackSpeed ?: playerPreferences.defaultSpeed.get(),
          onSpeedClick = { onOpenSheet(Sheets.PlaybackSpeed) },
          onCycleRotation = viewModel::cycleScreenRotations,
          aspectRatio = aspectRatio,
          onChangeAspectRatio = {
            val nextAspect = when (aspectRatio) {
              VideoAspect.Fit -> VideoAspect.Stretch
              VideoAspect.Stretch -> VideoAspect.Crop
              VideoAspect.Crop -> VideoAspect.Fit
            }
            viewModel.changeVideoAspect(nextAspect, showText = true)
          },
          onToggleHandedness = { viewModel.toggleHandedness() },
          paused = paused == true,
          modifier = Modifier
            .constrainAs(seekbar) {
              bottom.linkTo(parent.bottom, margin = fixedBottomPadding + extraBottomPadding)
            }
            .padding(start = fixedLeftPadding, end = fixedRightPadding)
        )
      }

      if (isSleepTimerDialogShown) {
        TimePickerDialog(
          remainingTime = sleepTimerTimeRemaining,
          onDismissRequest = { isSleepTimerDialogShown = false },
          onTimeSelect = viewModel::startTimer,
        )
      }

      val sheetShown by viewModel.sheetShown.collectAsState()
      val subtitles by viewModel.subtitleTracks.collectAsState(persistentListOf())
      val audioTracks by viewModel.audioTracks.collectAsState(persistentListOf())
      val sleepTimerTimeRemainingVal by viewModel.remainingTime.collectAsState()
      val speedPresets by playerPreferences.speedPresets.collectAsState()
      PlayerSheets(
        sheetShown = sheetShown,
        subtitles = subtitles,
        onAddSubtitle = viewModel::addSubtitle,
        onSelectSubtitle = viewModel::selectSub,
        audioTracks = audioTracks,
        onAddAudio = viewModel::addAudio,
        onSelectAudio = {
          if (MPVLib.getPropertyInt("aid") == it.id) {
            MPVLib.setPropertyBoolean("aid", false)
          } else {
            MPVLib.setPropertyInt("aid", it.id)
          }
        },
        chapter = chapters.getOrNull(currentChapter ?: 0),
        chapters = chapters,
        onSeekToChapter = {
          MPVLib.setPropertyInt("chapter", it)
          viewModel.unpause()
        },
        decoder = decoder,
        onUpdateDecoder = { MPVLib.setPropertyString("hwdec", it.value) },
        speed = playbackSpeed ?: playerPreferences.defaultSpeed.get(),
        onSpeedChange = { MPVLib.setPropertyFloat("speed", it.toFixed(2)) },
        onMakeDefaultSpeed = { playerPreferences.defaultSpeed.set(it.toFixed(2)) },
        onAddSpeedPreset = { playerPreferences.speedPresets += it.toFixed(2).toString() },
        onRemoveSpeedPreset = { playerPreferences.speedPresets -= it.toFixed(2).toString() },
        onResetSpeedPresets = playerPreferences.speedPresets::delete,
        speedPresets = speedPresets.map { it.toFloat() }.sorted(),
        onResetDefaultSpeed = {
          MPVLib.setPropertyFloat("speed", playerPreferences.defaultSpeed.deleteAndGet().toFixed(2))
        },
        sleepTimerTimeRemaining = sleepTimerTimeRemainingVal,
        onStartSleepTimer = viewModel::startTimer,
        onOpenPanel = onOpenPanel,
        onDismissRequest = { onOpenSheet(Sheets.None) },
      )

      val panel by viewModel.panelShown.collectAsState()
      PlayerPanels(
        panelShown = panel,
        onDismissRequest = { onOpenPanel(Panels.None) },
        viewModel = viewModel,
        onOpenPanel = onOpenPanel,
      )
      androidx.compose.animation.AnimatedVisibility(
        visible = !isVideoReadyToReveal,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(500))
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {}
        )
      }
    }
  }
}

fun <T> playerControlsExitAnimationSpec(): FiniteAnimationSpec<T> = tween(
  durationMillis = 300,
  easing = FastOutSlowInEasing,
)

fun <T> playerControlsEnterAnimationSpec(): FiniteAnimationSpec<T> = tween(
  durationMillis = 100,
  easing = LinearOutSlowInEasing,
)

@SuppressLint("NewApi")
fun Modifier.systemGestureExclusion(
  enabled: Boolean,
  target: com.noxtan.player.ui.player.EdgeTarget,
  leftFraction: Float,
  rightFraction: Float
) = composed {
  val view = LocalView.current
  val density = LocalDensity.current

  androidx.compose.runtime.LaunchedEffect(enabled, target, leftFraction, rightFraction, view.width, view.height) {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || view.width <= 0 || view.height <= 0) {
      view.systemGestureExclusionRects = emptyList()
      return@LaunchedEffect
    }

    val rects = mutableListOf<android.graphics.Rect>()
    val maxExclusionHeightPx = with(density) { 200.dp.toPx() }

    val availableHeight = (view.height - maxExclusionHeightPx).coerceAtLeast(0f)

    val edgeWidthPx = with(density) { 120.dp.toPx() }.toInt()

    if (target == com.noxtan.player.ui.player.EdgeTarget.Left || target == com.noxtan.player.ui.player.EdgeTarget.Both) {
      val topPx = (availableHeight * leftFraction).coerceIn(0f, availableHeight).toInt()
      rects.add(android.graphics.Rect(0, topPx, edgeWidthPx, topPx + maxExclusionHeightPx.toInt()))
    }
    if (target == com.noxtan.player.ui.player.EdgeTarget.Right || target == com.noxtan.player.ui.player.EdgeTarget.Both) {
      val topPx = (availableHeight * rightFraction).coerceIn(0f, availableHeight).toInt()
      rects.add(android.graphics.Rect(view.width - edgeWidthPx, topPx, view.width, topPx + maxExclusionHeightPx.toInt()))
    }

    view.systemGestureExclusionRects = rects
  }
  this
}
