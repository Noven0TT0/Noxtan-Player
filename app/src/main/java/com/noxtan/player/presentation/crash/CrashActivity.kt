package com.noxtan.player.presentation.crash

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.coroutineScope
import com.noxtan.player.BuildConfig
import com.noxtan.player.MainActivity
import com.noxtan.player.R
import com.noxtan.player.preferences.AppearancePreferences
import com.noxtan.player.preferences.preference.collectAsState
import com.noxtan.player.ui.theme.DarkMode
import com.noxtan.player.ui.theme.NoxtanTheme
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class CrashActivity : ComponentActivity() {

  private val clipboardManager by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
  private lateinit var logcat: String
  private val appearancePreferences: AppearancePreferences by inject()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    lifecycle.coroutineScope.launch {
      logcat = collectLogcat()
    }
    setContent {
      val dark by appearancePreferences.darkMode.collectAsState()
      val isSystemInDarkTheme = isSystemInDarkTheme()
      enableEdgeToEdge(
        SystemBarStyle.auto(
          lightScrim = Color.White.toArgb(),
          darkScrim = Color.White.toArgb(),
        ) { dark == DarkMode.Dark || (dark == DarkMode.System && isSystemInDarkTheme) },
      )
      NoxtanTheme {
        CrashScreen(intent.getStringExtra("exception") ?: "")
      }
    }
  }

  companion object {
    suspend fun shareLogs(
      deviceInfo: String,
      exceptionString: String? = null,
      logcat: String,
      activity: Activity,
    ) {
      withContext(NonCancellable) {
        val file = File(activity.cacheDir, "noxtan_error_logs.txt")
        if (file.exists()) file.delete()
        file.createNewFile()
        file.appendText(concatLogs(deviceInfo, exceptionString, logcat))
        val uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.clipData = ClipData.newRawUri(null, uri)
        intent.type = "text/plain"
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        activity.startActivity(
          Intent.createChooser(intent, activity.getString(R.string.crash_screen_share)),
        )
      }
    }

    fun concatLogs(
      deviceInfo: String,
      crashLogs: String? = null,
      logcat: String,
    ): String {
      return StringBuilder().apply {
        appendLine(deviceInfo)
        appendLine()
        if (!crashLogs.isNullOrBlank()) {
          appendLine("Exception:")
          appendLine(crashLogs)
          appendLine()
        }
        appendLine("Logcat:")
        appendLine(logcat)
      }.toString()
    }

    fun collectLogcat(): String {
      val logcat = StringBuilder()
      try {
        val pid = android.os.Process.myPid()
        val command = "logcat -d --pid=$pid"

        val process = Runtime.getRuntime().exec(command)
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          reader.lines().forEach(logcat::appendLine)
        } else {
          reader.readLines().forEach(logcat::appendLine)
        }
      } catch (e: Exception) {
        logcat.appendLine("Failed to collect logcat: ${e.message}")
      }
      return logcat.toString()
    }

    fun collectDeviceInfo(): String {
      return """
      --- Noxtan System Diagnostics ---
      Build Version : ${BuildConfig.VERSION_NAME} (${BuildConfig.GIT_SHA})
      OS Engine     : Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
      Hardware      : ${Build.MANUFACTURER} ${Build.BRAND} ${Build.MODEL} (${Build.DEVICE})
      Core Player   : MPV ${Utils.VERSIONS.mpv}
      Media Codec   : FFmpeg ${Utils.VERSIONS.ffmpeg}
      Renderer      : libplacebo ${Utils.VERSIONS.libPlacebo}
      ---------------------------------
      """.trimIndent()
    }
  }

  @Composable
  fun CrashScreen(
    exceptionString: String,
    modifier: Modifier = Modifier,
  ) {
    val scope = rememberCoroutineScope()

    Column(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .systemBarsPadding()
        .padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // --- Error Icon ---
      Box(
        modifier = Modifier
          .padding(top = 24.dp)
          .size(80.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.BugReport,
          contentDescription = "Crash Icon",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(40.dp)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // --- Friendly Title & Subtitle ---
      Text(
        text = "Oops! Something went wrong.",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "NoxtanPlayer encountered an unexpected error. Please share the error report to help us fix this issue.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )

      Spacer(modifier = Modifier.height(24.dp))

      // --- Exception Details Box ---
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          .padding(16.dp)
      ) {
        SelectionContainer {
          Text(
            text = exceptionString.ifBlank { "Unknown Runtime Exception Occurred" },
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.verticalScroll(rememberScrollState())
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // --- Action Buttons ---
      Button(
        onClick = {
          scope.launch(Dispatchers.IO) {
            shareLogs(collectDeviceInfo(), exceptionString, logcat, this@CrashActivity)
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text("Share Error Report", fontWeight = FontWeight.Bold, fontSize = 16.sp)
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedButton(
        onClick = {
          finish()
          startActivity(Intent(this@CrashActivity, MainActivity::class.java))
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text("Restart App", fontWeight = FontWeight.Bold, fontSize = 16.sp)
      }
    }
  }
}
