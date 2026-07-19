package com.noxtan.player.ui.player.managers

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class FoundSubtitle(val uriString: String, val label: String)
data class SubtitleSearchResult(val allFoundSubs: List<FoundSubtitle>, val prioritySub: FoundSubtitle?)

class SubtitleSearchManager(private val context: Context) {

  private fun extractSeriesInfo(fileName: String): List<String> {
    val searchTerms = mutableListOf<String>()
    val namePattern = Regex("^(.*?)\\s*[-_]?\\s*(?:[sS](?:eason)?\\s*\\d+|[eE](?:pisode|p)?\\s*\\d+)", RegexOption.IGNORE_CASE)
    val nameMatch = namePattern.find(fileName)
    val seriesName = nameMatch?.groupValues?.get(1)?.trim() ?: ""
    if (seriesName.isNotEmpty()) searchTerms.add(seriesName)

    val epPattern = Regex("(?:episode|ep|[eE])\\s*?(\\d+)", RegexOption.IGNORE_CASE)
    val epMatch = epPattern.find(fileName)
    val epNumber = epMatch?.groupValues?.get(1)?.trim()?.toIntOrNull()?.toString() ?: ""
    if (epNumber.isNotEmpty()) searchTerms.add(epNumber)

    return searchTerms
  }

  private fun extractVideoId(fileName: String): String? {
    val idPattern = Regex("[A-Za-z]{2,5}[-\\s]?\\d{3,5}")
    return idPattern.find(fileName)?.value
  }

  suspend fun searchSubtitles(videoPath: String, preferredLang: String): SubtitleSearchResult = withContext(Dispatchers.IO) {
    val videoFile = File(videoPath)
    val fileName = videoFile.nameWithoutExtension

    val seriesInfo = extractSeriesInfo(fileName)
    val videoId = extractVideoId(fileName)

    val localSubs = mutableListOf<FoundSubtitle>()

    val normalizedVideoId = videoId?.replace("-", "")?.replace(" ", "")?.lowercase()

    fun isSubtitleMatch(subName: String): Boolean {
      val normalizedSubName = subName.replace("-", "").replace(" ", "").lowercase()

      if (subName.equals(fileName, ignoreCase = true)) return true
      if (normalizedVideoId != null && normalizedSubName.contains(normalizedVideoId)) return true

      if (seriesInfo.isNotEmpty() && subName.contains(seriesInfo[0], ignoreCase = true)) {
        if (seriesInfo.size > 1) {
          val videoEp = seriesInfo[1]

          val subEpPattern = Regex("(?:[sS]\\d+[eE]|[eE]pisode\\s*|[eE]p\\s*|\\b[eE])(\\d+)", RegexOption.IGNORE_CASE)
          val subEpMatch = subEpPattern.find(subName)
          val subEp = subEpMatch?.groupValues?.get(1)?.toIntOrNull()?.toString()

          if (subEp != null) {
            return videoEp == subEp
          } else {
            val fallbackPattern = Regex("(?<!\\d)0*$videoEp(?!\\d)")
            return fallbackPattern.containsMatchIn(subName)
          }
        } else {
          return true
        }
      } else if (fileName.contains(subName, ignoreCase = true)) {
        return true
      }
      return false
    }

    val parentDir = videoFile.parentFile
    if (parentDir != null && parentDir.exists() && parentDir.isDirectory) {
      try {
        val subFiles = parentDir.listFiles { file ->
          file.isFile && (file.name.endsWith(".srt", true) || file.name.endsWith(".vtt", true) || file.name.endsWith(".ass", true))
        }
        if (subFiles != null) {
          for (subFile in subFiles) {
            val subName = subFile.nameWithoutExtension
            val isMatch = isSubtitleMatch(subName)

            if (isMatch) {
              localSubs.add(FoundSubtitle(Uri.fromFile(subFile).toString(), "Found: ${subFile.name}"))
            } else { }
          }
        }
      } catch (e: Exception) {
      }
    }

    val uri = MediaStore.Files.getContentUri("external")
    val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.DATA)

    val searchTerm = when {
      videoId != null -> {
        val parts = Regex("([A-Za-z]+)[-\\s]?(\\d+)").find(videoId)?.destructured
        if (parts != null) {
          "%${parts.component1()}%${parts.component2()}%"
        } else {
          "%$videoId%"
        }
      }
      seriesInfo.isNotEmpty() -> "%${seriesInfo[0]}%"
      else -> "%$fileName%"
    }

    val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? AND ${MediaStore.Files.FileColumns.DATA} LIKE ?"
    val selectionArgs = arrayOf(searchTerm, "%.srt")

    try {
      context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
        while (cursor.moveToNext()) {
          val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
          val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
          val contentUri = ContentUris.withAppendedId(uri, id)

          val subNameWithoutExt = name.substringBeforeLast(".")
          val isMatch = isSubtitleMatch(subNameWithoutExt)

          if (isMatch) {
            if (localSubs.none { it.uriString.contains(name) || it.label.contains(name) }) {
              localSubs.add(FoundSubtitle(contentUri.toString(), "Found: $name"))
            }
          } else { }
        }
      }
    } catch (e: Exception) { }

    var prioritySub: FoundSubtitle? = null
    if (localSubs.isNotEmpty()) {

      val langMatch = if (preferredLang.isNotBlank()) {
        val langs = preferredLang.split(",").map { it.trim() }
        localSubs.find { sub -> langs.any { lang -> sub.label.contains(lang, ignoreCase = true) } }
      } else null

      val myanmarMatch = localSubs.find {
        it.label.contains("my", ignoreCase = true) || it.label.contains("mm", ignoreCase = true) || it.label.contains("burmese", ignoreCase = true)
      }

      val engMatch = localSubs.find {
        it.label.contains("en", ignoreCase = true) || it.label.contains("eng", ignoreCase = true)
      }

      prioritySub = langMatch ?: myanmarMatch ?: engMatch ?: localSubs.first()
    } else { }

    return@withContext SubtitleSearchResult(localSubs.distinctBy { it.uriString }, prioritySub)
  }
}
