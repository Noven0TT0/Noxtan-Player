package com.noxtan.player.ui.Settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxtan.player.R
import com.noxtan.player.preferences.SubtitlesPreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.presentation.Screen
import com.noxtan.player.presentation.components.CollapsingTopBar
import com.noxtan.player.ui.Settings.components.CustomInputDialogItem
import com.noxtan.player.ui.Settings.components.CustomSettingsGroup
import com.noxtan.player.ui.Settings.components.CustomSwitchItem
import com.noxtan.player.ui.utils.LocalBackStack
import com.noxtan.player.ui.utils.topAndBottomNoise
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object SubtitlesPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val preferences = koinInject<SubtitlesPreferences>()

    val preferredLanguages by preferences.preferredLanguages.collectAsState()
    val fontsFolder by preferences.fontsFolder.collectAsState()
    val autoloadExternal by preferences.autoLoadExternal.collectAsState()

    val locationPicker = rememberLauncherForActivityResult(
      ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
      context.contentResolver.takePersistableUriPermission(uri, flags)
      preferences.fontsFolder.set(uri.toString())
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
      modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .topAndBottomNoise(noiseAlpha = 0.15f, fadeFraction = 0.09f),
      topBar = {
        CollapsingTopBar(
          title = stringResource(R.string.pref_subtitles),
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

        CustomSettingsGroup("LANGUAGES") {
          CustomInputDialogItem(
            title = stringResource(R.string.pref_preferred_languages),
            summary = preferredLanguages.ifBlank { "Not set" },
            icon = Icons.Rounded.Translate,
            value = preferredLanguages,
            onValueChange = { preferences.preferredLanguages.set(it) }
          )
        }

        CustomSettingsGroup("EXTERNAL SUBTITLES") {
          CustomFolderPickerItem(
            title = stringResource(R.string.pref_subtitles_fonts_dir),
            summary = if (fontsFolder.isNotBlank()) getSimplifiedPathFromUri(fontsFolder) else "No custom folder selected",
            icon = Icons.Rounded.FolderOpen,
            hasValue = fontsFolder.isNotBlank(),
            onClick = { locationPicker.launch(null) },
            onClear = { preferences.fontsFolder.delete() }
          )

          CustomSwitchItem(
            title = stringResource(R.string.pref_subtitles_autoload_title),
            summary = stringResource(R.string.pref_subtitles_autoload_summary),
            icon = Icons.Rounded.AutoMode,
            checked = autoloadExternal,
            onCheckedChange = { preferences.autoLoadExternal.set(it) }
          )
        }

        Spacer(modifier = Modifier.height(padding.calculateBottomPadding() + 32.dp))
      }
    }
  }
}

@Composable
private fun CustomFolderPickerItem(
  title: String,
  summary: String,
  icon: ImageVector,
  hasValue: Boolean,
  onClick: () -> Unit,
  onClear: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 20.dp, vertical = 14.dp),
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

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )
      if (summary.isNotBlank()) {
        Text(
          text = summary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 16.sp
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    if (hasValue) {
      IconButton(
        onClick = onClear,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Rounded.Clear,
          contentDescription = "Clear",
          tint = MaterialTheme.colorScheme.error
        )
      }
    } else {
      Icon(
        imageVector = Icons.Rounded.FolderOpen,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(20.dp)
      )
    }
  }
}
