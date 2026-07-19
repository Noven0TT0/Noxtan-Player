package com.noxtan.player.ui.player

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.noxtan.player.R
import com.noxtan.player.preferences.GesturePreferences
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import org.koin.android.ext.android.inject

class MediaPlaybackService : MediaBrowserServiceCompat(), MPVLib.EventObserver {
  companion object {
    private const val NOTIFICATION_ID = 69420
    private const val NOTIFICATION_CHANNEL_ID = "noxtan_playback_channel"
    private const val TAG = "MediaPlaybackService"

    const val ACTION_PLAY = "com.noxtan.player.action.PLAY"
    const val ACTION_PAUSE = "com.noxtan.player.action.PAUSE"
    const val ACTION_STOP = "com.noxtan.player.action.STOP"
    const val ACTION_SKIP_FORWARD = "com.noxtan.player.action.SKIP_FORWARD"
    const val ACTION_SKIP_BACKWARD = "com.noxtan.player.action.SKIP_BACKWARD"
  }

  private val gesturePreferences by inject<GesturePreferences>()

  private val binder = MediaPlaybackBinder()
  private var mediaTitle = ""
  private var mediaArtist = ""
  private var mediaUri: String? = null

  private var positionMs: Long?
    get() = MPVLib.getPropertyDouble("time-pos")?.times(1000L)?.toLong()
    set(value) = MPVLib.command("seek", (value!! / 1000f).toString(), "absolute")

  private val durationMs: Long?
    get() = (MPVLib.getPropertyDouble("duration")?.times(1000L))?.toLong()

  private var paused: Boolean?
    get() = MPVLib.getPropertyBoolean("pause")
    set(value) = MPVLib.command("set", "pause", if (value == true) "yes" else "no")

  private lateinit var mediaSession: MediaSessionCompat
  private lateinit var audioManager: AudioManager
  private var audioFocusRequest: AudioFocusRequest? = null
  private var audioFocusCallback: AudioManager.OnAudioFocusChangeListener? = null
  private var mediaThumbnail: Bitmap? = null
  private var wasPlayingBeforeFocusLoss = false

