package com.example.application.data.local

import android.content.Context
import com.example.application.data.local.entity.IngredientEntity
import com.example.application.data.local.entity.RecipeEntity
import com.example.application.data.local.entity.RecipeIngredientEntity
import org.json.JSONArray

/**
 * Загружает рецепты из assets/recipes.json и наполняет ими БД.
 * Работает один раз — если рецептов уже больше 5, повторно не грузит.
 */
object RecipeJsonImporter {

    suspend fun importFromJson(context: Context, db: AppDatabase) {
        if (db.recipeDao().count() > 5) return

        val json = context.assets.open("recipes.json")
            .bufferedReader().use { it.readText() }
        val array = JSONArray(json)

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val title = obj.getString("title")
            val categoryName = obj.getString("category")

            val category = db.categoryDao().getByName(categoryName) ?: continue

            val recipe = RecipeEntity(
                title = title,
                description = "",
                imageUrl = if (obj.isNull("imageUrl")) null else obj.getString("imageUrl"),
                cookingTimeMinutes = obj.optInt("cookingTimeMinutes", 30),
                complexity = obj.optInt("complexity", 1),
                baseServings = obj.optInt("baseServings", 2),
                instructions = obj.getString("instructions"),
                categoryId = category.id,
                isUserRecipe = false
            )
            val recipeId = db.recipeDao().insert(recipe)

            val ingredientsArr = obj.getJSONArray("ingredients")
            val links = mutableListOf<RecipeIngredientEntity>()
            for (j in 0 until ingredientsArr.length()) {
                val ingObj = ingredientsArr.getJSONObject(j)
                val name = ingObj.getString("name").lowercase().trim()
                val quantity = ingObj.optDouble("quantity", 0.0)
                val unit = ingObj.optString("unit", "")

                val existing = db.ingredientDao().getByName(name)
                val ingId = existing?.id ?: db.ingredientDao().insert(
                    IngredientEntity(
                        name = name,
                        displayName = ingObj.getString("name"),
                        emoji = "🥕",
                        synonyms = "",
                        category = "other"
                    )
                )

                links.add(
                    RecipeIngredientEntity(
                        recipeId = recipeId,
                        ingredientId = ingId,
                        quantity = quantity,
                        unit = unit
                    )
                )
            }
            if (links.isNotEmpty()) db.recipeIngredientDao().insertAll(links)
        }
    }
}