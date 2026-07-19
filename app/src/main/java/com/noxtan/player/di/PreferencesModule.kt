package com.noxtan.player.di

import com.noxtan.player.preferences.AdvancedPreferences
import com.noxtan.player.preferences.AppearancePreferences
import com.noxtan.player.preferences.AudioPreferences
import com.noxtan.player.preferences.DecoderPreferences
import com.noxtan.player.preferences.GesturePreferences
import com.noxtan.player.preferences.PlayerPreferences
import com.noxtan.player.preferences.SubtitlesPreferences
import com.noxtan.player.preferences.preference.AndroidPreferenceStore
import com.noxtan.player.preferences.preference.PreferenceStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val PreferencesModule = module {
  single { AndroidPreferenceStore(androidContext()) }.bind(PreferenceStore::class)

  singleOf(::AppearancePreferences)
  singleOf(::PlayerPreferences)
  singleOf(::GesturePreferences)
  singleOf(::DecoderPreferences)
  singleOf(::SubtitlesPreferences)
  singleOf(::AudioPreferences)
  singleOf(::AdvancedPreferences)
}
