package com.example.application.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.application.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements")
    suspend fun getAllOnce(): List<AchievementEntity>

    @Query("SELECT * FROM achievements")
    fun getAll(): Flow<List<AchievementEntity>>

    @Query("SELECT id FROM achievements")
    suspend fun getIdsOnce(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM achievements WHERE id = :id)")
    suspend fun isUnlocked(id: String): Boolean

    @Query("DELETE FROM achievements")
    suspend fun clear()
}