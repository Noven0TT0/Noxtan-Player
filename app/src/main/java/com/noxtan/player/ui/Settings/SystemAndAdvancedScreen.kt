package com.noxtan.player.ui.Settings

import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.github.k1rakishou.fsaf.FileManager
import com.noxtan.player.R
import com.noxtan.player.database.PlaybackDatabase
import com.noxtan.player.preferences.AdvancedPreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.presentation.Screen
import com.noxtan.player.presentation.components.CollapsingTopBar
import com.noxtan.player.presentation.components.ConfirmDialog
import com.noxtan.player.presentation.crash.CrashActivity
import com.noxtan.player.ui.Settings.components.CustomSettingsGroup
import com.noxtan.player.ui.Settings.components.CustomSwitchItem
import com.noxtan.player.ui.utils.LocalBackStack
import com.noxtan.player.ui.utils.topAndBottomNoise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import java.io.File

@Serializable
object AdvancedPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val preferences = koinInject<AdvancedPreferences>()
    val fileManager = koinInject<FileManager>()
    val scope = rememberCoroutineScope()

    val verboseLogging by preferences.verboseLogging.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
      modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .topAndBottomNoise(noiseAlpha = 0.15f, fadeFraction = 0.09f),
      topBar = {
        CollapsingTopBar(
          title = stringResource(R.string.pref_advanced),
          scrollBehavior = scrollBehavior,
          onBackClick = { backStack.removeLastOrNull() }
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

        CustomSettingsGroup(stringResource(id = R.string.pref_advanced_group_config)) {
          var mpvConf by remember { mutableStateOf(preferences.mpvConf.get()) }

          CustomMultiLineInputDialogItem(
            title = stringResource(R.string.pref_advanced_mpv_conf),
            summary = if (mpvConf.isNotBlank()) mpvConf.lines().firstOrNull() ?: "" else "Empty",
            icon = Icons.Rounded.Terminal,
            value = mpvConf,
            onValueChange = {
              mpvConf = it
              preferences.mpvConf.set(it)
              File(context.filesDir.path, "mpv.conf").writeText(it)
            }
          )

          var inputConf by remember { mutableStateOf(preferences.inputConf.get()) }

          CustomMultiLineInputDialogItem(
            title = stringResource(R.string.pref_advanced_input_conf),
            summary = if (inputConf.isNotBlank()) inputConf.lines().firstOrNull() ?: "" else "Empty",
            icon = Icons.Rounded.Code,
            value = inputConf,
            onValueChange = {
              inputConf = it
              preferences.inputConf.set(it)
              File(context.filesDir.path, "input.conf").writeText(it)
            }
          )
        }

        CustomSettingsGroup(stringResource(id = R.string.pref_advanced_group_debugging)) {
          val activity = LocalActivity.current!!

          CustomActionItem(
            title = stringResource(R.string.pref_advanced_dump_logs_title),
            summary = stringResource(R.string.pref_advanced_dump_logs_summary),
            icon = Icons.Rounded.BugReport,
            onClick = {
              scope.launch(Dispatchers.IO) {
                val deviceInfo = CrashActivity.collectDeviceInfo()
                val logcat = CrashActivity.collectLogcat()
                CrashActivity.shareLogs(deviceInfo, null, logcat, activity)
              }
            }
          )

          CustomSwitchItem(
            title = stringResource(R.string.pref_advanced_verbose_logging_title),
            summary = stringResource(R.string.pref_advanced_verbose_logging_summary),
            icon = Icons.Rounded.Terminal,
            checked = verboseLogging,
            onCheckedChange = preferences.verboseLogging::set
          )
        }

        CustomSettingsGroup(stringResource(id = R.string.pref_advanced_group_data)) {
          var isConfirmDialogShown by remember { mutableStateOf(false) }
          val NoxtanPlayerDatabase = koinInject<PlaybackDatabase>()
          val videoRepository = koinInject<com.noxtan.player.data.repository.VideoRepository>()

          CustomActionItem(
            title = stringResource(R.string.pref_advanced_clear_playback_history),
            summary = stringResource(id = R.string.pref_advanced_clear_playback_history_summary),
            icon = Icons.Rounded.History,
            onClick = { isConfirmDialogShown = true },
            isDestructive = true
          )

          if (isConfirmDialogShown) {
            ConfirmDialog(
              title = stringResource(R.string.pref_advanced_clear_playback_history_confirm_title),
              subtitle = stringResource(R.string.pref_advanced_clear_playback_history_confirm_subtitle),
              onConfirm = {
                scope.launch(Dispatchers.IO) {
                  NoxtanPlayerDatabase.videoDataDao().clearAllPlaybackStates()
                  videoRepository.clearAllPlaybackHistory()
                }
                isConfirmDialogShown = false
                Toast.makeText(context, context.getString(R.string.pref_advanced_cleared_playback_history), Toast.LENGTH_SHORT).show()
              },
              onCancel = { isConfirmDialogShown = false }
            )
          }

          CustomActionItem(
            title = stringResource(id = R.string.pref_advanced_clear_mpv_conf_cache),
            summary = stringResource(id = R.string.pref_advanced_clear_mpv_conf_cache_summary),
            icon = Icons.Rounded.DeleteForever,
            onClick = {
              fileManager.deleteContent(fileManager.fromPath(context.filesDir.path))
              Toast.makeText(context, context.getString(R.string.pref_advanced_cleared_mpv_conf_cache), Toast.LENGTH_SHORT).show()
            }
          )

          CustomActionItem(
            title = stringResource(id = R.string.pref_advanced_clear_fonts_cache),
            summary = stringResource(id = R.string.pref_advanced_clear_fonts_cache_summary),
            icon = Icons.Rounded.FontDownload,
            onClick = {
              fileManager.deleteContent(fileManager.fromPath(context.cacheDir.path + "/fonts"))
              Toast.makeText(context, context.getString(R.string.pref_advanced_cleared_fonts_cache), Toast.LENGTH_SHORT).show()
            }
          )
        }

        Spacer(modifier = Modifier.height(padding.calculateBottomPadding() + 32.dp))
      }
    }
  }
}

