package com.example.application.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.application.data.local.entity.RecipeIngredientEntity

@Dao
interface RecipeIngredientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecipeIngredientEntity>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteByRecipeId(recipeId: Long)

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun getByRecipeId(recipeId: Long): List<RecipeIngredientEntity>

    /**
     * ГЛАВНЫЙ ПОИСК: рецепты, где есть ВСЕ указанные ингредиенты.
     */
    @Query("""
        SELECT recipeId FROM recipe_ingredients
        WHERE ingredientId IN (:ingredientIds)
        GROUP BY recipeId
        HAVING COUNT(DISTINCT ingredientId) = :total
        ORDER BY recipeId DESC
    """)
    suspend fun findRecipesWithAllIngredients(
        ingredientIds: List<Long>,
        total: Int
    ): List<Long>

    /**
     * Рецепты, где есть ХОТЯ БЫ ОДИН ингредиент.
     */
    @Query("""
        SELECT DISTINCT recipeId FROM recipe_ingredients
        WHERE ingredientId IN (:ingredientIds)
    """)
    suspend fun findRecipesWithAnyIngredient(ingredientIds: List<Long>): List<Long>

    @Query("SELECT COUNT(*) FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun countIngredients(recipeId: Long): Int

    @Query("SELECT * FROM recipe_ingredients")
    suspend fun getAllOnce(): List<RecipeIngredientEntity>
}