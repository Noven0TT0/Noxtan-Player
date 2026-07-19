package com.noxtan.player.ui.player

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.AttributeSet
import android.view.KeyCharacterMap
import android.view.KeyEvent
import com.noxtan.player.preferences.AdvancedPreferences
import com.noxtan.player.preferences.AudioPreferences
import com.noxtan.player.preferences.DecoderPreferences
import com.noxtan.player.preferences.PlayerPreferences
import com.noxtan.player.preferences.SubtitlesPreferences
import `is`.xyz.mpv.BaseMPVView
import `is`.xyz.mpv.KeyMapping
import `is`.xyz.mpv.MPVLib
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.reflect.KProperty

class MPVView(context: Context, attributes: AttributeSet) : BaseMPVView(context, attributes), KoinComponent {

  private val audioPreferences: AudioPreferences by inject()
  private val playerPreferences: PlayerPreferences by inject()
  private val decoderPreferences: DecoderPreferences by inject()
  private val advancedPreferences: AdvancedPreferences by inject()
  private val subtitlesPreferences: SubtitlesPreferences by inject()

  var isExiting = false

  fun getVideoOutAspect(): Double? {
    val aspect = MPVLib.getPropertyDouble("video-params/aspect")
    val rotate = MPVLib.getPropertyInt("video-params/rotate") ?: 0

    return aspect?.let {
      if (it < 0.001) return 0.0
      if (rotate % 180 == 90) 1.0 / it else it
    }
  }

