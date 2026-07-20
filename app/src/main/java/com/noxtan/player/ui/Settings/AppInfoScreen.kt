package com.noxtan.player.ui.Settings

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.util.withContext
import com.noxtan.player.BuildConfig
import com.noxtan.player.R
import com.noxtan.player.presentation.Screen
import com.noxtan.player.presentation.components.CollapsingTopBar
import com.noxtan.player.presentation.crash.CrashActivity.Companion.collectDeviceInfo
import com.noxtan.player.ui.Settings.components.CustomSettingsGroup
import com.noxtan.player.ui.utils.LocalBackStack
import com.noxtan.player.ui.utils.topAndBottomNoise
import compose.icons.SimpleIcons
import compose.icons.simpleicons.Github
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
object AboutScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val backstack = LocalBackStack.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
      modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .topAndBottomNoise(noiseAlpha = 0.15f, fadeFraction = 0.09f),
      topBar = {
        CollapsingTopBar(
          title = stringResource(id = R.string.pref_about_title),
          scrollBehavior = scrollBehavior,
          onBackClick = { backstack.removeLastOrNull() }
        )
      },
      containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(top = paddingValues.calculateTopPadding())
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Surface(
            modifier = Modifier.size(150.dp),
            color = androidx.compose.ui.graphics.Color.Transparent, // ဘောင်အရောင်ကို ဖျောက်လိုက်ပါသည်
            tonalElevation = 0.dp                                  // Shadow ကို ဖျောက်လိုက်ပါသည်
          ) {
            Box(
              modifier = Modifier.fillMaxSize().padding(16.dp),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_noxtan_logo),
                contentDescription = "NoxtanPlayer Logo",
                modifier = Modifier.fillMaxSize(),
                tint = androidx.compose.ui.graphics.Color.Unspecified
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        CustomSettingsGroup(title = stringResource(id = R.string.about_acknowledgement)) {
          AboutLinkItem(
            title = stringResource(id = R.string.about_based_on),
            subtitle = stringResource(id = R.string.about_based_on_desc),
            icon = Icons.Rounded.Code,
            onClick = {
              context.startActivity(
                Intent(
                  Intent.ACTION_VIEW,
                  "https://github.com/abdallahmehiz/mpvKt".toUri()
                )
              )
            }
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        CustomSettingsGroup(title = stringResource(id = R.string.about_info_legal)) {
          AboutLinkItem(
            title = stringResource(id = R.string.about_system_diagnostics),
            subtitle = stringResource(id = R.string.about_system_diagnostics_desc),
            icon = androidx.compose.material.icons.Icons.Rounded.Memory,
            onClick = { backstack.add(DeviceInfoScreen) }
          )

          AboutLinkItem(
            title = stringResource(id = R.string.about_open_source_licenses),
            subtitle = stringResource(id = R.string.about_open_source_licenses_desc),
            icon = Icons.Rounded.LibraryBooks,
            onClick = { backstack.add(LibrariesScreen) }
          )

          AboutLinkItem(
            title = stringResource(id = R.string.pref_about_privacy_policy),
            subtitle = "Read our data policy",
            icon = Icons.Rounded.Security,
            onClick = {
              context.startActivity(
                Intent(
                  Intent.ACTION_VIEW,
                  "https://noven0tt0.github.io/Noxtan-Player/privacy_policy.html".toUri()
                )
              )
            }
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center
        ) {
          IconButton(
            onClick = {
              context.startActivity(
                Intent(Intent.ACTION_VIEW, context.getString(R.string.github_repo_url).toUri())
              )
            },
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          ) {
            Icon(
              imageVector = SimpleIcons.Github,
              contentDescription = "GitHub",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(28.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 80.dp))
      }
    }
  }
}

@Composable
private fun AboutLinkItem(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(role = Role.Button) { onClick() }
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
      if (subtitle.isNotBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 16.sp
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    Icon(
      imageVector = Icons.Rounded.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
      modifier = Modifier.size(20.dp)
    )
  }
}

@Serializable
object LibrariesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var libsData by remember { mutableStateOf<Libs?>(null) }

    LaunchedEffect(Unit) {
      withContext(Dispatchers.IO) {
        try {
          libsData = Libs.Builder().withContext(context).build()
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }

    Scaffold(
      modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .topAndBottomNoise(noiseAlpha = 0.15f, fadeFraction = 0.09f),
      topBar = {
        CollapsingTopBar(
          title = "Open Source Licenses",
          scrollBehavior = scrollBehavior,
          onBackClick = { backstack.removeLastOrNull() }
        )
      },
      containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
      if (libsData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
      } else {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = paddingValues.calculateTopPadding())
        ) {
          Spacer(modifier = Modifier.height(12.dp))

          CustomSettingsGroup(title = "THIRD-PARTY SOFTWARE") {
            libsData!!.libraries.forEach { library ->
              LibraryRowItem(
                name = library.name,
                author = library.author,
                version = library.artifactVersion,
                licenseName = library.licenses.firstOrNull()?.name,
                onClick = {
                  library.website?.let { url ->
                    try { uriHandler.openUri(url) } catch (e: Exception) {}
                  }
                }
              )
            }
          }

          Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 32.dp))
        }
      }
    }
  }
}

@Composable
private fun LibraryRowItem(
  name: String,
  author: String?,
  version: String?,
  licenseName: String?,
  onClick: () -> Unit
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
        imageVector = Icons.Rounded.Code,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp)
      )
    }

    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = name,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )
      if (!author.isNullOrBlank()) {
        Text(
          text = author,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 16.sp
        )
      }

      if (!version.isNullOrBlank() || !licenseName.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (!version.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
              Text(
                text = "v$version",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
          if (!licenseName.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            ) {
              Text(
                text = licenseName,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    Icon(
      imageVector = Icons.Rounded.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
      modifier = Modifier.size(20.dp)
    )
  }
}

@Serializable
object DeviceInfoScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val clipboard = LocalClipboardManager.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val deviceInfo = remember { collectDeviceInfo() }

    Scaffold(
      modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .topAndBottomNoise(noiseAlpha = 0.15f, fadeFraction = 0.09f),
      topBar = {
        CollapsingTopBar(
          title = "System Diagnostics",
          scrollBehavior = scrollBehavior,
          onBackClick = { backstack.removeLastOrNull() }
        )
      },
      floatingActionButton = {
        androidx.compose.material3.ExtendedFloatingActionButton(
          onClick = {
            clipboard.setText(AnnotatedString(deviceInfo))
            Toast.makeText(context, "Diagnostics copied to clipboard", Toast.LENGTH_SHORT).show()
          },
          icon = { Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy") },
          text = { Text("Copy Details", fontWeight = FontWeight.Bold) },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      },
      containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(top = paddingValues.calculateTopPadding(), start = 16.dp, end = 16.dp)
      ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "ENGINE INFORMATION",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          tonalElevation = 0.dp
        ) {
          androidx.compose.foundation.text.selection.SelectionContainer {
            Text(
              text = deviceInfo,
              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
              fontSize = 12.sp,
              lineHeight = 20.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 80.dp))
      }
    }
  }
}
