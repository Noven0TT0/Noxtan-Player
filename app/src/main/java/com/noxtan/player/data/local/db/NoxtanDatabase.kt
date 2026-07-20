package com.noxtan.player.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [VideoEntity::class], version = 2, exportSchema = false)
abstract class NoxtanDatabase : RoomDatabase() {
  abstract fun videoDao(): VideoDao

  companion object {
    @Volatile
    private var INSTANCE: NoxtanDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE videos ADD COLUMN markState TEXT NOT NULL DEFAULT 'NONE'")
      }
    }

    fun getDatabase(context: Context): NoxtanDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          NoxtanDatabase::class.java,
          "noxtan_player_db"
        )
          .addMigrations(MIGRATION_1_2)
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
