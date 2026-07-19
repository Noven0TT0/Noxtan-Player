package com.noxtan.player.ui.theme

import androidx.compose.ui.graphics.Color

val ProtonMidnight = Color(0xFF0C0C14)
val ProtonSurface = Color(0xFF171725)
val ProtonPurple = Color(0xFF7B57FF)
val ProtonTeal = Color(0xFF40E0D0)
val ProtonWhite = Color(0xFFF0F0F5)
val ProtonGray = Color(0xFF70708C)

val CinematicBlack = Color(0xFF08080A)
val GlassySurface = Color(0xB3121212)
val CinemaGray = Color(0xFF9E9EAE)

fun getCinematicAccent(userColor: Color): Color {
  return userColor.copy(alpha = 0.85f)
}
