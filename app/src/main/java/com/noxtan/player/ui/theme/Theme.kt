package com.noxtan.player.ui.theme

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.noxtan.player.R
import com.noxtan.player.preferences.AppearancePreferences
import com.noxtan.player.preferences.preference.collectAsState
import org.koin.compose.koinInject

private val DarkColorScheme = darkColorScheme(
  primary = ProtonPurple,
  secondary = ProtonTeal,
  background = ProtonMidnight,
  surface = ProtonSurface,
  onBackground = ProtonWhite,
  onSurface = ProtonWhite,
  outline = ProtonGray
)

private val LightColorScheme = lightColorScheme(
  primary = ProtonPurple,
  onPrimary = Color.White,
  background = Color.White,
  onBackground = Color.Black,
  surface = Color(0xFFF5F5F5),
  onSurface = Color.Black
)

private val StreamingColorScheme = darkColorScheme(
  primary = ProtonPurple,
  background = CinematicBlack,
  surface = GlassySurface,
  onBackground = Color.White,
  onSurface = Color.White,
  outline = CinemaGray
)

@Composable
fun NoxtanTheme(
  isStreaming: Boolean = false,
  content: @Composable () -> Unit
) {
  val preferences = koinInject<AppearancePreferences>()
  val darkMode by preferences.darkMode.collectAsState()
  val darkTheme = isSystemInDarkTheme()
  val dynamicColor by preferences.materialYou.collectAsState()
  val context = LocalContext.current

  val colorScheme = when {
    isStreaming -> StreamingColorScheme

    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      if (darkMode == DarkMode.Dark || (darkMode == DarkMode.System && darkTheme)) {
        dynamicDarkColorScheme(context)
      } else {
        dynamicLightColorScheme(context)
      }
    }

    darkMode == DarkMode.Dark -> DarkColorScheme
    darkMode == DarkMode.Light -> LightColorScheme
    else -> if (darkTheme) DarkColorScheme else LightColorScheme
  }

  CompositionLocalProvider(
    LocalSpacing provides Spacing(),
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content,
    )
  }
}

enum class DarkMode(@StringRes val titleRes: Int) {
  Dark(R.string.pref_appearance_darkmode_dark),
  Light(R.string.pref_appearance_darkmode_light),
  System(R.string.pref_appearance_darkmode_system),
}

private const val RIPPLE_DRAGGED_ALPHA = .5f
private const val RIPPLE_FOCUSED_ALPHA = .6f
private const val RIPPLE_HOVERED_ALPHA = .4f
private const val RIPPLE_PRESSED_ALPHA = .6f

@OptIn(ExperimentalMaterial3Api::class)
val playerRippleConfiguration
  @Composable get() = RippleConfiguration(
    color = MaterialTheme.colorScheme.primary,
    rippleAlpha = RippleAlpha(
      draggedAlpha = RIPPLE_DRAGGED_ALPHA,
      focusedAlpha = RIPPLE_FOCUSED_ALPHA,
      hoveredAlpha = RIPPLE_HOVERED_ALPHA,
      pressedAlpha = RIPPLE_PRESSED_ALPHA,
    ),
  )
