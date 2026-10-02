package com.example.application.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.application.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<IngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: IngredientEntity): Long

    @Query("SELECT * FROM ingredients ORDER BY displayName ASC")
    fun getAll(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): IngredientEntity?

    /**
     * Поиск по запросу — учитывает синонимы.
     * "лук" найдёт и "лук репчатый", и "луковица".
     */
    @Query("""
        SELECT * FROM ingredients
        WHERE name LIKE '%' || :query || '%'
           OR displayName LIKE '%' || :query || '%'
           OR synonyms LIKE '%' || :query || '%'
        ORDER BY displayName ASC
        LIMIT 20
    """)
    suspend fun search(query: String): List<IngredientEntity>

    @Query("SELECT * FROM ingredients WHERE name IN (:names)")
    suspend fun getByNames(names: List<String>): List<IngredientEntity>

    @Query("SELECT COUNT(*) FROM ingredients")
    suspend fun count(): Int

    @Query("SELECT * FROM ingredients WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): IngredientEntity?

    @Query("SELECT * FROM ingredients WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<IngredientEntity>
}