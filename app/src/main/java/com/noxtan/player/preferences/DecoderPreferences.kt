package com.noxtan.player.preferences

import com.noxtan.player.preferences.preference.PreferenceStore
import com.noxtan.player.preferences.preference.getEnum
import com.noxtan.player.ui.player.Debanding

class DecoderPreferences(preferenceStore: PreferenceStore) {
  val tryHWDecoding = preferenceStore.getBoolean("try_hw_dec", true)
  val gpuNext = preferenceStore.getBoolean("gpu_next")
  val useYUV420P = preferenceStore.getBoolean("use_yuv420p", true)

  val debanding = preferenceStore.getEnum("debanding", Debanding.None)
  val debandIterations = preferenceStore.getInt("deband_iterations", 1)
  val debandThreshold = preferenceStore.getInt("deband_threshold", 48)
  val debandRange = preferenceStore.getInt("deband_range", 16)
  val debandGrain = preferenceStore.getInt("deband_grain", 32)

  val brightnessFilter = preferenceStore.getInt("filter_brightness")
  val saturationFilter = preferenceStore.getInt("filter_saturation")
  val gammaFilter = preferenceStore.getInt("filter_gamma")
  val contrastFilter = preferenceStore.getInt("filter_contrast")
  val hueFilter = preferenceStore.getInt("filter_hue")
  val eyeCareBlueLight = preferenceStore.getBoolean("eye_care_blue_light", false)
  val eyeCareAntiGlare = preferenceStore.getBoolean("eye_care_anti_glare", false)
  val eyeCareAntiStrobe = preferenceStore.getBoolean("eye_care_anti_strobe", false)
  val eyeCareAutoDimming = preferenceStore.getBoolean("eye_care_auto_dimming", false)
  val eyeCareNightAudio = preferenceStore.getBoolean("eye_care_night_audio", false)
  val cinematicMode = preferenceStore.getBoolean("cinematic_mode", false)
}
