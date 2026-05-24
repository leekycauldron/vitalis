package com.vitalis.foodlog.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {

  @Insert suspend fun insert(entry: FoodLogEntity): Long

  @Query("SELECT * FROM food_log ORDER BY timestamp DESC LIMIT :limit")
  fun observeRecent(limit: Int): Flow<List<FoodLogEntity>>

  @Query("SELECT * FROM food_log WHERE timestamp BETWEEN :startMs AND :endMs ORDER BY timestamp ASC")
  fun observeForRange(startMs: Long, endMs: Long): Flow<List<FoodLogEntity>>

  @Query("DELETE FROM food_log") suspend fun clear()
}
