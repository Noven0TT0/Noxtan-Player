package com.noxtan.player.ui.Settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxtan.player.R
import com.noxtan.player.presentation.Screen
import com.noxtan.player.presentation.components.CollapsingTopBar
import com.noxtan.player.ui.utils.LocalBackStack
import com.noxtan.player.ui.utils.topAndBottomNoise
import kotlinx.serialization.Serializable

@Serializable
object SettingsScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
      modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .topAndBottomNoise(noiseAlpha = 0.15f, fadeFraction = 0.09f),
      topBar = {
        CollapsingTopBar(
          title = stringResource(R.string.pref_preferences),
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
          .padding(top = padding.calculateTopPadding(), start = 16.dp, end = 16.dp)
      ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "DASHBOARD",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsTile(
              modifier = Modifier.weight(1f),
              title = stringResource(id = R.string.pref_appearance_title),
              icon = Icons.Rounded.Brush,
              iconColor = Color(0xFFE91E63),
              onClick = { backstack.add(AppearancePreferencesScreen) }
            )
            SettingsTile(
              modifier = Modifier.weight(1f),
              title = stringResource(id = R.string.pref_player),
              icon = Icons.Rounded.SmartDisplay,
              iconColor = Color(0xFF2196F3),
              onClick = { backstack.add(PlayerControlsScreen) }
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsTile(
              modifier = Modifier.weight(1f),
              title = stringResource(id = R.string.pref_audio),
              icon = Icons.Rounded.GraphicEq,
              iconColor = Color(0xFF00BCD4),
              onClick = { backstack.add(SoundAndAudioScreen) }
            )
            SettingsTile(
              modifier = Modifier.weight(1f),
              title = stringResource(id = R.string.pref_subtitles),
              icon = Icons.Rounded.ClosedCaption,
              iconColor = Color(0xFF9C27B0),
              onClick = { backstack.add(SubtitlesPreferencesScreen) }
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsTile(
              modifier = Modifier.weight(1f),
              title = stringResource(id = R.string.pref_decoder),
              icon = Icons.Rounded.Extension,
              iconColor = Color(0xFFFF9800),
              onClick = { backstack.add(VideoEngineScreen) }
            )
            SettingsTile(
              modifier = Modifier.weight(1f),
              title = stringResource(id = R.string.pref_gesture),
              icon = Icons.Rounded.Swipe,
              iconColor = Color(0xFF4CAF50),
              onClick = { backstack.add(GesturePreferencesScreen) }
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
          text = "OTHER",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp
        ) {
          Column {
            CompactListItem(
              title = "Language",
              icon = Icons.Rounded.Language,
              iconColor = Color(0xFF00BCD4),
              onClick = { backstack.add(LanguageScreen) }
            )

            CompactListItem(
              title = stringResource(R.string.pref_advanced),
              icon = Icons.Rounded.Build,
              iconColor = Color(0xFFF44336),
              onClick = { backstack.add(AdvancedPreferencesScreen) }
            )
            CompactListItem(
              title = stringResource(id = R.string.pref_about_title),
              icon = Icons.Rounded.Info,
              iconColor = Color(0xFF3F51B5),
              onClick = { backstack.add(AboutScreen) }
            )
          }
        }

        Spacer(modifier = Modifier.height(padding.calculateBottomPadding() + 40.dp))
      }
    }
  }
}

@Composable
fun SettingsTile(
  modifier: Modifier = Modifier,
  title: String,
  icon: ImageVector,
  iconColor: Color,
  onClick: () -> Unit
) {
  Surface(
    modifier = modifier
      .height(100.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable(role = Role.Button) { onClick() },
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.Start
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(iconColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconColor,
          modifier = Modifier.size(20.dp)
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1
      )
    }
  }
}

@Composable
fun CompactListItem(
  title: String,
  icon: ImageVector,
  iconColor: Color,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(role = Role.Button) { onClick() }
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(iconColor.copy(alpha = 0.15f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconColor,
        modifier = Modifier.size(20.dp)
      )
    }

    Spacer(modifier = Modifier.width(16.dp))

    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f)
    )

    Icon(
      imageVector = Icons.Rounded.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
      modifier = Modifier.size(20.dp)
    )
  }
}
