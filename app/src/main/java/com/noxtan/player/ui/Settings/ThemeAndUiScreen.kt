package com.noxtan.player.ui.Settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxtan.player.R
import com.noxtan.player.preferences.AppearancePreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.presentation.Screen
import com.noxtan.player.presentation.components.CollapsingTopBar
import com.noxtan.player.ui.theme.DarkMode
import com.noxtan.player.ui.utils.LocalBackStack
import com.noxtan.player.ui.utils.topAndBottomNoise
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object AppearancePreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<AppearancePreferences>()
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
      modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .topAndBottomNoise(noiseAlpha = 0.15f, fadeFraction = 0.09f),
      topBar = {
        CollapsingTopBar(
          title = stringResource(R.string.pref_appearance_title),
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

        AppearanceSettingsGroup(title = "THEME") {
          val darkMode by preferences.darkMode.collectAsState()

          AppearanceSettingsHeader(
            title = "Dark Mode",
            subtitle = "Choose your preferred theme style",
            icon = Icons.Rounded.DarkMode
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            DarkMode.entries.forEach { option ->
              val isSelected = darkMode == option
              val icon = when (option) {
                DarkMode.Dark -> Icons.Rounded.DarkMode
                DarkMode.Light -> Icons.Rounded.LightMode
                DarkMode.System -> Icons.Rounded.BrightnessAuto
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.background
                  )
                  .border(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                  )
                  .clickable { preferences.darkMode.set(option) }
                  .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp).padding(bottom = 6.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = context.getString(option.titleRes).replace(" Theme", "").replace(" Default", ""),
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                  )
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
        }

        val materialYou by preferences.materialYou.collectAsState()
        val isMaterialYouAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        AppearanceSettingsGroup(title = "DYNAMIC COLORS") {
          AppearanceSettingsSwitchItem(
            title = stringResource(id = R.string.pref_appearance_material_you_title),
            subtitle = stringResource(
              if (isMaterialYouAvailable) R.string.pref_appearance_material_you_summary
              else R.string.pref_appearance_material_you_summary_disabled
            ),
            icon = Icons.Rounded.ColorLens,
            checked = materialYou,
            enabled = isMaterialYouAvailable,
            onCheckedChange = { preferences.materialYou.set(it) }
          )
        }

        Spacer(modifier = Modifier.height(padding.calculateBottomPadding() + 40.dp))
      }
    }
  }
}

@Composable
private fun AppearanceSettingsGroup(
  title: String,
  content: @Composable () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp
      ),
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp
    ) {
      Column(
        modifier = Modifier.padding(vertical = 8.dp)
      ) {
        content()
      }
    }
  }
}

@Composable
private fun AppearanceSettingsHeader(
  title: String,
  subtitle: String,
  icon: ImageVector
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp)
      )
    }
    Spacer(modifier = Modifier.width(16.dp))
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 16.sp
      )
    }
  }
}

@Composable
private fun AppearanceSettingsSwitchItem(
  title: String,
  subtitle: String,
  icon: ImageVector,
  checked: Boolean,
  enabled: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .toggleable(value = checked, onValueChange = onCheckedChange, enabled = enabled, role = Role.Switch)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.5f else 0.2f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        modifier = Modifier.size(22.dp)
      )
    }

    Spacer(modifier = Modifier.width(16.dp))

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        lineHeight = 16.sp
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      enabled = enabled,
      modifier = Modifier.scale(0.85f).clearAndSetSemantics {}
    )
  }
}
