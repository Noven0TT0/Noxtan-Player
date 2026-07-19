package com.noxtan.player.ui.player.managers

import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowInsetsCompat
import com.noxtan.player.ui.player.PlayerActivity
import com.noxtan.player.ui.player.PlayerOrientation

class SystemMediaController(private val activity: PlayerActivity) {
  val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private val inputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

  val maxVolume: Int
    get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

  val windowManager: WindowManager
    get() = activity.windowManager

  val context: Context
    get() = activity

  fun getCurrentVolume(): Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

  fun setVolume(volume: Int) {
    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume.coerceIn(0..maxVolume), 0)
  }

  fun getCurrentBrightness(): Float {
    return runCatching {
      Settings.System.getFloat(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        .normalize(0f, 255f, 0f, 1f)
    }.getOrElse { 0f }
  }

  fun setBrightness(brightness: Float) {
    activity.window.attributes = activity.window.attributes.apply {
      screenBrightness = brightness.coerceIn(0f, 1f)
    }
  }

  fun showNavigationBars() {
    activity.windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
  }

  fun hideNavigationBars() {
    activity.windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
  }

  fun cycleScreenRotations(currentOrientationPref: (PlayerOrientation) -> Unit) {
    activity.requestedOrientation = when (activity.requestedOrientation) {
      ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
      ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
      ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE -> {
        currentOrientationPref(PlayerOrientation.SensorPortrait)
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
      }
      else -> {
        currentOrientationPref(PlayerOrientation.SensorLandscape)
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      }
    }
  }

  fun forceShowSoftwareKeyboard() {
    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
  }

  fun forceHideSoftwareKeyboard() {
    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
  }

  val isKeyboardActive: Boolean
    get() = inputMethodManager.isActive

  private fun Float.normalize(inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
    return (this - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
  }
}
