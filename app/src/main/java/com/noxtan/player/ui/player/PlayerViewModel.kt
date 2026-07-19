package com.noxtan.player.ui.player

import android.net.Uri
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.noxtan.player.R
import com.noxtan.player.preferences.AudioPreferences
import com.noxtan.player.preferences.GesturePreferences
import com.noxtan.player.preferences.PlayerPreferences
import com.noxtan.player.preferences.SubtitlesBorderStyle
import com.noxtan.player.preferences.SubtitlesPreferences
import com.noxtan.player.subtitle.SubtitleSearcher
import com.noxtan.player.ui.player.managers.SleepTimerManager
import com.noxtan.player.ui.player.managers.SubtitleSearchManager
import com.noxtan.player.ui.player.managers.SubtitleSyncManager
import com.noxtan.player.ui.player.managers.SystemMediaController
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class PlayerViewModelProviderFactory(
  private val activity: PlayerActivity,
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
    val systemMediaController = SystemMediaController(activity)
    return PlayerViewModel(systemMediaController) as T
  }
}

@Suppress("TooManyFunctions")
class PlayerViewModel(
  private val systemMediaController: SystemMediaController,
) : ViewModel() {
  private val playerPreferences: PlayerPreferences by inject(PlayerPreferences::class.java)
  private val gesturePreferences: GesturePreferences by inject(GesturePreferences::class.java)
  private val audioPreferences: AudioPreferences by inject(AudioPreferences::class.java)
  private val decoderPreferences: com.noxtan.player.preferences.DecoderPreferences by inject(com.noxtan.player.preferences.DecoderPreferences::class.java)
  private val json: Json by inject(Json::class.java)

  private val sleepTimerManager = SleepTimerManager(
    coroutineScope = viewModelScope,
    onTimerEnd = { MPVLib.setPropertyBoolean("pause", true) },
    showToast = {
      Toast.makeText(
        systemMediaController.context,
        systemMediaController.context.getString(R.string.toast_sleep_timer_ended),
        Toast.LENGTH_SHORT
      ).show()
    }
  )

  private val _mediaTitle = MutableStateFlow("")
  val mediaTitle = _mediaTitle.asStateFlow()

  fun setMediaTitle(title: String) {
    _mediaTitle.value = title
  }
  val isLeftHanded = playerPreferences.isLeftHanded.stateIn(viewModelScope)

  fun toggleHandedness() {
    playerPreferences.isLeftHanded.set(!playerPreferences.isLeftHanded.get())
  }

  val paused by MPVLib.propBoolean["pause"].collectAsState(viewModelScope)
  val pos by MPVLib.propInt["time-pos"].collectAsState(viewModelScope)
  val duration by MPVLib.propInt["duration"].collectAsState(viewModelScope)
  private val currentMPVVolume by MPVLib.propInt["volume"].collectAsState(viewModelScope)

  val currentVolume = MutableStateFlow(systemMediaController.getCurrentVolume())
  val maxVolume = systemMediaController.maxVolume
  private val volumeBoostCap by MPVLib.propInt["volume-max"].collectAsState(viewModelScope)

  private val _customSrtTracks = MutableStateFlow<List<TrackNode>>(emptyList())
  private val _selectedCustomSubId = MutableStateFlow<Int?>(null)
  val selectedCustomSubId = _selectedCustomSubId.asStateFlow()

  private var pendingRestoreSubUri: String? = null

  fun setPendingRestoreSubUri(uri: String) {
    pendingRestoreSubUri = uri
  }
  private var customSubIdCounter = -1
  val subtitleTracks = combine(
    MPVLib.propNode["track-list"],
    _customSrtTracks,
    _selectedCustomSubId
  ) { node, customTracks, selectedCustomId ->
    val mpvTracks = node?.toObject<List<TrackNode>>(json)?.filter { it.isSubtitle }?.map {
      if (selectedCustomId != null) it.copy(selected = false) else it
    } ?: emptyList()

    val mappedCustom = customTracks.map {
      it.copy(selected = it.id == selectedCustomId)
    }

    (mpvTracks + mappedCustom).toImmutableList()
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

  val audioTracks = MPVLib.propNode["track-list"]
    .map { (it?.toObject<List<TrackNode>>(json)?.filter { it.isAudio } ?: persistentListOf()).toImmutableList() }

  val chapters = MPVLib.propNode["chapter-list"]
    .map { (it?.toObject<List<ChapterNode>>(json) ?: persistentListOf()).map { it.toSegment() }.toImmutableList() }

  private val _controlsShown = MutableStateFlow(true)
  val controlsShown = _controlsShown.asStateFlow()
  private val _seekBarShown = MutableStateFlow(true)
  val seekBarShown = _seekBarShown.asStateFlow()
  private val _areControlsLocked = MutableStateFlow(false)
  val areControlsLocked = _areControlsLocked.asStateFlow()

  val playerUpdate = MutableStateFlow<PlayerUpdates>(PlayerUpdates.None)
  val isBrightnessSliderShown = MutableStateFlow(false)
  val isVolumeSliderShown = MutableStateFlow(false)

  val isSpeedSliderShown = MutableStateFlow(false)
  val currentGestureSpeed = MutableStateFlow(1f)

  val currentBrightness = MutableStateFlow(systemMediaController.getCurrentBrightness())

  val sheetShown = MutableStateFlow(Sheets.None)
  val panelShown = MutableStateFlow(Panels.None)

  val gestureSeekAmount = MutableStateFlow<Pair<Int, Int>?>(null)

  private val _seekText = MutableStateFlow<String?>(null)
  val seekText = _seekText.asStateFlow()
  private val _doubleTapSeekAmount = MutableStateFlow(0)
  val doubleTapSeekAmount = _doubleTapSeekAmount.asStateFlow()
  private val _isSeekingForwards = MutableStateFlow(false)
  val isSeekingForwards = _isSeekingForwards.asStateFlow()

  val remainingTime = sleepTimerManager.remainingTime
  private val subtitlesPreferences: SubtitlesPreferences by inject(SubtitlesPreferences::class.java)

  val customCues = MutableStateFlow<List<String>>(emptyList())
  val isCustomSubActive = _selectedCustomSubId.map { it != null }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
  val customSubVerticalPosition = subtitlesPreferences.subPos.changes()
    .map { it / 100f }
    .stateIn(viewModelScope, SharingStarted.Eagerly, 1f)

  val customSubBackgroundEnabled = subtitlesPreferences.borderStyle.changes()
    .map { it != SubtitlesBorderStyle.OutlineAndShadow }
    .stateIn(viewModelScope, SharingStarted.Eagerly, true)
  val customSubFontSize = subtitlesPreferences.fontSize.changes().stateIn(viewModelScope, SharingStarted.Eagerly, 55)
  val customSubScale = subtitlesPreferences.subScale.changes().stateIn(viewModelScope, SharingStarted.Eagerly, 1f)
  val customSubBold = subtitlesPreferences.bold.changes().stateIn(viewModelScope, SharingStarted.Eagerly, false)
  val customSubItalic = subtitlesPreferences.italic.changes().stateIn(viewModelScope, SharingStarted.Eagerly, false)
  val customSubTextColor = subtitlesPreferences.textColor.changes().stateIn(viewModelScope, SharingStarted.Eagerly, android.graphics.Color.WHITE)
  val customSubBorderColor = subtitlesPreferences.borderColor.changes().stateIn(viewModelScope, SharingStarted.Eagerly, android.graphics.Color.BLACK)
  val customSubBorderSize = subtitlesPreferences.borderSize.changes().stateIn(viewModelScope, SharingStarted.Eagerly, 3)
  val customSubShadowOffset = subtitlesPreferences.shadowOffset.changes().stateIn(viewModelScope, SharingStarted.Eagerly, 0)

  private val subtitleSyncManager = SubtitleSyncManager(
    context = systemMediaController.context,
    coroutineScope = viewModelScope,
    getCurrentPositionMs = { ((MPVLib.getPropertyDouble("time-pos") ?: 0.0) * 1000).toLong() }
  )

  private val _isVideoReadyToReveal = MutableStateFlow(false)
  val isVideoReadyToReveal = _isVideoReadyToReveal.asStateFlow()

  private var targetPosition: Int = 0
  private var isFileLoaded = false
  private var isAspectReceived = false

  fun prepareForNewVideo() {
    _isVideoReadyToReveal.value = false
    isFileLoaded = false
    isAspectReceived = false
    targetPosition = 0
    pause()

    _customSrtTracks.value = emptyList()
    _selectedCustomSubId.value = null
    subtitleSyncManager.stopAndClear()
  }

  fun setTargetPosition(pos: Int) {
    targetPosition = pos
    checkIfReady()
  }

  fun markFileLoaded() {
    isFileLoaded = true
    checkIfReady()
    viewModelScope.launch {
      kotlinx.coroutines.delay(1500)
      if (!_isVideoReadyToReveal.value) {
        _isVideoReadyToReveal.value = true
        unpause()
      }
    }
  }

  fun markAspectReceived() {
    isAspectReceived = true
    checkIfReady()
  }

  private fun checkIfReady() {
    if (_isVideoReadyToReveal.value) return
    if (!isFileLoaded || !isAspectReceived) return

    val currentPos = pos ?: 0
    val isPositionReady = if (targetPosition <= 3) {
      true
    } else {
      currentPos >= (targetPosition - 5)
    }

    if (isPositionReady) {
      _isVideoReadyToReveal.value = true
      unpause()
    }
  }

  init {
    viewModelScope.launch {
      subtitleSyncManager.cues.collect { customCues.value = it }
    }
    viewModelScope.launch {
      MPVLib.propDouble["sub-delay"].collect { delaySec ->
        subtitleSyncManager.subtitleDelayMs = ((delaySec ?: 0.0) * 1000).toLong()
      }
    }
    viewModelScope.launch {
      MPVLib.propInt["time-pos"].collect {
        checkIfReady()
      }
    }
  }

  fun startTimer(seconds: Int) {
    sleepTimerManager.startTimer(seconds)
  }

  fun handlePrevious() {
    val currentPos = pos ?: 0
    if (currentPos > 5) {
      seekTo(0)
    } else {
      val playlistPos = MPVLib.getPropertyInt("playlist-pos") ?: 0
      if (playlistPos > 0) {
        prepareForNewVideo()
        MPVLib.command("playlist-prev")
        showControls()
      } else {
        seekTo(0)
        Toast.makeText(systemMediaController.context, systemMediaController.context.getString(R.string.toast_first_video), Toast.LENGTH_SHORT).show()
      }
    }
  }

  fun handleNext() {
    val playlistPos = MPVLib.getPropertyInt("playlist-pos") ?: 0
    val playlistCount = MPVLib.getPropertyInt("playlist-count") ?: 1

    if (playlistPos < playlistCount - 1) {
      prepareForNewVideo()
      MPVLib.command("playlist-next")
      showControls()
    } else {
      Toast.makeText(systemMediaController.context, systemMediaController.context.getString(R.string.toast_last_video), Toast.LENGTH_SHORT).show()
    }
  }

  fun cycleDecoders() {
    MPVLib.setPropertyString(
      "hwdec",
      when (Decoder.getDecoderFromValue(MPVLib.getPropertyString("hwdec-current") ?: return)) {
        Decoder.HWPlus -> Decoder.HW.value
        Decoder.HW -> Decoder.SW.value
        Decoder.SW -> Decoder.HWPlus.value
        Decoder.AutoCopy -> Decoder.SW.value
        Decoder.Auto -> Decoder.SW.value
      },
    )
  }

  fun addAudio(uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      val url = uri.toString()
      val path = if (url.startsWith("content://")) url.toUri().openContentFd(systemMediaController.context) else url
      if (path != null) {
        MPVLib.command("audio-add", path, "cached")
      }
    }
  }

  fun addSubtitle(uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val url = uri.toString()
        val path = if (url.startsWith("content://")) url.toUri().openContentFd(systemMediaController.context) else url
        path ?: return@launch

        if (path.endsWith(".srt", ignoreCase = true) || url.endsWith(".srt", ignoreCase = true)) {

          val existing = _customSrtTracks.value.find { it.externalFilename == uri.toString() }
          if (existing != null) {
            selectSub(existing.id)
            return@launch
          }

          val fileName = try {
            androidx.documentfile.provider.DocumentFile.fromSingleUri(systemMediaController.context, uri)?.name ?: "Custom SRT"
          } catch (e: Exception) { "Custom SRT" }

          val newId = customSubIdCounter--
          val newTrack = TrackNode(id = newId, type = "sub", title = fileName, externalFilename = uri.toString())

          _customSrtTracks.update { it + newTrack }
          selectSub(newId)
        } else {
          _selectedCustomSubId.value = null
          subtitleSyncManager.stopAndClear()
          MPVLib.command("sub-add", path, "cached")
        }
      } catch (e: Exception) { }
    }
  }

  fun fetchLocalSubtitles(videoPath: String) {
    val prefLangs = subtitlesPreferences.preferredLanguages.get().lowercase()

    viewModelScope.launch {
      val result = SubtitleSearchManager(systemMediaController.context).searchSubtitles(videoPath, prefLangs)

      val newTracks = result.allFoundSubs.map { found ->
        val newId = customSubIdCounter--
        TrackNode(id = newId, type = "sub", title = found.label, externalFilename = found.uriString)
      }

      _customSrtTracks.update { current ->
        (current + newTracks).distinctBy { it.externalFilename }
      }

      if (pendingRestoreSubUri != null) {
        val trackToRestore = _customSrtTracks.value.find { it.externalFilename == pendingRestoreSubUri }
        if (trackToRestore != null) {
          selectSub(trackToRestore.id)
        } else {
          addSubtitle(Uri.parse(pendingRestoreSubUri!!))
        }
        pendingRestoreSubUri = null
      }
      else if (_selectedCustomSubId.value == null) {
        val currentMpvSid = MPVLib.getPropertyInt("sid") ?: -1
        if (currentMpvSid == -1) {
          result.prioritySub?.let { priority ->
            val trackToSelect = _customSrtTracks.value.find { it.externalFilename == priority.uriString }
            trackToSelect?.let {
              selectSub(it.id)
            }
          }
        }
      }
    }
  }

  fun skipToNextSubtitle() {
    val subtitles = subtitleSyncManager.getSubtitles()
    val currentPosMs = ((MPVLib.getPropertyDouble("time-pos") ?: 0.0) * 1000).toLong()
    val nextTimeMs = SubtitleSearcher.findNextTimestamp(currentPosMs, subtitles)

    if (nextTimeMs != null) {
      val exactSeconds = nextTimeMs / 1000.0
      MPVLib.command("seek", exactSeconds.toString(), "absolute")
    } else {
      seekBy(5, precise = true)
    }
  }

  fun skipToPreviousSubtitle() {
    val subtitles = subtitleSyncManager.getSubtitles()
    val currentPosMs = ((MPVLib.getPropertyDouble("time-pos") ?: 0.0) * 1000).toLong()
    val prevTimeMs = SubtitleSearcher.findPreviousTimestamp(currentPosMs, subtitles)

    if (prevTimeMs != null) {
      val exactSeconds = prevTimeMs / 1000.0
      MPVLib.command("seek", exactSeconds.toString(), "absolute")
    } else {
      seekBy(-5, precise = true)
    }
  }

  fun selectSub(id: Int) {
    if (id < 0) {
      if (_selectedCustomSubId.value == id) {
        _selectedCustomSubId.value = null
        subtitleSyncManager.stopAndClear()
      } else {
        _selectedCustomSubId.value = id
        MPVLib.setPropertyBoolean("sid", false)

        val track = _customSrtTracks.value.find { it.id == id }
        track?.externalFilename?.let { uriString ->
          subtitleSyncManager.loadManualSubtitle(Uri.parse(uriString))
        }
      }
      return
    }
    _selectedCustomSubId.value = null
    subtitleSyncManager.stopAndClear()

    val selectedSubs = Pair(MPVLib.getPropertyInt("sid"), MPVLib.getPropertyInt("secondary-sid"))
    when (id) {
      selectedSubs.first -> Pair(selectedSubs.second, null)
      selectedSubs.second -> Pair(selectedSubs.first, null)
      else -> if (selectedSubs.first != null) Pair(selectedSubs.first, id) else Pair(id, null)
    }.let {
      it.second?.let { MPVLib.setPropertyInt("secondary-sid", it) } ?: MPVLib.setPropertyBoolean("secondary-sid", false)
      it.first?.let { MPVLib.setPropertyInt("sid", it) } ?: MPVLib.setPropertyBoolean("sid", false)
    }
  }

  fun pauseUnpause() = MPVLib.command("cycle", "pause")
  fun pause() = MPVLib.setPropertyBoolean("pause", true)
  fun unpause() = MPVLib.setPropertyBoolean("pause", false)

  fun showControls() {
    if (sheetShown.value != Sheets.None || panelShown.value != Panels.None) return
    systemMediaController.showNavigationBars()
    _controlsShown.update { true }
    isBrightnessSliderShown.update { false }
    isVolumeSliderShown.update { false }
    isSpeedSliderShown.update { false }
  }

  fun hideControls() {
    systemMediaController.hideNavigationBars()
    _controlsShown.update { false }
  }

  fun hideSeekBar() {
    _seekBarShown.update { false }
  }

  fun showSeekBar() {
    if (sheetShown.value != Sheets.None) return
    _seekBarShown.update { true }
  }

  fun lockControls() {
    _areControlsLocked.update { true }
  }

  fun unlockControls() {
    _areControlsLocked.update { false }
  }

  fun seekBy(offset: Int, precise: Boolean = false) {
    MPVLib.command("seek", offset.toString(), if (precise) "relative+exact" else "relative")
  }

  fun seekTo(position: Int, precise: Boolean = true) {
    if (position !in 0..(MPVLib.getPropertyInt("duration") ?: 0)) return
    MPVLib.command("seek", position.toString(), if (precise) "absolute" else "absolute+keyframes")
  }

  fun changeBrightnessBy(change: Float) {
    changeBrightnessTo(currentBrightness.value + change)
  }

  fun changeBrightnessTo(brightness: Float) {
    systemMediaController.setBrightness(brightness)
    currentBrightness.update { brightness.coerceIn(0f, 1f) }
  }

  fun displayBrightnessSlider() {
    isVolumeSliderShown.update { false }
    isBrightnessSliderShown.update { true }
  }

  fun displayVolumeSlider() {
    isBrightnessSliderShown.update { false }
    isVolumeSliderShown.update { true }
  }

  fun changeVolumeBy(change: Int) {
    val mpvVolume = MPVLib.getPropertyInt("volume")
    if ((volumeBoostCap ?: audioPreferences.volumeBoostCap.get()) > 0 && currentVolume.value == maxVolume) {
      if (mpvVolume == 100 && change < 0) changeVolumeTo(currentVolume.value + change)
      val finalMPVVolume = (mpvVolume?.plus(change))?.coerceAtLeast(100) ?: 100
      if (finalMPVVolume in 100..(volumeBoostCap ?: audioPreferences.volumeBoostCap.get()) + 100) {
        changeMPVVolumeTo(finalMPVVolume)
        return
      }
    }
    changeVolumeTo(currentVolume.value + change)
  }

  fun changeVolumeTo(volume: Int) {
    val newVolume = volume.coerceIn(0..maxVolume)
    systemMediaController.setVolume(newVolume)
    currentVolume.update { newVolume }

    if (newVolume < maxVolume) {
      val mpvVol = MPVLib.getPropertyInt("volume") ?: 100
      if (mpvVol > 100) {
        changeMPVVolumeTo(100)
      }
    }
  }

  fun changeMPVVolumeTo(volume: Int) {
    MPVLib.setPropertyInt("volume", volume)
  }

  fun setMPVVolume(volume: Int) {
    if (volume != currentMPVVolume) displayVolumeSlider()
  }

  fun changeVideoAspect(aspect: VideoAspect, showText: Boolean = true) {
    var ratio = -1.0
    var pan = 1.0
    when (aspect) {
      VideoAspect.Crop -> { pan = 1.0 }
      VideoAspect.Fit -> {
        pan = 0.0
        MPVLib.setPropertyDouble("panscan", 0.0)
      }
      VideoAspect.Stretch -> {
        val dm = DisplayMetrics()
        systemMediaController.windowManager.defaultDisplay.getRealMetrics(dm)
        ratio = dm.widthPixels / dm.heightPixels.toDouble()
        pan = 0.0
      }
    }

    MPVLib.setPropertyDouble("panscan", pan)
    MPVLib.setPropertyDouble("video-aspect-override", ratio)
    playerPreferences.videoAspect.set(aspect)
    if (showText) {
      playerUpdate.update { PlayerUpdates.AspectRatio }
    }

    if (paused == true) {
      MPVLib.command("seek", "0", "relative")
    }
  }

  val isBlueLightFilterEnabled = decoderPreferences.eyeCareBlueLight.stateIn(viewModelScope)

  fun toggleBlueLightFilter(enabled: Boolean) {
    decoderPreferences.eyeCareBlueLight.set(enabled)
    if (enabled) {
      MPVLib.command("vf", "add", "@bluelight:colorchannelmixer=rr=1.0:gg=0.85:bb=0.65")
    } else {
      MPVLib.command("vf", "remove", "@bluelight")
    }
  }

  val isAntiGlareEnabled = decoderPreferences.eyeCareAntiGlare.stateIn(viewModelScope)

  fun toggleAntiGlareFilter(enabled: Boolean) {
    decoderPreferences.eyeCareAntiGlare.set(enabled)
    if (enabled) {
      MPVLib.command("vf", "add", "@antiglare:eq=contrast=0.8:brightness=-0.1:gamma=1.2")
    } else {
      MPVLib.command("vf", "remove", "@antiglare")
    }
  }

  val isAntiStrobeEnabled = decoderPreferences.eyeCareAntiStrobe.stateIn(viewModelScope)

  fun toggleAntiStrobeFilter(enabled: Boolean) {
    decoderPreferences.eyeCareAntiStrobe.set(enabled)
    if (enabled) {
      MPVLib.command("vf", "add", "@antistrobe:tblend=all_mode=average")
    } else {
      MPVLib.command("vf", "remove", "@antistrobe")
    }
  }

  val isAutoDimmingEnabled = decoderPreferences.eyeCareAutoDimming.stateIn(viewModelScope)

  fun toggleAutoDimmingFilter(enabled: Boolean) {
    decoderPreferences.eyeCareAutoDimming.set(enabled)
    if (enabled) {
      MPVLib.command("vf", "add", "@autodim:lavfi=[colorlevels=romax=0.8:gomax=0.8:bomax=0.8]")
    } else {
      MPVLib.command("vf", "remove", "@autodim")
    }
  }

  val isNightAudioEnabled = decoderPreferences.eyeCareNightAudio.stateIn(viewModelScope)

  fun toggleNightAudioFilter(enabled: Boolean) {
    decoderPreferences.eyeCareNightAudio.set(enabled)
    if (enabled) {
      MPVLib.command("af", "add", "@nightaudio:dynaudnorm=f=250:g=15:p=0.5")
    } else {
      MPVLib.command("af", "remove", "@nightaudio")
    }
  }

  val isCinematicModeEnabled = decoderPreferences.cinematicMode.stateIn(viewModelScope)

  fun toggleCinematicMode(enabled: Boolean) {
    decoderPreferences.cinematicMode.set(enabled)
    if (enabled) {
      MPVLib.command("vf", "add", "@cinematic_color:eq=contrast=1.03")
      MPVLib.command("vf", "add", "@cinematic_sharp:unsharp=5:5:1.0")
    } else {
      MPVLib.command("vf", "remove", "@cinematic_color")
      MPVLib.command("vf", "remove", "@cinematic_sharp")
    }
  }

  fun cycleScreenRotations() {
    systemMediaController.cycleScreenRotations {}
  }

  @Suppress("CyclomaticComplexMethod", "LongMethod")
  fun handleLuaInvocation(property: String, value: String) {
    val data = value
      .removePrefix("\"")
      .removeSuffix("\"")
      .ifEmpty { return }

    when (property.substringAfterLast("/")) {
      "show_text" -> playerUpdate.update { PlayerUpdates.ShowText(data) }
      "toggle_ui" -> {
        when (data) {
          "show" -> showControls()
          "toggle" -> {
            if (controlsShown.value) hideControls() else showControls()
          }

          "hide" -> {
            sheetShown.update { Sheets.None }
            panelShown.update { Panels.None }
            hideControls()
          }
        }
      }

      "show_panel" -> {
        when (data) {
          "subtitle_settings" -> panelShown.update { Panels.SubtitleAdvanced }
          "subtitle_delay" -> panelShown.update { Panels.SubtitleTracks }
          "audio_delay" -> panelShown.update { Panels.AudioTracks }
          "video_filters" -> panelShown.update { Panels.VideoFilters }
        }
      }

      "seek_to_with_text" -> {
        val (seekValue, text) = data.split("|", limit = 2)
        seekToWithText(seekValue.toInt(), text)
      }

      "seek_by_with_text" -> {
        val (seekValue, text) = data.split("|", limit = 2)
        seekByWithText(seekValue.toInt(), text)
      }

      "seek_by" -> seekByWithText(data.toInt(), null)
      "seek_to" -> seekToWithText(data.toInt(), null)

      "software_keyboard" -> when (data) {
        "show" -> systemMediaController.forceShowSoftwareKeyboard()
        "hide" -> systemMediaController.forceHideSoftwareKeyboard()
        "toggle" if !systemMediaController.isKeyboardActive -> systemMediaController.forceShowSoftwareKeyboard()
        else -> systemMediaController.forceHideSoftwareKeyboard()
      }
    }

    MPVLib.setPropertyString(property, "")
  }

  private fun seekToWithText(seekValue: Int, text: String?) {
    _isSeekingForwards.value = seekValue > 0
    _doubleTapSeekAmount.value = seekValue - (pos ?: return)
    _seekText.update { text }
    seekTo(seekValue, playerPreferences.preciseSeeking.get())
    if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
  }

  private fun seekByWithText(value: Int, text: String?) {
    _doubleTapSeekAmount.update {
      if (value < 0 && it < 0 || (pos ?: return) + value > (duration ?: return)) 0 else it + value
    }
    _seekText.update { text }
    _isSeekingForwards.value = value > 0
    seekBy(value, playerPreferences.preciseSeeking.get())
    if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
  }

  private val doubleTapToSeekDuration = gesturePreferences.doubleTapToSeekDuration.get()

  fun updateSeekAmount(amount: Int) {
    _doubleTapSeekAmount.update { amount }
  }

  fun updateSeekText(text: String?) {
    _seekText.update { text }
  }

  fun leftSeek() {
    if ((pos ?: return) > 0) _doubleTapSeekAmount.value -= doubleTapToSeekDuration
    _isSeekingForwards.value = false
    seekBy(-doubleTapToSeekDuration, playerPreferences.preciseSeeking.get())
    if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
  }

  fun rightSeek() {
    if ((pos ?: return) < (duration ?: return)) {
      _doubleTapSeekAmount.value += doubleTapToSeekDuration
    }
    _isSeekingForwards.value = true
    seekBy(doubleTapToSeekDuration, playerPreferences.preciseSeeking.get())
    if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
  }

  fun handleLeftDoubleTap() {
    when (gesturePreferences.leftSingleActionGesture.get()) {
      SingleActionGesture.Seek -> leftSeek()
      SingleActionGesture.PlayPause -> pauseUnpause()
      SingleActionGesture.Custom -> MPVLib.command("keypress", CustomKeyCodes.DoubleTapLeft.keyCode)
      SingleActionGesture.None -> {}
    }
  }

  fun handleCenterDoubleTap() {
    when (gesturePreferences.centerSingleActionGesture.get()) {
      SingleActionGesture.PlayPause -> pauseUnpause()
      SingleActionGesture.Custom -> MPVLib.command("keypress", CustomKeyCodes.DoubleTapCenter.keyCode)
      SingleActionGesture.Seek -> {}
      SingleActionGesture.None -> {}
    }
  }

  fun handleRightDoubleTap() {
    when (gesturePreferences.rightSingleActionGesture.get()) {
      SingleActionGesture.Seek -> rightSeek()
      SingleActionGesture.PlayPause -> pauseUnpause()
      SingleActionGesture.Custom -> MPVLib.command("keypress", CustomKeyCodes.DoubleTapRight.keyCode)
      SingleActionGesture.None -> {}
    }
  }
}

fun Float.normalize(inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
  return (this - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
}

fun <T> Flow<T>.collectAsState(scope: CoroutineScope, initialValue: T? = null) =
  object : ReadOnlyProperty<Any?, T?> {
    private var value: T? = initialValue
    init { scope.launch { collect { value = it } } }
    override fun getValue(thisRef: Any?, property: KProperty<*>) = value
  }
