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
    fun exportToJson(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val root = JSONObject()

                    // 1. Статистика
                    db.userStatsDao().get()?.let { stats ->
                        root.put("stats", JSONObject().apply {
                            put("totalXp", stats.totalXp)
                            put("level", stats.level)
                            put("dishesCooked", stats.dishesCooked)
                        })
                    }

                    // 2. Пользовательские рецепты
                    val userRecipes = db.recipeDao().getUserRecipesOnce()
                    val recipesArray = JSONArray()
                    val recipeIds = mutableListOf<Long>()

                    userRecipes.forEach { r ->
                        recipeIds.add(r.id)
                        recipesArray.put(JSONObject().apply {
                            put("id", r.id)
                            put("title", r.title)
                            put("description", r.description)
                            put("cookingTimeMinutes", r.cookingTimeMinutes)
                            put("complexity", r.complexity)
                            put("baseServings", r.baseServings)
                            put("instructions", r.instructions)
                            put("categoryId", r.categoryId)
                        })
                    }
                    root.put("recipes", recipesArray)

                    // 3. Ингредиенты пользовательских рецептов
                    val allLinks = db.recipeIngredientDao().getAllLinksOnce()
                    val userLinks = allLinks.filter { it.recipeId in recipeIds }
                    val linksArray = JSONArray()
                    userLinks.forEach { link ->
                        linksArray.put(JSONObject().apply {
                            put("recipeId", link.recipeId)
                            put("ingredientId", link.ingredientId) // или можно сохранять имя ингредиента, чтобы не зависеть от ID
                            put("quantity", link.quantity)
                            put("unit", link.unit)
                        })
                    }
                    root.put("recipe_ingredients", linksArray)

                    // 4. Продукты в холодильнике
                    val userProducts = db.userProductDao().getAllIdsOnce()
                    val productsArray = JSONArray()
                    userProducts.forEach { id -> productsArray.put(id) }
                    root.put("user_products", productsArray)

                    // 5. Достижения
                    val achievements = db.achievementDao().getAllOnce()
                    val achievementsArray = JSONArray()
                    achievements.forEach { a -> achievementsArray.put(a.id) }
                    root.put("achievements", achievementsArray)

                    // 6. История готовки
                    val history = db.cookingHistoryDao().getAllOnce()
                    val historyArray = JSONArray()
                    history.forEach { h ->
                        historyArray.put(JSONObject().apply {
                            put("recipeId", h.recipeId)
                            put("cookedAt", h.cookedAt)
                            put("servings", h.servings)
                            put("xpEarned", h.xpEarned)
                        })
                    }
                    root.put("history", historyArray)

                    // Запись файла
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(root.toString(2).toByteArray())
                    }
                }
                onResult(true, "Экспорт завершён")
            } catch (e: Exception) {
                onResult(false, "Ошибка экспорта: ${e.message}")
            }
        }
    }

    // --- ИМПОРТ ИЗ JSON ---
    fun importFromJson(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val jsonString = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() } ?: return@withContext

                    val root = JSONObject(jsonString)

                    // 1. Удаляем текущие пользовательские данные перед импортом
                    db.userProductDao().clear()
                    db.achievementDao().clear()
                    db.cookingHistoryDao().clear()
                    db.recipeDao().deleteAllUserRecipes()

                    // 2. Статистика
                    if (root.has("stats")) {
                        val s = root.getJSONObject("stats")
                        val currentStats = db.userStatsDao().get() ?: UserStatsEntity()
                        db.userStatsDao().save(currentStats.copy(
                            totalXp = s.optInt("totalXp", 0),
                            level = s.optInt("level", 1),
                            dishesCooked = s.optInt("dishesCooked", 0)
                        ))
                    }

                    // 3. Рецепты
                    if (root.has("recipes")) {
                        val arr = root.getJSONArray("recipes")
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val recipe = RecipeEntity(
                                id = obj.getLong("id"),
                                title = obj.getString("title"),
                                description = obj.optString("description", ""),
                                cookingTimeMinutes = obj.optInt("cookingTimeMinutes", 30),
                                complexity = obj.optInt("complexity", 1),
                                baseServings = obj.optInt("baseServings", 2),
                                instructions = obj.optString("instructions", ""),
                                categoryId = obj.getLong("categoryId"),
                                isUserRecipe = true
                            )
                            db.recipeDao().insert(recipe)
                        }
                    }

                    // 4. Ингредиенты рецептов
                    if (root.has("recipe_ingredients")) {
                        val arr = root.getJSONArray("recipe_ingredients")
                        val links = mutableListOf<RecipeIngredientEntity>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            links.add(RecipeIngredientEntity(
                                recipeId = obj.getLong("recipeId"),
                                ingredientId = obj.getLong("ingredientId"),
                                quantity = obj.optDouble("quantity", 0.0),
                                unit = obj.optString("unit", "")
                            ))
                        }
                        if (links.isNotEmpty()) db.recipeIngredientDao().insertAll(links)
                    }

                    // 5. Продукты в холодильнике
                    if (root.has("user_products")) {
                        val arr = root.getJSONArray("user_products")
                        for (i in 0 until arr.length()) {
                            db.userProductDao().insert(UserProductEntity(arr.getLong(i)))
                        }
                    }

                    // 6. Достижения
                    if (root.has("achievements")) {
                        val arr = root.getJSONArray("achievements")
                        for (i in 0 until arr.length()) {
                            db.achievementDao().insert(AchievementEntity(arr.getString(i)))
                        }
                    }

                    // 7. История
                    if (root.has("history")) {
                        val arr = root.getJSONArray("history")
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            db.cookingHistoryDao().insert(CookingHistoryEntity(
                                recipeId = obj.getLong("recipeId"),
                                cookedAt = obj.getLong("cookedAt"),
                                servings = obj.optInt("servings", 2),
                                xpEarned = obj.optInt("xpEarned", 0)
                            ))
                        }
                    }
                }
                onResult(true, "Данные успешно восстановлены")
            } catch (e: Exception) {
                onResult(false, "Ошибка импорта: ${e.message}")
            }
        }
    }

    // --- ОЧИСТКА ВСЕХ ДАННЫХ ---
    fun clearAllData(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Полностью очищаем БД
                    db.clearAllTables()

                    // Заново наполняем базовыми категориями и рецептами
                    com.example.application.data.local.DatabaseSeeder.seed(getApplication(), db)
                    com.example.application.data.local.RecipeJsonImporter.importFromJson(getApplication(), db)
                }
                onResult(true, "Все данные очищены. Приложение начинается с нуля.")
            } catch (e: Exception) {
                onResult(false, "Ошибка очистки: ${e.message}")
            }
        }
    }
}