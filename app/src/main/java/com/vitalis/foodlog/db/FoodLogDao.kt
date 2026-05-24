package com.vitalis.foodlog.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {

  @Insert suspend fun insert(entry: FoodLogEntity): Long

  @Update suspend fun update(entry: FoodLogEntity)

  @Query("DELETE FROM food_log WHERE id = :id") suspend fun deleteById(id: Long)

  @Query("SELECT * FROM food_log ORDER BY timestamp DESC LIMIT :limit")
  fun observeRecent(limit: Int): Flow<List<FoodLogEntity>>

  @Query("SELECT * FROM food_log WHERE timestamp BETWEEN :startMs AND :endMs ORDER BY timestamp ASC")
  fun observeForRange(startMs: Long, endMs: Long): Flow<List<FoodLogEntity>>

  @Query("SELECT * FROM food_log WHERE timestamp >= :sinceMs ORDER BY timestamp DESC")
  suspend fun listSince(sinceMs: Long): List<FoodLogEntity>

  @Query("DELETE FROM food_log") suspend fun clear()
}
