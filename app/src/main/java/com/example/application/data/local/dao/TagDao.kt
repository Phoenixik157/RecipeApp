package com.example.application.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.application.data.local.entity.RecipeTagCrossRef
import com.example.application.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: RecipeTagCrossRef)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAll(): Flow<List<TagEntity>>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN recipe_tags rt ON rt.tagId = t.id
        WHERE rt.recipeId = :recipeId
    """)
    suspend fun getTagsForRecipe(recipeId: Long): List<TagEntity>

    @Query("DELETE FROM recipe_tags WHERE recipeId = :recipeId")
    suspend fun deleteCrossRefsForRecipe(recipeId: Long)
}