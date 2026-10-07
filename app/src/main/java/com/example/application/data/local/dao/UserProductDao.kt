package com.example.application.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.application.data.local.entity.UserProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: UserProductEntity)

    @Query("DELETE FROM user_products WHERE ingredientId = :ingredientId")
    suspend fun deleteById(ingredientId: Long)

    @Query("DELETE FROM user_products")
    suspend fun clear()

    @Query("SELECT * FROM user_products")
    fun getAll(): Flow<List<UserProductEntity>>

    @Query("SELECT ingredientId FROM user_products")
    suspend fun getAllIds(): List<Long>

    @Query("SELECT COUNT(*) FROM user_products")
    fun countFlow(): Flow<Int>
}