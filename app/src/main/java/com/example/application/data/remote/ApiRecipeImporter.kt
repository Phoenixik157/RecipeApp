package com.example.application.data.remote

import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.IngredientEntity
import com.example.application.data.local.entity.RecipeEntity
import com.example.application.data.local.entity.RecipeIngredientEntity
import com.example.application.data.remote.dto.MealDto

/**
 * Загружает рецепты из TheMealDB и сохраняет их в Room.
 * Реализует кеширование: если apiId уже есть — пропускаем.
 */
object ApiRecipeImporter {

    /** Слова для первичного наполнения. По каждому делаем search. */
    private val SEED_QUERIES = listOf("chicken", "pasta", "soup", "salad", "beef", "rice")

    suspend fun importInitialRecipes(db: AppDatabase) {
        if (db.recipeDao().count() > 5) return   // уже что-то есть
        SEED_QUERIES.forEach { query ->
            try {
                val response = RetrofitClient.mealApi.searchByName(query)
                response.meals?.forEach { dto ->
                    saveMeal(db, dto)
                }
            } catch (e: Exception) {
                // лог или игнорируем
            }
        }
    }

    suspend fun saveMeal(db: AppDatabase, dto: MealDto): Long? {
        val apiId = dto.idMeal ?: return null
        if (db.recipeDao().getByApiId(apiId) != null) return null  // уже есть

        val categoryName = mapCategory(dto.strCategory)
        val category = db.categoryDao().getByName(categoryName)
            ?: db.categoryDao().getByName("Основные")
            ?: return null

        val recipe = RecipeEntity(
            title = dto.strMeal ?: "Без названия",
            description = dto.strArea ?: "",
            imageUrl = dto.strMealThumb,
            cookingTimeMinutes = 30,
            complexity = 2,
            baseServings = 2,
            instructions = dto.strInstructions ?: "",
            categoryId = category.id,
            isUserRecipe = false,
            apiId = apiId,
            sourceName = "TheMealDB",
            sourceUrl = dto.strSource
        )
        val recipeId = db.recipeDao().insert(recipe)

        // Ингредиенты
        val links = mutableListOf<RecipeIngredientEntity>()
        dto.ingredientsWithMeasures().forEach { (name, measure) ->
            val normalized = name.lowercase().trim()
            val existing = db.ingredientDao().getByName(normalized)
            val ingId = existing?.id ?: db.ingredientDao().insert(
                IngredientEntity(
                    name = normalized,
                    displayName = name,
                    emoji = "🥕",
                    synonyms = "",
                    category = "other"
                )
            )
            links.add(
                RecipeIngredientEntity(
                    recipeId = recipeId,
                    ingredientId = ingId,
                    quantity = 0.0,
                    unit = measure.ifEmpty { "по вкусу" }
                )
            )
        }
        if (links.isNotEmpty()) db.recipeIngredientDao().insertAll(links)

        return recipeId
    }

    /** Сопоставляет категорию TheMealDB с нашей категорией в БД. */
    private fun mapCategory(api: String?): String = when (api?.lowercase()) {
        "breakfast" -> "Завтраки"
        "dessert" -> "Десерты"
        "starter", "side", "vegan", "vegetarian" -> "Салаты"
        else -> "Основные"
    }
}