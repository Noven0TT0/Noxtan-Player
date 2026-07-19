package com.noxtan.player.ui.player

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Rational
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.github.k1rakishou.fsaf.FileManager
import com.noxtan.player.R
import com.noxtan.player.data.repository.VideoRepository
import com.noxtan.player.databinding.PlayerLayoutBinding
import com.noxtan.player.domain.playbackstate.repository.PlaybackStateRepository
import com.noxtan.player.preferences.AdvancedPreferences
import com.noxtan.player.preferences.AudioPreferences
import com.noxtan.player.preferences.PlayerPreferences
import com.noxtan.player.preferences.SubtitlesPreferences
import com.noxtan.player.ui.player.controls.PlayerControls
import com.noxtan.player.ui.player.managers.IntentParserHelper
import com.noxtan.player.ui.player.managers.PlaybackSyncManager
import com.noxtan.player.ui.player.managers.PlayerAssetManager
import com.noxtan.player.ui.player.managers.PlaylistManager
import com.noxtan.player.ui.theme.NoxtanTheme
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File

@Suppress("TooManyFunctions", "LargeClass")
class PlayerActivity : AppCompatActivity() {

  private val viewModel: PlayerViewModel by viewModels<PlayerViewModel> { PlayerViewModelProviderFactory(this) }
  private val binding by lazy { PlayerLayoutBinding.inflate(layoutInflater) }
  private val playerObserver by lazy { PlayerObserver(this) }
  private val playbackStateRepository: PlaybackStateRepository by inject()
  private val videoRepository: VideoRepository by inject()
  val player by lazy { binding.player }
  val windowInsetsController by lazy { WindowCompat.getInsetsController(window, window.decorView) }
  val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }

  private val playerPreferences: PlayerPreferences by inject()
  private val audioPreferences: AudioPreferences by inject()
  private val subtitlesPreferences: SubtitlesPreferences by inject()
  private val advancedPreferences: AdvancedPreferences by inject()
  private val fileManager: FileManager by inject()

  private lateinit var intentParserHelper: IntentParserHelper
  private lateinit var playbackSyncManager: PlaybackSyncManager

  private val playerAssetManager by lazy {
    PlayerAssetManager(this, fileManager, advancedPreferences, subtitlesPreferences)
  }
  private val playlistManager by lazy {
    PlaylistManager(videoRepository)
  }

  private var fileName = ""
  private var activeVideoTitle = ""
  private var activeVideoPath = ""
  private var lastLoadedTimestamp = 0L
  private var mediaPlaybackService: MediaPlaybackService? = null
  private var serviceBound = false
  private var isUserExiting = false

  private val pipReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent == null || intent.action != ACTION_MEDIA_CONTROL) return
      val controlType = intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)
      when (controlType) {
        CONTROL_TYPE_PLAY -> viewModel.unpause()
        CONTROL_TYPE_PAUSE -> viewModel.pause()
      }
    }
  }

  private fun updatePictureInPictureActions(isPaused: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      try {
        val icon: Icon
        val title: String
        val controlType: Int

        if (isPaused) {
          icon = Icon.createWithResource(this, R.drawable.baseline_play_arrow_24)
          title = "Play"
          controlType = CONTROL_TYPE_PLAY
        } else {
          icon = Icon.createWithResource(this, R.drawable.baseline_pause_24)
          title = "Pause"
          controlType = CONTROL_TYPE_PAUSE
        }

        val intent = Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, controlType)
        val pendingIntent = PendingIntent.getBroadcast(
          this, controlType, intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val action = RemoteAction(icon, title, title, pendingIntent)
        val params = PictureInPictureParams.Builder()
          .setActions(listOf(action))
          .build()

        setPictureInPictureParams(params)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fast_fade_in, R.anim.fast_fade_out)
    } else {
      @Suppress("DEPRECATION")
      overridePendingTransition(R.anim.fast_fade_in, R.anim.fast_fade_out)
    }

    activeInstances++

    try {
      if (!isMpvDestroyed) {
        MPVLib.destroy()
        isMpvDestroyed = true
      }
    } catch (e: Exception) { }

    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val filter = IntentFilter(ACTION_MEDIA_CONTROL)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(pipReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
      } else {
        registerReceiver(pipReceiver, filter)
      }
    }
    setContentView(binding.root)
    window.decorView.setBackgroundColor(android.graphics.Color.BLACK)

    intentParserHelper = IntentParserHelper(this)
    playbackSyncManager = PlaybackSyncManager(
      playbackStateRepository = playbackStateRepository,
      videoRepository = videoRepository,
      playerPreferences = playerPreferences,
      subtitlesPreferences = subtitlesPreferences,
      audioPreferences = audioPreferences
    )

    setupMPV()
    isMpvDestroyed = false

    viewModel.prepareForNewVideo()

    val serviceIntent = Intent(this, MediaPlaybackService::class.java).apply {
      val currentPath = intentParserHelper.parsePathFromIntent(this@PlayerActivity.intent)
      putExtra("uri", currentPath)
    }
    startService(serviceIntent)
    bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

    onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        isUserExiting = true
        isEnabled = false
        onBackPressedDispatcher.onBackPressed()
        isEnabled = true
      }
    })

    val playableUri = intentParserHelper.getPlayableUri(intent)
    if (playableUri != null) {
      if (playableUri.startsWith("/") || playableUri.startsWith("file://")) {
        lifecycleScope.launch {
          playlistManager.setupPlaylist(playableUri) { fallbackPath ->
            player.playFile(fallbackPath)
          }
        }
      } else {
        player.playFile(playableUri)
      }
    }

    binding.controls.setContent {
      NoxtanTheme {
        PlayerControls(viewModel = viewModel, onBackPress = {
          isUserExiting = true
          finish()
        })
      }
    }
  }

  override fun finish() {
    binding.player.visibility = View.GONE
    setReturnIntent()
    super.finish()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.fast_fade_in, R.anim.fast_fade_out)
    } else {
      @Suppress("DEPRECATION")
      overridePendingTransition(R.anim.fast_fade_in, R.anim.fast_fade_out)
    }
  }

  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    val autoPip = playerPreferences.autoPipMode.get()
    if (autoPip && viewModel.paused == false) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val aspect = player.getVideoOutAspect() ?: (16.0 / 9.0)
        val rational = if (aspect > 0.0) {
          Rational((aspect * 10000).toInt(), 10000)
        } else {
          Rational(16, 9)
        }
        val paramsBuilder = PictureInPictureParams.Builder()
          .setAspectRatio(rational)
        enterPictureInPictureMode(paramsBuilder.build())
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        @Suppress("DEPRECATION")
        enterPictureInPictureMode()
      }
    }
  }

  override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    if (isInPictureInPictureMode) {
      viewModel.hideControls()
      viewModel.hideSeekBar()
      viewModel.sheetShown.update { Sheets.None }
      viewModel.panelShown.update { Panels.None }
    }
  }

  override fun onPause() {
    val bgPref = playerPreferences.automaticBackgroundPlayback.get()
    val isPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) isInPictureInPictureMode else false

    viewModel.hideControls()
    viewModel.hideSeekBar()
    viewModel.isBrightnessSliderShown.update { false }
    viewModel.isVolumeSliderShown.update { false }
    viewModel.sheetShown.update { Sheets.None }

    if (!bgPref && !isPip) {
      viewModel.pause()
    }
    saveVideoPlaybackState(activeVideoTitle.ifBlank { fileName })
    super.onPause()
  }

  override fun onStop() {
    val bgPref = playerPreferences.automaticBackgroundPlayback.get()
    saveVideoPlaybackState(activeVideoTitle.ifBlank { fileName })

    if (!bgPref) {
      viewModel.pause()
    }

    window.attributes.screenBrightness.let {
      if (playerPreferences.rememberBrightness.get() && it != -1f) {
        playerPreferences.defaultBrightness.set(it)
      }
    }
    super.onStop()
  }

  override fun onDestroy() {
    val isFinishingState = isFinishing
    val isPlayingInBg = playerPreferences.automaticBackgroundPlayback.get() && viewModel.paused == false
    val shouldKeepPlaying = isPlayingInBg && isUserExiting

    player.isExiting = true

    if (serviceBound) {
      unbindService(serviceConnection)
      serviceBound = false
    }

    activeInstances--

    if (isFinishingState && !shouldKeepPlaying) {
      val stopIntent = Intent(this, MediaPlaybackService::class.java).apply {
        action = MediaPlaybackService.ACTION_STOP
      }
      startService(stopIntent)

      if (activeInstances == 0 && !isMpvDestroyed) {
        MPVLib.command("stop")
        MPVLib.removeObserver(playerObserver)
      } else {
        MPVLib.removeObserver(playerObserver)
      }
    } else {
      MPVLib.removeObserver(playerObserver)
    }

    super.onDestroy()
  }

  override fun onStart() {
    super.onStart()

    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    if (playerPreferences.showSystemStatusBar.get()) {
      windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
    } else {
      windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
    }

    if (viewModel.controlsShown.value) {
      windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
    } else {
      windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      window.attributes.layoutInDisplayCutoutMode = if (playerPreferences.drawOverDisplayCutout.get()) {
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      } else {
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
      }
    }

    if (playerPreferences.rememberBrightness.get()) {
      playerPreferences.defaultBrightness.get().let {
        if (it != -1f) viewModel.changeBrightnessTo(it)
      }
    }
  }

  private fun setupMPV() {
    playerAssetManager.copyMPVAssets(lifecycleScope)
    player.initialize(filesDir.path, cacheDir.path)
    MPVLib.addObserver(playerObserver)
    audioPreferences.audioChannels.get().let {
      MPVLib.setPropertyString(it.property, it.value)
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.currentVolume.update {
      val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
      val maxVol = viewModel.maxVolume
      currentVol.also {
        if (it < maxVol) viewModel.changeMPVVolumeTo(100)
      }
    }
  }

  private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      val binder = service as MediaPlaybackService.MediaPlaybackBinder
      mediaPlaybackService = binder.getService()
      serviceBound = true
      fileName.let { title ->
        val artist = MPVLib.getPropertyString("metadata/artist") ?: ""
        mediaPlaybackService?.setMediaInfo(title = title, artist = artist, thumbnail = MPVLib.grabThumbnail(1080))
      }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      mediaPlaybackService = null
      serviceBound = false
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    binding.root.post {
      val currentAspect = playerPreferences.videoAspect.get()
      viewModel.changeVideoAspect(currentAspect, showText = false)
    }
  }

  internal fun onObserverEvent(property: String, value: Long) { if (player.isExiting) return }
  internal fun onObserverEvent(property: String) { if (player.isExiting) return }

  internal fun onObserverEvent(property: String, value: Boolean) {
    if (player.isExiting) return
    when (property) {
      "pause" -> {
        if (value) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
      "eof-reached" -> {
        if (value) {
          if (playerPreferences.closeAfterReachingEndOfVideo.get()) {
            finish()
          } else {
            val currentPos = MPVLib.getPropertyInt("playlist-pos") ?: 0
            val totalCount = MPVLib.getPropertyInt("playlist-count") ?: 1

            if (currentPos < totalCount - 1) {
              viewModel.handleNext()
            } else {
              finish()
            }
          }
        }
      }
    }
  }

  internal fun onObserverEvent(property: String, value: String) {
    if (player.isExiting) return
    when (property.substringBeforeLast("/")) {
      "user-data/noxtan" -> viewModel.handleLuaInvocation(property, value)
    }
  }

  internal fun onObserverEvent(property: String, value: MPVNode) { if (player.isExiting) return }

  internal fun onObserverEvent(property: String, value: Double) {
    if (player.isExiting) return
    when (property) {
      "video-params/aspect" -> {
        setOrientation()
        viewModel.markAspectReceived()
      }
    }
  }

  internal fun event(eventId: Int) {
    if (player.isExiting) return
    when (eventId) {
      MPVLib.mpvEventId.MPV_EVENT_END_FILE -> {
        if (activeVideoTitle.isNotBlank()) saveVideoPlaybackState(activeVideoTitle, useLastKnown = true)
      }
      MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {
        val mpvTitle = MPVLib.getPropertyString("media-title")
        val currentPath = MPVLib.getPropertyString("path")
        activeVideoPath = currentPath ?: ""
        val resolvedFileName = if (!currentPath.isNullOrBlank()) File(currentPath).name else intentParserHelper.getFileName(intent)

        val finalTitle = if (mpvTitle.isNullOrBlank() || mpvTitle.isDigitsOnly()) {
          MPVLib.setPropertyString("force-media-title", resolvedFileName)
          resolvedFileName
        } else {
          mpvTitle
        }
        fileName = resolvedFileName
        activeVideoTitle = finalTitle
        lastLoadedTimestamp = System.currentTimeMillis()
        viewModel.setMediaTitle(finalTitle)

        intentParserHelper.setIntentExtras(intent.extras)

        setOrientation()
        viewModel.changeVideoAspect(playerPreferences.videoAspect.get(), showText = false)

        lifecycleScope.launch {
          playbackSyncManager.loadPlaybackState(finalTitle) { state ->

            viewModel.setTargetPosition(state?.lastPosition ?: 0)

            state?.let {
              if (it.customSubUri.isNotBlank()) {
                viewModel.setPendingRestoreSubUri(it.customSubUri)
              } else if (it.sid >= 0) {
                player.sid = it.sid
              }

              player.secondarySid = it.secondarySid
              player.aid = it.aid
            }

            viewModel.fetchLocalSubtitles(activeVideoPath)

            viewModel.markFileLoaded()
          }
        }
      }
      MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART -> player.isExiting = false
    }
  }

  private fun saveVideoPlaybackState(mediaTitle: String, useLastKnown: Boolean = false) {
    if (mediaTitle.isBlank()) return
    val currentPos = if (useLastKnown) viewModel.pos ?: 0 else MPVLib.getPropertyDouble("time-pos")?.toInt() ?: viewModel.pos ?: 0
    val duration = if (useLastKnown) viewModel.duration ?: 0 else MPVLib.getPropertyDouble("duration")?.toInt() ?: viewModel.duration ?: 0

    val currentMpvSid = MPVLib.getPropertyInt("sid") ?: 0
    val activeSid = if (viewModel.selectedCustomSubId.value != null) viewModel.selectedCustomSubId.value!! else currentMpvSid

    val customSubUri = if (viewModel.selectedCustomSubId.value != null) {
      viewModel.subtitleTracks.value.find { it.id == activeSid }?.externalFilename ?: ""
    } else ""

    val playbackSpeed = MPVLib.getPropertyDouble("speed") ?: 1.0
    val subDelay = (MPVLib.getPropertyDouble("sub-delay") ?: 0.0 * 1000).toInt()
    val subSpeed = MPVLib.getPropertyDouble("sub-speed") ?: 1.0
    val secondarySubDelay = (MPVLib.getPropertyDouble("secondary-sub-delay") ?: 0.0 * 1000).toInt()
    val audioDelay = (MPVLib.getPropertyDouble("audio-delay") ?: 0.0 * 1000).toInt()

    lifecycleScope.launch {
      playbackSyncManager.savePlaybackState(
        mediaTitle = mediaTitle,
        videoPath = activeVideoPath,
        currentPos = currentPos,
        duration = duration,
        lastLoadedTimestamp = lastLoadedTimestamp,
        playbackSpeed = playbackSpeed,
        sid = activeSid,
        subDelay = subDelay,
        subSpeed = subSpeed,
        secondarySid = player.secondarySid,
        secondarySubDelay = secondarySubDelay,
        aid = player.aid,
        audioDelay = audioDelay,
        customSubUri = customSubUri
      )
    }
  }

  private fun setReturnIntent() {
    setResult(
      RESULT_OK,
      Intent(RESULT_INTENT).apply {
        viewModel.pos?.let { putExtra("position", it * 1000) }
        viewModel.duration?.let { putExtra("duration", it * 1000) }
      },
    )
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    val uriToPlay = intentParserHelper.getPlayableUri(intent)
    if (uriToPlay != null) {

      viewModel.prepareForNewVideo()

      if (uriToPlay.startsWith("/") || uriToPlay.startsWith("file://")) {
        lifecycleScope.launch {
          playlistManager.setupPlaylist(uriToPlay) { fallbackPath ->
            player.playFile(fallbackPath)
          }
        }
      } else {
        MPVLib.command("loadfile", uriToPlay)
      }
    }
    setIntent(intent)
  }

  @SuppressLint("UnspecifiedRegisterReceiverFlag")
  private fun setOrientation() {
    val pref = playerPreferences.orientation.get()
    val aspect = player.getVideoOutAspect() ?: 0.0

    if (pref == PlayerOrientation.Video && aspect <= 0.0) {
      return
    }

    requestedOrientation = when (pref) {
      PlayerOrientation.Free -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
      PlayerOrientation.Video -> {
        if (aspect > 1.0) {
          ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
          ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
      }
      PlayerOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      PlayerOrientation.ReversePortrait -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
      PlayerOrientation.SensorPortrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
      PlayerOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
      PlayerOrientation.ReverseLandscape -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
      PlayerOrientation.SensorLandscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    when (keyCode) {
      KeyEvent.KEYCODE_VOLUME_UP -> {
        viewModel.changeVolumeBy(1)
        viewModel.displayVolumeSlider()
      }
      KeyEvent.KEYCODE_VOLUME_DOWN -> {
        viewModel.changeVolumeBy(-1)
        viewModel.displayVolumeSlider()
      }
      KeyEvent.KEYCODE_DPAD_RIGHT -> viewModel.handleLeftDoubleTap()
      KeyEvent.KEYCODE_DPAD_LEFT -> viewModel.handleRightDoubleTap()
      KeyEvent.KEYCODE_SPACE -> viewModel.pauseUnpause()
      KeyEvent.KEYCODE_MEDIA_STOP -> {
        finish()
      }
      KeyEvent.KEYCODE_MEDIA_REWIND -> viewModel.handleLeftDoubleTap()
      KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> viewModel.handleRightDoubleTap()
      else -> {
        event?.let { player.onKey(it) }
        super.onKeyDown(keyCode, event)
      }
    }
    return true
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
    if (player.onKey(event!!)) return true
    return super.onKeyUp(keyCode, event)
  }

  companion object {
    const val RESULT_INTENT = "com.noxtan.player.RESULT"

    var activeInstances = 0
    val isActivityActive: Boolean
      get() = activeInstances > 0

    var isMpvDestroyed = true

    private const val ACTION_MEDIA_CONTROL = "media_control"
    private const val EXTRA_CONTROL_TYPE = "control_type"
    private const val CONTROL_TYPE_PLAY = 1
    private const val CONTROL_TYPE_PAUSE = 2
  }
}
