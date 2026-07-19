package com.noxtan.player.preferences

import com.noxtan.player.preferences.preference.PreferenceStore
import com.noxtan.player.preferences.preference.getEnum
import com.noxtan.player.ui.theme.DarkMode

class AppearancePreferences(preferenceStore: PreferenceStore) {
  val darkMode = preferenceStore.getEnum("dark_mode", DarkMode.System)
  val materialYou = preferenceStore.getBoolean("material_you", false)
}
