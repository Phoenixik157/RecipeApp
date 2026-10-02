package com.example.application.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.IngredientEntity
import com.example.application.data.local.entity.RecipeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class IngredientWithQty(
    val ingredient: IngredientEntity,
    val quantity: Double,
    val unit: String
)

class RecipeDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _recipe = MutableStateFlow<RecipeEntity?>(null)
    val recipe: StateFlow<RecipeEntity?> = _recipe

    private val _ingredients = MutableStateFlow<List<IngredientWithQty>>(emptyList())
    val ingredients: StateFlow<List<IngredientWithQty>> = _ingredients

    private val _servings = MutableStateFlow(2)
    val servings: StateFlow<Int> = _servings

    private var loaded = false

    fun load(recipeId: Long) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            val recipe = db.recipeDao().getById(recipeId) ?: return@launch
            _recipe.value = recipe
            _servings.value = recipe.baseServings

            val links = db.recipeIngredientDao().getByRecipeId(recipeId)
            val ingIds = links.map { it.ingredientId }
            val ings = db.ingredientDao().getByIds(ingIds)
            val map = ings.associateBy { it.id }
            _ingredients.value = links.mapNotNull { link ->
                map[link.ingredientId]?.let { ing ->
                    IngredientWithQty(ing, link.quantity, link.unit)
                }
            }
        }
    }

    fun increaseServings() { _servings.value = _servings.value + 1 }
    fun decreaseServings() { if (_servings.value > 1) _servings.value = _servings.value - 1 }
}