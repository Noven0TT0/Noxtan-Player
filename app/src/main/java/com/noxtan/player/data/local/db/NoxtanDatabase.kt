package com.noxtan.player.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [VideoEntity::class], version = 1, exportSchema = false)
abstract class NoxtanDatabase : RoomDatabase() {
  abstract fun videoDao(): VideoDao

  companion object {
    @Volatile
    private var INSTANCE: NoxtanDatabase? = null

    fun getDatabase(context: Context): NoxtanDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          NoxtanDatabase::class.java,
          "noxtan_player_db"
        )

          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
