package com.noxtan.player.preferences

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.noxtan.player.R
import com.noxtan.player.preferences.preference.PreferenceStore
import com.noxtan.player.preferences.preference.getEnum

class SubtitlesPreferences(preferenceStore: PreferenceStore) {
  val autoLoadExternal = preferenceStore.getBoolean("sub_autoload", true)
  val preferredLanguages = preferenceStore.getString("sub_preferred_languages")

  val fontsFolder = preferenceStore.getString("sub_fonts_folder")
  val font = preferenceStore.getString("sub_font", "Sans Serif")
  val fontSize = preferenceStore.getInt("sub_font_size", 55)
  val subScale = preferenceStore.getFloat("sub_scale", 1f)
  val borderSize = preferenceStore.getInt("sub_border_size", 3)
  val bold = preferenceStore.getBoolean("sub_bold")
  val italic = preferenceStore.getBoolean("sub_italic")

  val textColor = preferenceStore.getInt("sub_color_text", Color.White.toArgb())

  val borderColor = preferenceStore.getInt("sub_color_border", Color.Black.toArgb())
  val borderStyle = preferenceStore.getEnum("sub_border_style", SubtitlesBorderStyle.OutlineAndShadow)
  val shadowOffset = preferenceStore.getInt("sub_shadow_offset")
  val backgroundColor = preferenceStore.getInt("sub_color_bg", Color.Transparent.toArgb())

  val justification = preferenceStore.getEnum("sub_justify", SubtitleJustification.Auto)
  val subPos = preferenceStore.getInt("sub_pos", 100)

  val overrideAssSubs = preferenceStore.getBoolean("sub_override_ass")

  val defaultSubDelay = preferenceStore.getInt("sub_default_delay")
  val defaultSubSpeed = preferenceStore.getFloat("sub_default_speed", 1f)
  val defaultSecondarySubDelay = preferenceStore.getInt("sub_default_secondary_delay")
}

enum class SubtitleJustification(
  val value: String,
  val icon: ImageVector
) {
  Left("left", Icons.AutoMirrored.Default.FormatAlignLeft),
  Center("center", Icons.Default.FormatAlignCenter),
  Right("right", Icons.AutoMirrored.Default.FormatAlignRight),
  Auto("auto", Icons.Default.FormatAlignJustify)
}

enum class SubtitlesBorderStyle(
  val value: String,
  @StringRes val titleRes: Int,
) {
  OutlineAndShadow("outline-and-shadow", R.string.player_sheets_subtitles_border_style_outline_and_shadow),
  OpaqueBox("opaque-box", R.string.player_sheets_subtitles_border_style_opaque_box),
  BackgroundBox("background-box", R.string.player_sheets_subtitles_border_style_background_box)
}
