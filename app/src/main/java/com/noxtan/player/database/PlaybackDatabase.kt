package com.noxtan.player.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.noxtan.player.database.dao.PlaybackStateDao
import com.noxtan.player.database.entities.PlaybackStateEntity

@Database(entities = [PlaybackStateEntity::class], version = 7, exportSchema = false)
abstract class PlaybackDatabase : RoomDatabase() {
  abstract fun videoDataDao(): PlaybackStateDao
}
