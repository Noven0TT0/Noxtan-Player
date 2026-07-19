package com.noxtan.player

import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import com.noxtan.player.features.local.viewmodel.LocalVideoViewModel
import com.noxtan.player.preferences.AppearancePreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.ui.navigation.AppNavigator
import com.noxtan.player.ui.theme.DarkMode
import com.noxtan.player.ui.theme.NoxtanTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
  private val appearancePreferences by inject<AppearancePreferences>()
  private val localVideoViewModel: LocalVideoViewModel by viewModel()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }

    setContent {
      val dark by appearancePreferences.darkMode.collectAsState()
      val isSystemInDarkTheme = isSystemInDarkTheme()

      enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(
          lightScrim = Color.Transparent.toArgb(),
          darkScrim = Color.Transparent.toArgb(),
        ) { dark == DarkMode.Dark || (dark == DarkMode.System && isSystemInDarkTheme) },
        navigationBarStyle = SystemBarStyle.auto(
          lightScrim = Color.Transparent.toArgb(),
          darkScrim = Color.Transparent.toArgb(),
        ) { dark == DarkMode.Dark || (dark == DarkMode.System && isSystemInDarkTheme) }
      )

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
      }

      NoxtanTheme {
        Surface {
          AppNavigator()
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()

    lifecycleScope.launch {
      localVideoViewModel.setAppResuming(true)
      delay(150)
      localVideoViewModel.setAppResuming(false)

      if (!localVideoViewModel.isSearchMode && !localVideoViewModel.isSelectionModeActive) {
        localVideoViewModel.setFabVisibility(true)
      }
    }
  }

  override fun onPause() {
    super.onPause()
    localVideoViewModel.setFabVisibility(false)
  }
}
