package com.noxtan.player.ui.Settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxtan.player.R
import com.noxtan.player.preferences.GesturePreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.presentation.Screen
import com.noxtan.player.presentation.components.CollapsingTopBar
import com.noxtan.player.ui.Settings.components.CustomListDialogItem
import com.noxtan.player.ui.Settings.components.CustomSettingsGroup
import com.noxtan.player.ui.player.CustomKeyCodes
import com.noxtan.player.ui.player.SingleActionGesture
import com.noxtan.player.ui.utils.LocalBackStack
import com.noxtan.player.ui.utils.topAndBottomNoise
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object GesturePreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<GesturePreferences>()
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
      modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .topAndBottomNoise(noiseAlpha = 0.15f, fadeFraction = 0.09f),
      topBar = {
        CollapsingTopBar(
          title = stringResource(R.string.pref_gesture),
          scrollBehavior = scrollBehavior,
          onBackClick = { backstack.removeLastOrNull() }
        )
      },
      containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(top = padding.calculateTopPadding())
      ) {
        Spacer(modifier = Modifier.height(12.dp))

        CustomSettingsGroup(title = "SCREEN DOUBLE TAP") {
          val doubleTapSeekDuration by preferences.doubleTapToSeekDuration.collectAsState()
          CustomListDialogItem(
            title = stringResource(id = R.string.pref_player_double_tap_seek_duration),
            summary = "${doubleTapSeekDuration}s",
            icon = Icons.Rounded.Timer,
            value = doubleTapSeekDuration,
            values = listOf(3, 5, 10, 15, 20, 25, 30),
            valueToText = { "${it}s" },
            onValueChange = preferences.doubleTapToSeekDuration::set
          )

          val leftDoubleTap by preferences.leftSingleActionGesture.collectAsState()
          CustomListDialogItem(
            title = stringResource(R.string.pref_gesture_double_tap_left_title),
            summary = context.getString(leftDoubleTap.titleRes),
            icon = Icons.Rounded.KeyboardDoubleArrowLeft,
            value = leftDoubleTap,
            values = SingleActionGesture.entries.toList(),
            valueToText = { context.getString(it.titleRes) },
            onValueChange = preferences.leftSingleActionGesture::set
          )

          val centerDoubleTap by preferences.centerSingleActionGesture.collectAsState()
          CustomListDialogItem(
            title = stringResource(R.string.pref_gesture_double_tap_center_title),
            summary = context.getString(centerDoubleTap.titleRes),
            icon = Icons.Rounded.TouchApp,
            value = centerDoubleTap,
            values = listOf(
              SingleActionGesture.None,
              SingleActionGesture.PlayPause,
              SingleActionGesture.Custom
            ),
            valueToText = { context.getString(it.titleRes) },
            onValueChange = preferences.centerSingleActionGesture::set
          )

          val rightDoubleTap by preferences.rightSingleActionGesture.collectAsState()
          CustomListDialogItem(
            title = stringResource(R.string.pref_gesture_double_tap_right_title),
            summary = context.getString(rightDoubleTap.titleRes),
            icon = Icons.Rounded.KeyboardDoubleArrowRight,
            value = rightDoubleTap,
            values = SingleActionGesture.entries.toList(),
            valueToText = { context.getString(it.titleRes) },
            onValueChange = preferences.rightSingleActionGesture::set
          )

          val doubleTapKeyCodes = listOf(
            CustomKeyCodes.DoubleTapLeft,
            CustomKeyCodes.DoubleTapCenter,
            CustomKeyCodes.DoubleTapRight,
          ).map { it.keyCode }.toImmutableList()

          var doubleTapAnnotatedString = buildAnnotatedString {
            append(stringResource(R.string.pref_gesture_double_tap_custom_info))
          }
          doubleTapKeyCodes.forEach { keyCode ->
            doubleTapAnnotatedString = buildAnnotatedString {
              val startIndex = doubleTapAnnotatedString.indexOf(keyCode)
              if (startIndex != -1) {
                val endIndex = startIndex + keyCode.length
                append(doubleTapAnnotatedString)
                addStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary), start = startIndex, end = endIndex)
              } else {
                append(doubleTapAnnotatedString)
              }
            }
          }
          CustomInfoItem(text = doubleTapAnnotatedString)
        }

        CustomSettingsGroup(title = "HARDWARE MEDIA KEYS") {
          val mediaPreviousGesture by preferences.mediaPreviousGesture.collectAsState()
          CustomListDialogItem(
            title = stringResource(R.string.pref_gesture_media_previous),
            summary = context.getString(mediaPreviousGesture.titleRes),
            icon = Icons.Rounded.SkipPrevious,
            value = mediaPreviousGesture,
            values = SingleActionGesture.entries.toList(),
            valueToText = { context.getString(it.titleRes) },
            onValueChange = preferences.mediaPreviousGesture::set
          )

          val mediaPlayGesture by preferences.mediaPlayGesture.collectAsState()
          CustomListDialogItem(
            title = stringResource(R.string.pref_gesture_media_play),
            summary = context.getString(mediaPlayGesture.titleRes),
            icon = Icons.Rounded.PlayCircle,
            value = mediaPlayGesture,
            values = listOf(
              SingleActionGesture.None,
              SingleActionGesture.PlayPause,
              SingleActionGesture.Custom
            ),
            valueToText = { context.getString(it.titleRes) },
            onValueChange = preferences.mediaPlayGesture::set
          )

          val mediaNextGesture by preferences.mediaNextGesture.collectAsState()
          CustomListDialogItem(
            title = stringResource(R.string.pref_gesture_media_next),
            summary = context.getString(mediaNextGesture.titleRes),
            icon = Icons.Rounded.SkipNext,
            value = mediaNextGesture,
            values = SingleActionGesture.entries.toList(),
            valueToText = { context.getString(it.titleRes) },
            onValueChange = preferences.mediaNextGesture::set
          )

          val mediaKeyCodes = listOf(
            CustomKeyCodes.MediaPrevious,
            CustomKeyCodes.MediaPlay,
            CustomKeyCodes.MediaNext,
          ).map { it.keyCode }.toImmutableList()

          var mediaAnnotatedString = buildAnnotatedString {
            append(stringResource(R.string.pref_gesture_media_custom_info))
          }
          mediaKeyCodes.forEach { keyCode ->
            mediaAnnotatedString = buildAnnotatedString {
              val startIndex = mediaAnnotatedString.indexOf(keyCode)
              if (startIndex != -1) {
                val endIndex = startIndex + keyCode.length
                append(mediaAnnotatedString)
                addStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary), start = startIndex, end = endIndex)
              } else {
                append(mediaAnnotatedString)
              }
            }
          }
          CustomInfoItem(text = mediaAnnotatedString)
        }

        Spacer(modifier = Modifier.height(padding.calculateBottomPadding() + 32.dp))
      }
    }
  }
}

@Composable
private fun CustomInfoItem(text: AnnotatedString) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 12.dp),
    verticalAlignment = Alignment.Top
  ) {
    Icon(
      imageVector = Icons.Rounded.Info,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp).padding(top = 2.dp)
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      lineHeight = 18.sp
    )
  }
}
