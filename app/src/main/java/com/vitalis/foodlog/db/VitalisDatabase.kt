package com.vitalis.foodlog.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FoodLogEntity::class], version = 1, exportSchema = false)
abstract class VitalisDatabase : RoomDatabase() {

  abstract fun foodLogDao(): FoodLogDao

  companion object {
    @Volatile private var instance: VitalisDatabase? = null

    fun get(context: Context): VitalisDatabase =
        instance
            ?: synchronized(this) {
              instance
                  ?: Room.databaseBuilder(
                          context.applicationContext,
                          VitalisDatabase::class.java,
                          "vitalis.db",
                      )
                      .fallbackToDestructiveMigration()
                      .build()
                      .also { instance = it }
            }
  }
}
