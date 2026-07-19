package com.noxtan.player

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.noxtan.player.di.AppModule
import com.noxtan.player.di.DatabaseModule
import com.noxtan.player.di.FileManagerModule
import com.noxtan.player.di.PreferencesModule
import com.noxtan.player.di.ViewModelModule
import com.noxtan.player.presentation.crash.CrashActivity
import com.noxtan.player.presentation.crash.GlobalExceptionHandler
import com.noxtan.player.utils.FFmpegCoilDecoder
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

@OptIn(KoinExperimentalAPI::class)
class App : Application(), KoinStartup, SingletonImageLoader.Factory {
  override fun onCreate() {
    super.onCreate()
    Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(applicationContext, CrashActivity::class.java))
  }

  override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
      .components {
        add(FFmpegCoilDecoder.Factory())
      }
      .crossfade(true)
      .build()
  }

  override fun onKoinStartup() = koinConfiguration {
    androidContext(this@App)
    modules(
      AppModule,
      PreferencesModule,
      DatabaseModule,
      FileManagerModule,
      ViewModelModule,
    )
  }
}
