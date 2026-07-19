package com.noxtan.player.ui.player.controls

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.noxtan.player.R
import com.noxtan.player.preferences.AudioPreferences
import com.noxtan.player.preferences.PlayerPreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.presentation.components.LeftSideOvalShape
import com.noxtan.player.presentation.components.RightSideOvalShape
import com.noxtan.player.ui.player.Panels
import com.noxtan.player.ui.player.PlayerViewModel
import com.noxtan.player.ui.player.controls.components.DoubleTapSeekTriangles
import com.noxtan.player.ui.theme.playerRippleConfiguration
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import org.koin.compose.koinInject
import kotlin.math.abs
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.res.stringResource

@Suppress("CyclomaticComplexMethod", "MultipleEmitters")
@Composable
fun GestureHandler(
  viewModel: PlayerViewModel,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier
) {
  val playerPreferences = koinInject<PlayerPreferences>()
  val audioPreferences = koinInject<AudioPreferences>()
  val panelShown by viewModel.panelShown.collectAsState()
  val paused by MPVLib.propBoolean["pause"].collectAsState()
  val duration by MPVLib.propInt["duration"].collectAsState()
  val position by MPVLib.propInt["time-pos"].collectAsState()
  val playbackSpeed by MPVLib.propFloat["speed"].collectAsState()
  val controlsShown by viewModel.controlsShown.collectAsState()
  val areControlsLocked by viewModel.areControlsLocked.collectAsState()
  val seekAmount by viewModel.doubleTapSeekAmount.collectAsState()
  val isSeekingForwards by viewModel.isSeekingForwards.collectAsState()
  var isDoubleTapSeeking by remember { mutableStateOf(false) }
  LaunchedEffect(seekAmount) {
    delay(800)
    isDoubleTapSeeking = false
    viewModel.updateSeekAmount(0)
    viewModel.updateSeekText(null)
    delay(100)
    viewModel.hideSeekBar()
  }
  val multipleSpeedGesture by playerPreferences.holdForMultipleSpeed.collectAsState()
  val brightnessGesture = playerPreferences.brightnessGesture.get()
  val volumeGesture by playerPreferences.volumeGesture.collectAsState()
  val swapVolumeAndBrightness by playerPreferences.swapVolumeAndBrightness.collectAsState()
  val seekGesture by playerPreferences.horizontalSeekGesture.collectAsState()
  val preciseSeeking by playerPreferences.preciseSeeking.collectAsState()
  val showSeekbarWhenSeeking by playerPreferences.showSeekBarWhenSeeking.collectAsState()
  val currentVolume by viewModel.currentVolume.collectAsState()
  val currentMPVVolume by MPVLib.propInt["volume"].collectAsState()
  val currentBrightness by viewModel.currentBrightness.collectAsState()
  val volumeBoostingCap = audioPreferences.volumeBoostCap.get()
  val haptics = LocalHapticFeedback.current
  val showControlsLabel = stringResource(R.string.a11y_show_controls)
  val hideControlsLabel = stringResource(R.string.a11y_hide_controls)

  Box(
    modifier = modifier
      .fillMaxSize()
      .semantics {
        onClick(label = if (controlsShown) hideControlsLabel else showControlsLabel) {
          if (controlsShown) viewModel.hideControls() else viewModel.showControls()
          true
        }
      }
      .pointerInput(Unit) {
        detectTapGestures(
          onTap = {
            if (controlsShown) viewModel.hideControls() else viewModel.showControls()
          },
          onDoubleTap = {
            if (areControlsLocked || isDoubleTapSeeking) return@detectTapGestures
            if (it.x > size.width * 3 / 5) {
              if (!isSeekingForwards) viewModel.updateSeekAmount(0)
              viewModel.handleRightDoubleTap()
              isDoubleTapSeeking = true
            } else if (it.x < size.width * 2 / 5) {
              if (isSeekingForwards) viewModel.updateSeekAmount(0)
              viewModel.handleLeftDoubleTap()
              isDoubleTapSeeking = true
            } else {
              viewModel.handleCenterDoubleTap()
            }
          },
          onPress = {
            if (panelShown != Panels.None) {
              viewModel.panelShown.update { Panels.None }
            }
            if (!areControlsLocked && isDoubleTapSeeking && seekAmount != 0) {
              if (it.x > size.width * 3 / 5) {
                if (!isSeekingForwards) viewModel.updateSeekAmount(0)
                viewModel.handleRightDoubleTap()
              } else if (it.x < size.width * 2 / 5) {
                if (isSeekingForwards) viewModel.updateSeekAmount(0)
                viewModel.handleLeftDoubleTap()
              } else {
                viewModel.handleCenterDoubleTap()
              }
            } else {
              isDoubleTapSeeking = false
            }
            val press = PressInteraction.Press(
              it.copy(x = if (it.x > size.width * 3 / 5) it.x - size.width * 0.6f else it.x),
            )
            interactionSource.emit(press)
            tryAwaitRelease()
            interactionSource.emit(PressInteraction.Release(press))
          }
        )
      }
      .pointerInput(areControlsLocked, multipleSpeedGesture) {
        if (areControlsLocked || multipleSpeedGesture == 0f) return@pointerInput
        var originalSpeedForLongPress = 1f
        var currentLongPressSpeed = 2f
        var isLongPressDragActive = false
        var accumulatedSpeedDragX = 0f

        val speedSteps = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f)

        detectDragGesturesAfterLongPress(
          onDragStart = {
            if (paused == false) {
              originalSpeedForLongPress = MPVLib.getPropertyFloat("speed") ?: 1f
              currentLongPressSpeed = multipleSpeedGesture
              isLongPressDragActive = true
              accumulatedSpeedDragX = 0f

              haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              MPVLib.setPropertyFloat("speed", currentLongPressSpeed)

              viewModel.hideControls()

              viewModel.currentGestureSpeed.update { currentLongPressSpeed }
              viewModel.isVolumeSliderShown.update { false }
              viewModel.isBrightnessSliderShown.update { false }
              viewModel.isSpeedSliderShown.update { true }
            }
          },
          onDragEnd = {
            if (isLongPressDragActive) {
              isLongPressDragActive = false
              MPVLib.setPropertyFloat("speed", originalSpeedForLongPress)
              viewModel.isSpeedSliderShown.update { false }
            }
          },
          onDragCancel = {
            if (isLongPressDragActive) {
              isLongPressDragActive = false
              MPVLib.setPropertyFloat("speed", originalSpeedForLongPress)
              viewModel.isSpeedSliderShown.update { false }
            }
          },
          onDrag = { _, dragAmount ->
            if (isLongPressDragActive) {
              accumulatedSpeedDragX += dragAmount.x
              val stepThreshold = 100f

              if (abs(accumulatedSpeedDragX) >= stepThreshold) {
                val stepsToMove = (accumulatedSpeedDragX / stepThreshold).toInt()
                if (stepsToMove != 0) {
                  val currentIndex = speedSteps.minByOrNull { abs(it - currentLongPressSpeed) }
                    ?.let { speedSteps.indexOf(it) } ?: 7

                  val newIndex = (currentIndex + stepsToMove).coerceIn(0, speedSteps.lastIndex)
                  val newSpeed = speedSteps[newIndex]

                  if (newSpeed != currentLongPressSpeed) {
                    currentLongPressSpeed = newSpeed
                    MPVLib.setPropertyFloat("speed", newSpeed)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                    viewModel.currentGestureSpeed.update { newSpeed }
                  }
                  accumulatedSpeedDragX %= stepThreshold
                }
              }
            }
          }
        )
      }
      .pointerInput(areControlsLocked) {
        if (!seekGesture || areControlsLocked) return@pointerInput
        var startingPosition = position ?: 0
        var startingX = 0f
        var wasPlayerAlreadyPause = false
        var wasControlsShown = false
        detectHorizontalDragGestures(
          onDragStart = {
            startingPosition = position ?: 0
            startingX = it.x
            wasPlayerAlreadyPause = paused ?: false
            wasControlsShown = controlsShown
            viewModel.pause()
          },
          onDragEnd = {
            viewModel.gestureSeekAmount.update { null }
            viewModel.hideSeekBar()
            if (!wasPlayerAlreadyPause) viewModel.unpause()
            if (wasControlsShown) viewModel.showControls()
          },
          onDragCancel = {
            viewModel.gestureSeekAmount.update { null }
            viewModel.hideSeekBar()
            if (!wasPlayerAlreadyPause) viewModel.unpause()
            if (wasControlsShown) viewModel.showControls()
          }
        ) { change, dragAmount ->
          if ((position ?: 0) <= 0f && dragAmount < 0) return@detectHorizontalDragGestures
          if ((position ?: 0) >= (duration ?: 0) && dragAmount > 0) return@detectHorizontalDragGestures
          calculateNewHorizontalGestureValue(
            startingPosition,
            startingX,
            change.position.x,
            0.15f
          ).let {
            viewModel.gestureSeekAmount.update { _ ->
              Pair(
                startingPosition,
                (it - startingPosition)
                  .coerceIn(0 - startingPosition, ((duration ?: 0) - startingPosition)),
              )
            }
            viewModel.seekTo(it, preciseSeeking)
          }

          if (showSeekbarWhenSeeking) viewModel.showSeekBar()
        }
      }
      .pointerInput(areControlsLocked) {
        if ((!brightnessGesture && !volumeGesture) || areControlsLocked) return@pointerInput
        var startingY = 0f
        var mpvVolumeStartingY = 0f
        var originalVolume = currentVolume
        var originalMPVVolume = currentMPVVolume
        var originalBrightness = currentBrightness
        val brightnessGestureSens = 0.001f
        val volumeGestureSens = 0.03f
        val mpvVolumeGestureSens = 0.02f
        val isIncreasingVolumeBoost: (Float) -> Boolean = {
          volumeBoostingCap > 0 && currentVolume == viewModel.maxVolume &&
            (currentMPVVolume ?: 100) - 100 < volumeBoostingCap && it < 0
        }
        val isDecreasingVolumeBoost: (Float) -> Boolean = {
          volumeBoostingCap > 0 && currentVolume == viewModel.maxVolume &&
            (currentMPVVolume ?: 100) - 100 in 1..volumeBoostingCap && it > 0
        }
        detectVerticalDragGestures(
          onDragEnd = { startingY = 0f },
          onDragStart = {
            startingY = 0f
            mpvVolumeStartingY = 0f
            originalVolume = currentVolume
            originalMPVVolume = currentMPVVolume
            originalBrightness = currentBrightness
            viewModel.hideControls()
          },
        ) { change, amount ->
          val changeVolume: () -> Unit = {
            if (isIncreasingVolumeBoost(amount) || isDecreasingVolumeBoost(amount)) {
              if (mpvVolumeStartingY == 0f) {
                startingY = 0f
                originalVolume = currentVolume
                mpvVolumeStartingY = change.position.y
              }
              viewModel.changeMPVVolumeTo(
                calculateNewVerticalGestureValue(
                  originalMPVVolume ?: 100,
                  mpvVolumeStartingY,
                  change.position.y,
                  mpvVolumeGestureSens,
                )
                  .coerceIn(100..volumeBoostingCap + 100),
              )
            } else {
              if (startingY == 0f) {
                mpvVolumeStartingY = 0f
                originalMPVVolume = currentMPVVolume
                startingY = change.position.y
              }
              viewModel.changeVolumeTo(
                calculateNewVerticalGestureValue(originalVolume, startingY, change.position.y, volumeGestureSens),
              )
            }
            viewModel.displayVolumeSlider()
          }
          val changeBrightness: () -> Unit = {
            if (startingY == 0f) startingY = change.position.y
            viewModel.changeBrightnessTo(
              calculateNewVerticalGestureValue(originalBrightness, startingY, change.position.y, brightnessGestureSens),
            )
            viewModel.displayBrightnessSlider()
          }
          when {
            volumeGesture && brightnessGesture -> {
              if (swapVolumeAndBrightness) {
                if (change.position.x > size.width / 2) changeBrightness() else changeVolume()
              } else {
                if (change.position.x < size.width / 2) changeBrightness() else changeVolume()
              }
            }

            brightnessGesture -> changeBrightness()
            volumeGesture -> changeVolume()
            else -> {}
          }
        }
      },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoubleTapToSeekOvals(
  amount: Int,
  text: String?,
  showOvals: Boolean,
  showSeekIcon: Boolean,
  showSeekTime: Boolean,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier,
) {
  val alpha by animateFloatAsState(if (amount == 0) 0f else 0.2f, label = "double_tap_animation_alpha")
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = if (amount > 0) Alignment.CenterEnd else Alignment.CenterStart,
  ) {
    CompositionLocalProvider(
      LocalRippleConfiguration provides playerRippleConfiguration,
    ) {
      if (amount != 0) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.4f),
          contentAlignment = Alignment.Center,
        ) {
          if (showOvals) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clip(if (amount > 0) RightSideOvalShape else LeftSideOvalShape)
                .background(Color.White.copy(alpha))
                .indication(interactionSource, ripple()),
            )
          }
          if (showSeekIcon || showSeekTime) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              if (showSeekIcon) {
                DoubleTapSeekTriangles(isForward = amount > 0)
              }
              if (showSeekTime) {
                Text(
                  text = text ?: pluralStringResource(R.plurals.seconds, amount, amount),
                  fontSize = 12.sp,
                  textAlign = TextAlign.Center,
                  color = Color.White,
                )
              }
            }
          }
        }
      }
    }
  }
}

fun calculateNewVerticalGestureValue(originalValue: Int, startingY: Float, newY: Float, sensitivity: Float): Int {
  return originalValue + ((startingY - newY) * sensitivity).toInt()
}

fun calculateNewVerticalGestureValue(originalValue: Float, startingY: Float, newY: Float, sensitivity: Float): Float {
  return originalValue + ((startingY - newY) * sensitivity)
}

fun calculateNewHorizontalGestureValue(originalValue: Int, startingX: Float, newX: Float, sensitivity: Float): Int {
  return originalValue + ((newX - startingX) * sensitivity).toInt()
}

fun calculateNewHorizontalGestureValue(originalValue: Float, startingX: Float, newX: Float, sensitivity: Float): Float {
  return originalValue + ((newX - startingX) * sensitivity)
}
