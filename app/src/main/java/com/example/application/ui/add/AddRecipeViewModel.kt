package com.example.application.ui.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.CategoryEntity
import com.example.application.data.local.entity.IngredientEntity
import com.example.application.data.local.entity.RecipeEntity
import com.example.application.data.local.entity.RecipeIngredientEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Строка ингредиента в форме. */
data class IngredientRow(
    var name: String = "",
    var quantity: String = "",
    var unit: String = ""
)

class AddRecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private var editingRecipeId: Long? = null

    init {
        viewModelScope.launch {
            db.categoryDao().getAll().collect { _categories.value = it }
        }
    }

    /** Загружает данные, если мы редактируем существующий рецепт. */
    suspend fun loadForEdit(recipeId: Long): RecipeEntity? {
        editingRecipeId = recipeId
        return db.recipeDao().getById(recipeId)
    }

    fun save(
        title: String,
        timeMinutes: Int,
        complexity: Int,
        servings: Int,
        instructions: String,
        categoryId: Long,
        ingredients: List<IngredientRow>
    ) {
        viewModelScope.launch {
            val recipe = RecipeEntity(
                id = editingRecipeId ?: 0,
                title = title,
                cookingTimeMinutes = timeMinutes,
                complexity = complexity,
                baseServings = servings,
                instructions = instructions,
                categoryId = categoryId,
                isUserRecipe = true
            )
            val recipeId: Long = if (editingRecipeId != null) {
                db.recipeDao().update(recipe)
                editingRecipeId!!
            } else {
                db.recipeDao().insert(recipe)
            }

            // Сначала удаляем старые связи (при редактировании)
            db.recipeIngredientDao().deleteByRecipeId(recipeId)

            // Создаём ингредиенты и связи
            val links = mutableListOf<RecipeIngredientEntity>()
            ingredients.forEach { row ->
                val name = row.name.trim().lowercase()
                if (name.isEmpty()) return@forEach
                val existing = db.ingredientDao().getByName(name)
                val ingId = existing?.id ?: db.ingredientDao().insert(
                    IngredientEntity(
                        name = name,
                        displayName = row.name.trim(),
                        emoji = "🥕",
                        synonyms = "",
                        category = "other"
                    )
                )
                links.add(
                    RecipeIngredientEntity(
                        recipeId = recipeId,
                        ingredientId = ingId,
                        quantity = row.quantity.toDoubleOrNull() ?: 0.0,
                        unit = row.unit.trim().ifEmpty { "по вкусу" }
                    )
                )
            }
            if (links.isNotEmpty()) db.recipeIngredientDao().insertAll(links)

            _saved.value = true
        }
    }
}