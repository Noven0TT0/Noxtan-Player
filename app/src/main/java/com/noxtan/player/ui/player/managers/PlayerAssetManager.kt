package com.noxtan.player.ui.player.managers

import android.content.Context
import androidx.core.net.toUri
import com.github.k1rakishou.fsaf.FileManager
import com.noxtan.player.preferences.AdvancedPreferences
import com.noxtan.player.preferences.SubtitlesPreferences
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PlayerAssetManager(
  private val context: Context,
  private val fileManager: FileManager,
  private val advancedPreferences: AdvancedPreferences,
  private val subtitlesPreferences: SubtitlesPreferences
) {

  fun copyMPVAssets(scope: CoroutineScope) {
    Utils.copyAssets(context)
    copyMPVScripts()
    copyMPVConfigFiles()
    scope.launch(Dispatchers.IO) {
      copyMPVFonts()
    }
  }

  private fun copyMPVConfigFiles() {
    val applicationPath = context.filesDir.path
    try {
      val mpvConf = fileManager.fromUri(advancedPreferences.mpvConfStorageUri.get().toUri())
        ?: error("User hasn't set any mpvConfig directory")
      if (!fileManager.exists(mpvConf)) error("Couldn't access mpv configuration directory")
      fileManager.copyDirectoryWithContent(mpvConf, fileManager.fromPath(applicationPath), true)
    } catch (e: Exception) {
      File("$applicationPath/mpv.conf").also { if (!it.exists()) it.createNewFile() }.writeText(advancedPreferences.mpvConf.get())
      File("$applicationPath/input.conf").also { if (!it.exists()) it.createNewFile() }.writeText(advancedPreferences.inputConf.get())
    }
  }

  private fun copyMPVScripts() {
    val NoxtanPlayerLua = context.assets.open("noxtan.lua")
    val applicationPath = context.filesDir.path
    val scriptsDir = fileManager.createDir(fileManager.fromPath(applicationPath), "scripts")!!
    fileManager.deleteContent(scriptsDir)
    File("$scriptsDir/noxtan.lua").also { if (!it.exists()) it.createNewFile() }.writeText(NoxtanPlayerLua.bufferedReader().readText())
  }

  private suspend fun copyMPVFonts() {
    try {
      val cachePath = context.cacheDir.path
      val destDir = fileManager.fromPath("$cachePath/fonts")
      if (!fileManager.exists(destDir)) {
        fileManager.createDir(fileManager.fromPath(cachePath), "fonts")
      }
      val subfontFile = File("$cachePath/fonts/subfont.ttf")
      if (subfontFile.exists()) subfontFile.delete()

      context.resources.assets.open("subfont.ttf").use { input ->
        subfontFile.outputStream().use { output -> input.copyTo(output) }
      }

      val fontsFolderUri = subtitlesPreferences.fontsFolder.get()
      if (fontsFolderUri.isNotBlank()) {
        val fontsDir = fileManager.fromUri(fontsFolderUri.toUri())
        if (fontsDir != null && fileManager.exists(fontsDir)) {
          fileManager.copyDirectoryWithContent(fontsDir, destDir, false)
        }
      }
    } catch (e: Exception) { }
  }
}