fun getSimplifiedPathFromUri(uri: String): String {
  return Environment.getExternalStorageDirectory().canonicalPath + "/" + Uri.decode(uri).substringAfterLast(":")
}

@Composable
private fun CustomActionItem(
  title: String,
  summary: String,
  icon: ImageVector,
  onClick: () -> Unit,
  isDestructive: Boolean = false
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(role = Role.Button) { onClick() }
      .padding(horizontal = 20.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    val containerColor = if (isDestructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .background(containerColor),
      contentAlignment = Alignment.Center
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
    }

    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
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
  }
}

@Composable
private fun CustomMultiLineInputDialogItem(
  title: String,
  summary: String,
  icon: ImageVector,
  value: String,
  onValueChange: (String) -> Unit
) {
  var showDialog by remember { mutableStateOf(false) }
  var textValue by remember(value) { mutableStateOf(value) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(role = Role.Button) { showDialog = true }
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
      Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
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
          lineHeight = 16.sp,
          maxLines = 1
        )
      }
    }
  }

  if (showDialog) {
    AlertDialog(
      onDismissRequest = { showDialog = false },
      properties = DialogProperties(usePlatformDefaultWidth = false),
      modifier = Modifier.fillMaxWidth(0.9f),
      title = { Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
      text = {
        OutlinedTextField(
          value = textValue,
          onValueChange = { textValue = it },
          modifier = Modifier.fillMaxWidth().height(300.dp),
          shape = RoundedCornerShape(12.dp)
        )
      },
      confirmButton = { TextButton(onClick = { onValueChange(textValue); showDialog = false }) { Text("Save") } },
      dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
    )
  }
}
