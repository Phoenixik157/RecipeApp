package com.example.application.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.application.data.local.entity.CookingHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CookingHistoryDao {

    @Insert
    suspend fun insert(entry: CookingHistoryEntity): Long

    @Query("SELECT * FROM cooking_history ORDER BY cookedAt DESC")
    fun getAll(): Flow<List<CookingHistoryEntity>>

    @Query("SELECT * FROM cooking_history WHERE cookedAt >= :from ORDER BY cookedAt DESC")
    suspend fun getSince(from: Long): List<CookingHistoryEntity>

    @Query("SELECT COUNT(*) FROM cooking_history")
    suspend fun count(): Int

    @Query("DELETE FROM cooking_history")
    suspend fun clear()

    @Query("""
    SELECT COUNT(DISTINCT r.categoryId) FROM cooking_history ch
    INNER JOIN recipes r ON r.id = ch.recipeId
    """)
    suspend fun distinctCategoriesOnce(): Int

    @Query("SELECT COUNT(DISTINCT cookedAt / 86400000) FROM cooking_history")
    suspend fun distinctDaysOnce(): Int
}