  class TrackDelegate(private val name: String) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
      val v = MPVLib.getPropertyString(name)
      return when (v) {
        "no" -> 0
        "auto" -> -1
        null -> -1
        else -> v.toIntOrNull() ?: -1
      }
    }
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
      when (value) {
        -1 -> MPVLib.setPropertyString(name, "auto")
        0 -> MPVLib.setPropertyString(name, "no")
        else -> MPVLib.setPropertyInt(name, value)
      }
    }
  }

  var sid: Int by TrackDelegate("sid")
  var secondarySid: Int by TrackDelegate("secondary-sid")
  var aid: Int by TrackDelegate("aid")

  override fun initOptions() {
    setVo(if (decoderPreferences.gpuNext.get()) "gpu-next" else "gpu")
    MPVLib.setOptionString("profile", "fast")
    MPVLib.setOptionString("hwdec", if (decoderPreferences.tryHWDecoding.get()) "auto" else "no")

    if (decoderPreferences.useYUV420P.get()) {
      MPVLib.setOptionString("vf", "format=yuv420p")
    }
    MPVLib.setOptionString("msg-level", "all=" + if (advancedPreferences.verboseLogging.get()) "v" else "warn")

    MPVLib.setOptionString("keep-open", "always")

    MPVLib.setPropertyBoolean("input-default-bindings", true)

    MPVLib.setOptionString("tls-verify", "yes")
    MPVLib.setOptionString("tls-ca-file", "${context.filesDir.path}/cacert.pem")

    val cacheMegs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) 64 else 32
    MPVLib.setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
    MPVLib.setOptionString("demuxer-max-back-bytes", "${cacheMegs * 1024 * 1024}")

    val screenshotDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    screenshotDir.mkdirs()
    MPVLib.setOptionString("screenshot-directory", screenshotDir.path)

    VideoFilters.entries.forEach {
      MPVLib.setOptionString(it.mpvProperty, it.preference(decoderPreferences).get().toString())
    }

    MPVLib.setOptionString("speed", playerPreferences.defaultSpeed.get().toString())
    MPVLib.setOptionString("vd-lavc-film-grain", "cpu")

    setupSubtitlesOptions()
    setupAudioOptions()
  }

  override fun observeProperties() {
    for ((name, format) in observedProps) MPVLib.observeProperty(name, format)
  }

  override fun postInitOptions() {
    when (decoderPreferences.debanding.get()) {
      Debanding.None -> {}
      Debanding.CPU -> MPVLib.command("vf", "add", "@deband:gradfun=radius=12")
      Debanding.GPU -> MPVLib.setOptionString("deband", "yes")
    }

    if (decoderPreferences.eyeCareBlueLight.get()) {
      MPVLib.command("vf", "add", "@bluelight:colorchannelmixer=rr=1.0:gg=0.85:bb=0.65")
    }

    if (decoderPreferences.eyeCareAntiGlare.get()) {
      MPVLib.command("vf", "add", "@antiglare:eq=contrast=0.8:brightness=-0.1:gamma=1.2")
    }

    /* if (decoderPreferences.eyeCareHdrToSdr.get()) {
      MPVLib.setOptionString("tone-mapping", "reinhard")
      MPVLib.setOptionString("target-peak", "100")
    } */

    if (decoderPreferences.eyeCareAntiStrobe.get()) {
      MPVLib.command("vf", "add", "@antistrobe:tblend=all_mode=average")
    }

    if (decoderPreferences.eyeCareAutoDimming.get()) {
      MPVLib.command("vf", "add", "@autodim:lavfi=[colorlevels=romax=0.8:gomax=0.8:bomax=0.8]")
    }

    if (decoderPreferences.eyeCareNightAudio.get()) {
      MPVLib.command("af", "add", "@nightaudio:dynaudnorm=f=250:g=15:p=0.5")
    }

    if (decoderPreferences.cinematicMode.get()) {
      MPVLib.command("vf", "add", "@cinematic_color:eq=contrast=1.03")
      MPVLib.command("vf", "add", "@cinematic_sharp:unsharp=5:5:1.0")
    }

    advancedPreferences.enabledStatisticsPage.get().let {
      if (it != 0) {
        MPVLib.command("script-binding", "stats/display-stats-toggle")
        MPVLib.command("script-binding", "stats/display-page-$it")
      }
    }
  }

  @Suppress("ReturnCount")
  fun onKey(event: KeyEvent): Boolean {
    if (event.action == KeyEvent.ACTION_MULTIPLE || KeyEvent.isModifierKey(event.keyCode)) {
      return false
    }

    var mapped = KeyMapping[event.keyCode]
    if (mapped == null) {
      if (!event.isPrintingKey) {
        return false
      }

      val ch = event.unicodeChar
      if (ch.and(KeyCharacterMap.COMBINING_ACCENT) != 0) {
        return false
      }
      mapped = ch.toChar().toString()
    }

    if (event.repeatCount > 0) {
      return true
    }

    val mod: MutableList<String> = mutableListOf()
    event.isShiftPressed && mod.add("shift")
    event.isCtrlPressed && mod.add("ctrl")
    event.isAltPressed && mod.add("alt")
    event.isMetaPressed && mod.add("meta")

    val action = if (event.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
    mod.add(mapped)
    MPVLib.command(action, mod.joinToString("+"))

    return true
  }

  private val observedProps = mapOf(
    "pause" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
    "video-params/aspect" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
    "eof-reached" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
    "media-title" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "time-pos" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
    "duration" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
    "user-data/noxtan/show_text" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "user-data/noxtan/toggle_ui" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "user-data/noxtan/show_panel" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "user-data/noxtan/set_button_title" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "user-data/noxtan/reset_button_title" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "user-data/noxtan/toggle_button" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "user-data/noxtan/seek_by" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "user-data/noxtan/seek_to" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "user-data/noxtan/seek_by_with_text" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "user-data/noxtan/seek_to_with_text" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
    "user-data/noxtan/software_keyboard" to MPVLib.mpvFormat.MPV_FORMAT_STRING,
  )

  private fun setupAudioOptions() {
    val alang = audioPreferences.preferredLanguages.get()
    val delay = (audioPreferences.defaultAudioDelay.get() / 1000.0).toString()
    val pitch = audioPreferences.audioPitchCorrection.get().toString()
    val volMax = (audioPreferences.volumeBoostCap.get() + 100).toString()

    MPVLib.setOptionString("alang", alang)
    MPVLib.setOptionString("audio-delay", delay)
    MPVLib.setOptionString("audio-pitch-correction", pitch)
    MPVLib.setOptionString("volume-max", volMax)
  }

  private fun setupSubtitlesOptions() {
    MPVLib.setOptionString("slang", subtitlesPreferences.preferredLanguages.get())
    MPVLib.setOptionString("sub-fonts-dir", context.cacheDir.path + "/fonts/")
    MPVLib.setOptionString("sub-delay", (subtitlesPreferences.defaultSubDelay.get() / 1000.0).toString())
    MPVLib.setOptionString("sub-speed", subtitlesPreferences.defaultSubSpeed.get().toString())
    MPVLib.setOptionString("secondary-sub-delay", (subtitlesPreferences.defaultSecondarySubDelay.get() / 1000.0).toString())
    MPVLib.setOptionString("sub-font", subtitlesPreferences.font.get())
    if (subtitlesPreferences.overrideAssSubs.get()) {
      MPVLib.setOptionString("sub-ass-override", "force")
      MPVLib.setOptionString("sub-ass-justify", "yes")
    }
    MPVLib.setOptionString("sub-font-size", subtitlesPreferences.fontSize.get().toString())
    MPVLib.setOptionString("sub-bold", if (subtitlesPreferences.bold.get()) "yes" else "no")
    MPVLib.setOptionString("sub-italic", if (subtitlesPreferences.italic.get()) "yes" else "no")
    MPVLib.setOptionString("sub-justify", subtitlesPreferences.justification.get().value)
    MPVLib.setOptionString("sub-color", subtitlesPreferences.textColor.get().toColorHexString())
    MPVLib.setOptionString("sub-back-color", subtitlesPreferences.backgroundColor.get().toColorHexString())
    MPVLib.setOptionString("sub-border-color", subtitlesPreferences.borderColor.get().toColorHexString())
    MPVLib.setOptionString("sub-border-size", subtitlesPreferences.borderSize.get().toString())
    MPVLib.setOptionString("sub-border-style", subtitlesPreferences.borderStyle.get().value)
    MPVLib.setOptionString("sub-shadow-offset", subtitlesPreferences.shadowOffset.get().toString())
    MPVLib.setOptionString("sub-pos", subtitlesPreferences.subPos.get().toString())
    MPVLib.setOptionString("sub-scale", subtitlesPreferences.subScale.get().toString())
  }
}
