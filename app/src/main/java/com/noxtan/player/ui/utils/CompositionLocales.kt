package com.noxtan.player.ui.utils

import androidx.compose.runtime.compositionLocalOf
import com.noxtan.player.presentation.Screen

interface AppBackStack {
  val size: Int
  fun add(screen: Screen)
  fun removeLastOrNull(): Screen?
}

@Suppress("CompositionLocalAllowlist")
val LocalBackStack = compositionLocalOf<AppBackStack> {
  error("LocalBackStack not initialized!")
}
