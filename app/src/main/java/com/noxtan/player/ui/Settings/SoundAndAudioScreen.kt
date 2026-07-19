package com.noxtan.player.ui.Settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noxtan.player.R
import com.noxtan.player.preferences.AudioChannels
import com.noxtan.player.preferences.AudioPreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.presentation.Screen
import com.noxtan.player.presentation.components.CollapsingTopBar
import com.noxtan.player.ui.Settings.components.CustomInputDialogItem
import com.noxtan.player.ui.Settings.components.CustomListDialogItem
import com.noxtan.player.ui.Settings.components.CustomSettingsGroup
import com.noxtan.player.ui.Settings.components.CustomSliderItem
import com.noxtan.player.ui.Settings.components.CustomSwitchItem
import com.noxtan.player.ui.utils.LocalBackStack
import com.noxtan.player.ui.utils.topAndBottomNoise
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Serializable
object SoundAndAudioScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<AudioPreferences>()
    val backstack = LocalBackStack.current
    val context = LocalContext.current

    val preferredLanguages by preferences.preferredLanguages.collectAsState()
    val audioPitchCorrection by preferences.audioPitchCorrection.collectAsState()
    val audioChannel by preferences.audioChannels.collectAsState()
    val volumeBoostCap by preferences.volumeBoostCap.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
      modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .topAndBottomNoise(noiseAlpha = 0.15f, fadeFraction = 0.09f),
      topBar = {
        CollapsingTopBar(
          title = stringResource(R.string.pref_audio),
          scrollBehavior = scrollBehavior,
          onBackClick = { backstack.removeLastOrNull() }
        )
      },
      containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
      Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = padding.calculateTopPadding())) {

        CustomSettingsGroup("AUDIO PREFERENCES") {
          CustomInputDialogItem(
            title = stringResource(R.string.pref_preferred_languages),
            summary = preferredLanguages.ifBlank { "Not set" },
            icon = Icons.Rounded.Language,
            value = preferredLanguages,
            onValueChange = { preferences.preferredLanguages.set(it) }
          )
          CustomSwitchItem(
            title = stringResource(R.string.pref_audio_pitch_correction_title),
            summary = stringResource(R.string.pref_audio_pitch_correction_summary),
            icon = Icons.Rounded.Speed,
            checked = audioPitchCorrection,
            onCheckedChange = { preferences.audioPitchCorrection.set(it) }
          )
        }

        CustomSettingsGroup("SPATIAL AUDIO") {
          CustomListDialogItem(
            title = stringResource(R.string.pref_audio_channels),
            summary = "Current: ${context.getString(audioChannel.title)}",
            icon = Icons.Rounded.GraphicEq,
            value = audioChannel,
            values = AudioChannels.entries.toList(),
            valueToText = { context.getString(it.title) },
            onValueChange = { preferences.audioChannels.set(it) }
          )
        }

        CustomSettingsGroup("VOLUME") {
          CustomSliderItem(
            title = stringResource(R.string.pref_audio_volume_boost_cap),
            summary = "Allow volume to be boosted beyond 100%.",
            icon = Icons.Rounded.Speaker,
            value = volumeBoostCap.toFloat(),
            valueRange = 0f..200f,
            onValueChange = { preferences.volumeBoostCap.set(it.roundToInt()) },
            valueText = if (volumeBoostCap == 0) stringResource(R.string.generic_disabled) else "$volumeBoostCap%"
          )
        }
        Spacer(modifier = Modifier.height(padding.calculateBottomPadding() + 32.dp))
      }
    }
  }
}
