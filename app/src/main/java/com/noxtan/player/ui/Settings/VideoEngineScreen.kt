package com.noxtan.player.ui.Settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.VideoSettings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noxtan.player.R
import com.noxtan.player.preferences.DecoderPreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.presentation.Screen
import com.noxtan.player.presentation.components.CollapsingTopBar
import com.noxtan.player.ui.Settings.components.CustomListDialogItem
import com.noxtan.player.ui.Settings.components.CustomSettingsGroup
import com.noxtan.player.ui.Settings.components.CustomSwitchItem
import com.noxtan.player.ui.player.Debanding
import com.noxtan.player.ui.utils.LocalBackStack
import com.noxtan.player.ui.utils.topAndBottomNoise
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object VideoEngineScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<DecoderPreferences>()
    val backstack = LocalBackStack.current

    val tryHWDecoding by preferences.tryHWDecoding.collectAsState()
    val gpuNext by preferences.gpuNext.collectAsState()
    val debanding by preferences.debanding.collectAsState()
    val useYUV420p by preferences.useYUV420P.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
      modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .topAndBottomNoise(noiseAlpha = 0.15f, fadeFraction = 0.09f),
      topBar = {
        CollapsingTopBar(
          title = stringResource(R.string.pref_decoder),
          scrollBehavior = scrollBehavior,
          onBackClick = { backstack.removeLastOrNull() }
        )
      },
      containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
      Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = padding.calculateTopPadding())) {

        CustomSettingsGroup("HARDWARE DECODING") {
          CustomSwitchItem(
            title = stringResource(R.string.pref_decoder_try_hw_dec_title),
            summary = "Attempt to use device hardware for decoding to save battery.",
            icon = Icons.Rounded.Memory,
            checked = tryHWDecoding,
            onCheckedChange = { preferences.tryHWDecoding.set(it) }
          )
          CustomSwitchItem(
            title = stringResource(R.string.pref_decoder_gpu_next_title),
            summary = stringResource(R.string.pref_decoder_gpu_next_summary),
            icon = Icons.Rounded.DeveloperBoard,
            checked = gpuNext,
            onCheckedChange = { preferences.gpuNext.set(it) }
          )
        }

        CustomSettingsGroup("VIDEO RENDERING") {
          CustomListDialogItem(
            title = stringResource(R.string.pref_decoder_debanding_title),
            summary = "Current: ${debanding.name}",
            icon = Icons.Rounded.Gradient,
            value = debanding,
            values = Debanding.entries.toList(),
            valueToText = { it.name },
            onValueChange = { preferences.debanding.set(it) }
          )
          CustomSwitchItem(
            title = stringResource(R.string.pref_decoder_yuv420p_title),
            summary = stringResource(R.string.pref_decoder_yuv420p_summary),
            icon = Icons.Rounded.VideoSettings,
            checked = useYUV420p,
            onCheckedChange = { preferences.useYUV420P.set(it) }
          )
        }
        Spacer(modifier = Modifier.height(padding.calculateBottomPadding() + 32.dp))
      }
    }
  }
}
