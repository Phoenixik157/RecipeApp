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
}