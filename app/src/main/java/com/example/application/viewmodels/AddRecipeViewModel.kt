package com.example.application.viewmodels

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
import com.example.application.utils.XpManager
import com.example.application.utils.AchievementManager

data class IngredientRow(
    val id: String = java.util.UUID.randomUUID().toString(),
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

    // --- СОСТОЯНИЕ ФОРМЫ ---
    val title = MutableStateFlow("")
    val timeMinutes = MutableStateFlow("")
    val instructions = MutableStateFlow("")
    val difficulty = MutableStateFlow(2)
    val servings = MutableStateFlow(1)
    val selectedCategoryName = MutableStateFlow("")
    val ingredientRows = MutableStateFlow<List<IngredientRow>>(
        listOf(IngredientRow(), IngredientRow(), IngredientRow())
    )

    var editingRecipeId: Long? = null

    init {
        viewModelScope.launch {
            db.categoryDao().getAll().collect { _categories.value = it }
        }
    }

    /** Очищает форму (вызывается при отмене или после сохранения) */
    fun clearState() {
        title.value = ""
        timeMinutes.value = ""
        instructions.value = ""
        difficulty.value = 2
        servings.value = 1
        selectedCategoryName.value = ""
        ingredientRows.value = listOf(IngredientRow(), IngredientRow(), IngredientRow())
        editingRecipeId = null
        _saved.value = false
    }

    /** Загружает данные рецепта в ViewModel для редактирования */
    suspend fun loadForEdit(recipeId: Long): RecipeEntity? {
        editingRecipeId = recipeId
        val recipe = db.recipeDao().getById(recipeId) ?: return null

        title.value = recipe.title
        timeMinutes.value = recipe.cookingTimeMinutes.toString()
        instructions.value = recipe.instructions
        difficulty.value = recipe.complexity
        servings.value = recipe.baseServings

        val cat = db.categoryDao().getById(recipe.categoryId)
        selectedCategoryName.value = cat?.name ?: ""

        val links = db.recipeIngredientDao().getByRecipeId(recipeId)
        val ids = links.map { it.ingredientId }
        val ings = db.ingredientDao().getByIds(ids)
        val map = ings.associateBy { it.id }

        ingredientRows.value = links.map { link ->
            val ing = map[link.ingredientId]
            IngredientRow(
                name = ing?.displayName ?: "",
                quantity = if (link.quantity % 1.0 == 0.0) link.quantity.toInt().toString() else link.quantity.toString(),
                unit = link.unit
            )
        }.ifEmpty { listOf(IngredientRow(), IngredientRow(), IngredientRow()) }

        return recipe
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

            db.recipeIngredientDao().deleteByRecipeId(recipeId)

            val links = mutableListOf<RecipeIngredientEntity>()
            ingredients.forEach { row ->
                val name = row.name.trim().lowercase()
                if (name.isEmpty()) return@forEach
                val existing = db.ingredientDao().getByName(name)
                val ingId = existing?.id ?: db.ingredientDao().insert(
                    IngredientEntity(
                        name = name,
                        displayName = row.name.trim(),
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

            if (editingRecipeId == null) {
                XpManager.award(db, XpManager.ACTION_CREATE_RECIPE)
            }
            AchievementManager.checkAll(db)
            _saved.value = true
        }
    }
}