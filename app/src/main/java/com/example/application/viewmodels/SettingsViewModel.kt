package com.example.application.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    /** Экспортирует пользовательские рецепты, статистику и историю в JSON-файл. */
    // --- ЭКСПОРТ ---
    fun exportToJson(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val root = JSONObject()

                    db.userStatsDao().get()?.let { stats ->
                        root.put("stats", JSONObject().apply {
                            put("totalXp", stats.totalXp)
                            put("level", stats.level)
                            put("dishesCooked", stats.dishesCooked)

                            // Сохраняем дневные лимиты и дату
                            put("dailyXpGained", stats.dailyXpGained)
                            put("lastResetDate", stats.lastResetDate)
                            put("dailyLogins", stats.dailyLogins)
                            put("dailyProductsAdded", stats.dailyProductsAdded)
                            put("dailyRecipesViewed", stats.dailyRecipesViewed)
                            put("dailyDishesCooked", stats.dailyDishesCooked)
                            put("dailyRecipesCreated", stats.dailyRecipesCreated)
                            put("dailyMinPurchasesUsed", stats.dailyMinPurchasesUsed)
                            put("dailyExports", stats.dailyExports)
                        })
                    }

                    val categories = db.categoryDao().getAllOnce().associateBy { it.id }
                    val allRecipes = db.recipeDao().getAllOnce().associateBy { it.id }
                    val allIngredients = db.ingredientDao().getAllOnce().associateBy { it.id }

                    // 1. Рецепты (сохраняем НАЗВАНИЕ категории вместо ID)
                    val userRecipes = db.recipeDao().getUserRecipesOnce()
                    val recipesArray = JSONArray()
                    val recipeIds = mutableListOf<Long>()

                    userRecipes.forEach { r ->
                        recipeIds.add(r.id)
                        recipesArray.put(JSONObject().apply {
                            put("id", r.id) // временно сохраняем, чтобы связать с ингредиентами
                            put("title", r.title)
                            put("description", r.description)
                            put("cookingTimeMinutes", r.cookingTimeMinutes)
                            put("complexity", r.complexity)
                            put("baseServings", r.baseServings)
                            put("instructions", r.instructions)
                            put("categoryName", categories[r.categoryId]?.name ?: "Основные")
                        })
                    }
                    root.put("recipes", recipesArray)

                    // 2. Ингредиенты (сохраняем НАЗВАНИЕ ингредиента)
                    val allLinks = db.recipeIngredientDao().getAllLinksOnce()
                    val userLinks = allLinks.filter { it.recipeId in recipeIds }
                    val linksArray = JSONArray()
                    userLinks.forEach { link ->
                        linksArray.put(JSONObject().apply {
                            put("recipeId", link.recipeId)
                            put("ingredientName", allIngredients[link.ingredientId]?.name ?: "")
                            put("quantity", link.quantity)
                            put("unit", link.unit)
                        })
                    }
                    root.put("recipe_ingredients", linksArray)

                    // 3. Продукты в холодильнике (сохраняем НАЗВАНИЯ)
                    val productsArray = JSONArray()
                    db.userProductDao().getAllIdsOnce().forEach { id ->
                        allIngredients[id]?.let { productsArray.put(it.name) }
                    }
                    root.put("user_products", productsArray)

                    // 4. Достижения
                    val achievementsArray = JSONArray()
                    db.achievementDao().getAllOnce().forEach { achievementsArray.put(it.id) }
                    root.put("achievements", achievementsArray)

                    // 5. История (сохраняем НАЗВАНИЕ рецепта вместо ID)
                    val historyArray = JSONArray()
                    db.cookingHistoryDao().getAllOnce().forEach { h ->
                        historyArray.put(JSONObject().apply {
                            put("recipeTitle", allRecipes[h.recipeId]?.title ?: "")
                            put("cookedAt", h.cookedAt)
                            put("servings", h.servings)
                            put("xpEarned", h.xpEarned)
                        })
                    }
                    root.put("history", historyArray)

                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(root.toString(2).toByteArray())
                    }

                    // Отмечаем ачивку
                    val stats = db.userStatsDao().get()
                    if (stats != null && !stats.hasExportedJson) {
                        db.userStatsDao().save(stats.copy(hasExportedJson = true))
                        com.example.application.utils.AchievementManager.checkAll(db)
                    }
                }
                onResult(true, "Экспорт завершён")
            } catch (e: Exception) {
                onResult(false, "Ошибка экспорта: ${e.message}")
            }
        }
    }

    // --- ИМПОРТ ---
    fun importFromJson(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val jsonString = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() } ?: return@withContext
                    val root = JSONObject(jsonString)

                    // Очищаем старые пользовательские данные
                    db.userProductDao().clear()
                    db.achievementDao().clear()
                    db.cookingHistoryDao().clear()
                    db.recipeDao().deleteAllUserRecipes()

                    if (root.has("stats")) {
                        val s = root.getJSONObject("stats")
                        db.userStatsDao().save(UserStatsEntity().copy(
                            totalXp = s.optInt("totalXp", 0),
                            level = s.optInt("level", 1),
                            dishesCooked = s.optInt("dishesCooked", 0),

                            // Восстанавливаем дневные лимиты и дату
                            dailyXpGained = s.optInt("dailyXpGained", 0),
                            lastResetDate = s.optString("lastResetDate", ""),
                            dailyLogins = s.optInt("dailyLogins", 0),
                            dailyProductsAdded = s.optInt("dailyProductsAdded", 0),
                            dailyRecipesViewed = s.optInt("dailyRecipesViewed", 0),
                            dailyDishesCooked = s.optInt("dailyDishesCooked", 0),
                            dailyRecipesCreated = s.optInt("dailyRecipesCreated", 0),
                            dailyMinPurchasesUsed = s.optInt("dailyMinPurchasesUsed", 0),
                            dailyExports = s.optInt("dailyExports", 0)
                        ))
                    }

                    // Восстанавливаем рецепты и запоминаем новые ID
                    val idMap = mutableMapOf<Long, Long>()
                    if (root.has("recipes")) {
                        val arr = root.getJSONArray("recipes")
                        val allCategories = db.categoryDao().getAllOnce()

                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val oldId = obj.getLong("id")
                            val categoryName = obj.optString("categoryName", "Основные")

                            // Ищем категорию по имени, если нет - берем "Основные", если и её нет - любую первую
                            val catId = allCategories.find { it.name == categoryName }?.id
                                ?: allCategories.find { it.name == "Основные" }?.id
                                ?: allCategories.firstOrNull()?.id ?: continue

                            val recipe = RecipeEntity(
                                id = 0, // 0 = сгенерировать новый ID
                                title = obj.getString("title"),
                                description = obj.optString("description", ""),
                                cookingTimeMinutes = obj.optInt("cookingTimeMinutes", 30),
                                complexity = obj.optInt("complexity", 1),
                                baseServings = obj.optInt("baseServings", 2),
                                instructions = obj.optString("instructions", ""),
                                categoryId = catId,
                                isUserRecipe = true
                            )
                            val newId = db.recipeDao().insert(recipe)
                            idMap[oldId] = newId
                        }
                    }

                    // Восстанавливаем ингредиенты рецептов
                    if (root.has("recipe_ingredients")) {
                        val arr = root.getJSONArray("recipe_ingredients")
                        val links = mutableListOf<RecipeIngredientEntity>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val oldRecipeId = obj.getLong("recipeId")
                            val newRecipeId = idMap[oldRecipeId] ?: continue
                            val ingName = obj.getString("ingredientName").lowercase().trim()
                            if (ingName.isEmpty()) continue

                            val existing = db.ingredientDao().getByName(ingName)
                            val ingId = existing?.id ?: db.ingredientDao().insert(
                                IngredientEntity(name = ingName, displayName = ingName, category = "other")
                            )

                            links.add(RecipeIngredientEntity(
                                recipeId = newRecipeId,
                                ingredientId = ingId,
                                quantity = obj.optDouble("quantity", 0.0),
                                unit = obj.optString("unit", "")
                            ))
                        }
                        if (links.isNotEmpty()) db.recipeIngredientDao().insertAll(links)
                    }

                    // Восстанавливаем холодильник
                    if (root.has("user_products")) {
                        val arr = root.getJSONArray("user_products")
                        for (i in 0 until arr.length()) {
                            val ingName = arr.getString(i).lowercase().trim()
                            val ing = db.ingredientDao().getByName(ingName)
                            if (ing != null) db.userProductDao().insert(UserProductEntity(ing.id))
                        }
                    }

                    if (root.has("achievements")) {
                        val arr = root.getJSONArray("achievements")
                        for (i in 0 until arr.length()) db.achievementDao().insert(AchievementEntity(arr.getString(i)))
                    }

                    // Восстанавливаем историю (ищем рецепт по названию)
                    if (root.has("history")) {
                        val arr = root.getJSONArray("history")
                        val allCurrentRecipes = db.recipeDao().getAllOnce()

                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val title = obj.optString("recipeTitle", "")
                            val recipe = allCurrentRecipes.find { it.title == title }

                            if (recipe != null) {
                                db.cookingHistoryDao().insert(CookingHistoryEntity(
                                    recipeId = recipe.id,
                                    cookedAt = obj.getLong("cookedAt"),
                                    servings = obj.optInt("servings", 2),
                                    xpEarned = obj.optInt("xpEarned", 0)
                                ))
                            }
                        }
                    }
                }
                onResult(true, "Данные успешно восстановлены")
            } catch (e: Exception) {
                onResult(false, "Ошибка импорта: ${e.message}")
            }
        }
    }

    // --- ОЧИСТКА ---
    fun clearAllData(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    db.clearAllTables()

                    // Восстанавливаем базовые данные
                    com.example.application.data.local.DatabaseSeeder.seed(getApplication(), db)
                    com.example.application.data.local.RecipeJsonImporter.importFromJson(getApplication(), db)

                    // Создаем пустую статистику и начисляем вход
                    db.userStatsDao().save(UserStatsEntity())
                    com.example.application.utils.XpManager.award(db, com.example.application.utils.XpManager.ACTION_DAILY_LOGIN)
                }
                onResult(true, "Все данные очищены. Приложение начинается с нуля.")
            } catch (e: Exception) {
                onResult(false, "Ошибка очистки: ${e.message}")
            }
        }
    }
}