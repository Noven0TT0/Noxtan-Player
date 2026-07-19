package com.noxtan.player.di

import androidx.room.Room
import com.noxtan.player.data.local.db.NoxtanDatabase
import com.noxtan.player.data.repository.VideoRepository
import com.noxtan.player.database.Migrations
import com.noxtan.player.database.PlaybackDatabase
import com.noxtan.player.database.repository.PlaybackStateRepositoryImpl
import com.noxtan.player.domain.playbackstate.repository.PlaybackStateRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val DatabaseModule = module {
  single<PlaybackDatabase> {
    Room
      .databaseBuilder(androidContext(), PlaybackDatabase::class.java, "playback.db")
      .addMigrations(migrations = Migrations)
      .fallbackToDestructiveMigration()
      .build()
  }

  single<NoxtanDatabase> {
    NoxtanDatabase.getDatabase(androidContext())
  }

  single<VideoRepository> {
    VideoRepository(androidContext(), get())
  }

  singleOf(::PlaybackStateRepositoryImpl).bind(PlaybackStateRepository::class)
}