  private val noisyReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
        pauseMedia()
      }
    }
  }

  inner class MediaPlaybackBinder : Binder() {
    fun getService(): MediaPlaybackService = this@MediaPlaybackService
  }

  override fun onCreate() {
    super.onCreate()
    audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
    setupMediaSession()
    MPVLib.addObserver(this)

    mapOf(
      "pause" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
      "duration" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
      "time-pos" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
      "media-title" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
      "metadata/artist" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    ).onEach { MPVLib.observeProperty(it.key, it.value) }

    setupAudioFocus()
    createNotificationChannel()
    registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
  }

  override fun onBind(intent: Intent): IBinder = binder

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.hasExtra("uri") == true) {
      mediaUri = intent.getStringExtra("uri")
    }
    MediaButtonReceiver.handleIntent(mediaSession, intent)
    handleIntent(intent)
    return START_STICKY
  }

  override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: android.os.Bundle?) =
    BrowserRoot("root_id", null)

  override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
    result.sendResult(mutableListOf())
  }

  private fun handleIntent(intent: Intent?) {
    when (intent?.action) {
      ACTION_PLAY -> playMedia()
      ACTION_PAUSE -> pauseMedia()
      ACTION_STOP -> stopSelf()
      ACTION_SKIP_FORWARD -> seekForward()
      ACTION_SKIP_BACKWARD -> seekBackward()
    }
  }

  fun setMediaInfo(title: String, artist: String, thumbnail: Bitmap? = null) {
    mediaThumbnail = thumbnail
    mediaTitle = title
    mediaArtist = artist
    updateMediaSessionMetadata()
    updateNotification()
  }

  private fun setupMediaSession() {
    val previousAction = gesturePreferences.mediaPreviousGesture.get()
    val playAction = gesturePreferences.mediaPlayGesture.get()
    val nextAction = gesturePreferences.mediaNextGesture.get()

    mediaSession = MediaSessionCompat(this, "MediaPlaybackService").apply {
      isActive = true
      setCallback(object : MediaSessionCompat.Callback() {
        override fun onPlay() {
          when (playAction) {
            SingleActionGesture.PlayPause -> playMedia()
            SingleActionGesture.Custom -> MPVLib.command("keypress", CustomKeyCodes.MediaPlay.keyCode)
            else -> {}
          }
        }
        override fun onPause() {
          when (playAction) {
            SingleActionGesture.PlayPause -> pauseMedia()
            SingleActionGesture.Custom -> MPVLib.command("keypress", CustomKeyCodes.MediaPlay.keyCode)
            else -> {}
          }
        }
        override fun onStop() = stopMedia()
        override fun onSkipToNext() {
          when (nextAction) {
            SingleActionGesture.Seek -> seekForward()
            SingleActionGesture.PlayPause -> if (paused == true) playMedia() else pauseMedia()
            SingleActionGesture.Custom -> MPVLib.command("keypress", CustomKeyCodes.MediaNext.keyCode)
            else -> MPVLib.command("playlist-next")
          }
        }
        override fun onSkipToPrevious() {
          when (previousAction) {
            SingleActionGesture.Seek -> seekBackward()
            SingleActionGesture.PlayPause -> if (paused == true) playMedia() else pauseMedia()
            SingleActionGesture.Custom -> MPVLib.command("keypress", CustomKeyCodes.MediaPrevious.keyCode)
            else -> {
              val currentPos = MPVLib.getPropertyDouble("time-pos") ?: 0.0
              if (currentPos > 5.0) MPVLib.command("seek", "0", "absolute") else MPVLib.command("playlist-prev")
            }
          }
        }
        override fun onSeekTo(pos: Long) { positionMs = pos }
      })
      setSessionToken(sessionToken)
      setPlaybackState(
        PlaybackStateCompat.Builder()
          .setActions(getAvailableActions())
          .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
          .build()
      )
    }
  }

  private fun getAvailableActions(): Long =
    PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
      PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
      PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_STOP or
      PlaybackStateCompat.ACTION_SEEK_TO

  private fun setupAudioFocus() {
    audioFocusCallback = AudioManager.OnAudioFocusChangeListener { focusChange ->
      when (focusChange) {
        AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
          wasPlayingBeforeFocusLoss = paused == false
          pauseMedia()
        }
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
          MPVLib.command("multiply", "volume", "0.5")
        }
        AudioManager.AUDIOFOCUS_GAIN -> {
          MPVLib.command("multiply", "volume", "2.0")
          if (wasPlayingBeforeFocusLoss) playMedia()
        }
      }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
        .build()
      audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(audioAttributes)
        .setAcceptsDelayedFocusGain(true)
        .setOnAudioFocusChangeListener(audioFocusCallback!!)
        .build()
    }
  }

  private fun requestAudioFocus(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      audioFocusRequest?.let { audioManager.requestAudioFocus(it) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED } ?: false
    } else {
      @Suppress("DEPRECATION")
      audioManager.requestAudioFocus(audioFocusCallback, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
  }

  private fun abandonAudioFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    } else {
      @Suppress("DEPRECATION")
      audioManager.abandonAudioFocus(audioFocusCallback)
    }
  }

  fun playMedia() {
    if (requestAudioFocus()) {
      paused = false
      updateNotification()
    }
  }

  fun pauseMedia() {
    paused = true
    updateNotification()
  }

  private fun stopMedia() {
    try {
      pauseMedia()
      abandonAudioFocus()
      mediaSession.isActive = false

      val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.cancel(NOTIFICATION_ID)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        stopForeground(STOP_FOREGROUND_REMOVE)
      } else {
        @Suppress("DEPRECATION")
        stopForeground(true)
      }
      stopSelf()
    } catch (e: Exception) { }
  }

  private fun seekForward() {
    positionMs = positionMs?.plus(gesturePreferences.doubleTapToSeekDuration.get() * 1000L)
  }

  private fun seekBackward() {
    positionMs = positionMs?.minus(gesturePreferences.doubleTapToSeekDuration.get() * 1000L)
  }

  private fun updateMediaSessionMetadata() {
    try {
      val metadataBuilder = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mediaTitle)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mediaArtist)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs ?: 0L)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, mediaTitle)
      mediaThumbnail?.let {
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, it)
      }
      mediaSession.setMetadata(metadataBuilder.build())
    } catch (e: Exception) { }
  }

  private fun updatePlaybackState() {
    try {
      val stateBuilder = PlaybackStateCompat.Builder()
        .setActions(getAvailableActions())
        .setState(
          if (paused == true) PlaybackStateCompat.STATE_PAUSED else PlaybackStateCompat.STATE_PLAYING,
          positionMs ?: 0,
          1.0f,
        )
      mediaSession.setPlaybackState(stateBuilder.build())
    } catch (e: Exception) { }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_LOW,
      ).apply { setShowBadge(false); enableLights(false); enableVibration(false) }
      val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }

  private fun createNotification(): Notification {
    val openAppIntent = Intent(this, PlayerActivity::class.java).apply {

      action = Intent.ACTION_VIEW
      mediaUri?.let { uriStr ->
        data = if (uriStr.startsWith("content://") || uriStr.startsWith("file://") || uriStr.startsWith("http")) {
          uriStr.toUri()
        } else {
          Uri.fromFile(java.io.File(uriStr))
        }
      }
    }
    val pendingOpenAppIntent = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    val pendingPlayIntent = PendingIntent.getService(this, 1, Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_PLAY }, PendingIntent.FLAG_IMMUTABLE)
    val pendingPauseIntent = PendingIntent.getService(this, 2, Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_PAUSE }, PendingIntent.FLAG_IMMUTABLE)
    val pendingSkipForwardIntent = PendingIntent.getService(this, 3, Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_SKIP_FORWARD }, PendingIntent.FLAG_IMMUTABLE)
    val pendingSkipBackwardIntent = PendingIntent.getService(this, 4, Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_SKIP_BACKWARD }, PendingIntent.FLAG_IMMUTABLE)
    val pendingStopIntent = PendingIntent.getService(this, 5, Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_STOP }, PendingIntent.FLAG_IMMUTABLE)

    return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
      .setContentTitle(mediaTitle)
      .setContentText(mediaArtist.ifBlank { getString(R.string.notification_playing) })
      .setSmallIcon(R.mipmap.ic_launcher)
      .setLargeIcon(mediaThumbnail)
      .setContentIntent(pendingOpenAppIntent)
      .setDeleteIntent(pendingStopIntent)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOnlyAlertOnce(true)
      .setOngoing(paused == false)
      .addAction(R.drawable.baseline_fast_rewind_24, getString(R.string.notification_rewind), pendingSkipBackwardIntent)
      .addAction(
        if (paused == false) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24,
        if (paused == false) getString(R.string.notification_pause) else getString(R.string.notification_play),
        if (paused == false) pendingPauseIntent else pendingPlayIntent,
      )
      .addAction(R.drawable.baseline_fast_forward_24, getString(R.string.notification_forward), pendingSkipForwardIntent)
      .addAction(R.drawable.sharp_shadow_24, getString(R.string.notification_stop), pendingStopIntent)
      .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken).setShowActionsInCompactView(0, 1, 2))
      .setColorized(true)
      .setColor(android.graphics.Color.BLUE)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }

  @SuppressLint("MissingPermission")
  private fun updateNotification() {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = createNotification()

    if (paused == false) {
      try {
        startForeground(NOTIFICATION_ID, notification)
      } catch (e: Exception) {
        notificationManager.notify(NOTIFICATION_ID, notification)
      }
    } else {
      @Suppress("DEPRECATION")
      stopForeground(false)
      notificationManager.notify(NOTIFICATION_ID, notification)
    }
  }

  override fun eventProperty(property: String) {}
  override fun eventProperty(property: String, value: Long) {
    when (property) { "duration", "time-pos" -> updatePlaybackState() }
  }
  override fun eventProperty(property: String, value: Boolean) {
    when (property) {
      "pause" -> {
        if (!value) requestAudioFocus()
        updatePlaybackState()
        updateNotification()
      }
    }
  }
  override fun eventProperty(property: String, value: String) {
    when (property) {
      "metadata/artist" -> { mediaArtist = value; updateMediaSessionMetadata(); updateNotification() }
      "media-title" -> { mediaTitle = value; updateMediaSessionMetadata(); updateNotification() }
    }
  }
  override fun eventProperty(property: String, value: Double) {}
  override fun eventProperty(property: String, value: MPVNode) {}
  override fun event(eventId: Int) {}

  override fun onDestroy() {
    try {
      val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.cancel(NOTIFICATION_ID)

      unregisterReceiver(noisyReceiver)
      MPVLib.removeObserver(this)
      mediaSession.release()
      abandonAudioFocus()

      if (!PlayerActivity.isActivityActive && !PlayerActivity.isMpvDestroyed) {
        MPVLib.command("stop")
      }
      super.onDestroy()
    } catch (e: Exception) { }
  }
}
