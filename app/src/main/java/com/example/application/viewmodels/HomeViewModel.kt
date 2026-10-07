package com.example.application.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.CategoryEntity
import com.example.application.data.local.entity.RecipeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecipeWithMissing(
    val recipe: RecipeEntity,
    val missingCount: Int,
    val totalCount: Int
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _selectedCategory = MutableStateFlow<Long?>(null)
    val selectedCategory: StateFlow<Long?> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Режим «минимум покупок» — включается, если выбраны продукты
    private val _minPurchasesMode = MutableStateFlow(false)
    val minPurchasesMode: StateFlow<Boolean> = _minPurchasesMode

    // Все рецепты + посчитанные «не хватает»
    private val _allRecipesWithMissing = MutableStateFlow<List<RecipeWithMissing>>(emptyList())

    val categories: StateFlow<List<CategoryEntity>> = db.categoryDao()
        .getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Итоговый список для отображения: фильтр по категории + поиск + сортировка.
     */
    val recipes: StateFlow<List<RecipeWithMissing>> = combine(
        _allRecipesWithMissing,
        _selectedCategory,
        _searchQuery
    ) { all, categoryId, query ->
        val filtered = all.filter { item ->
            val r = item.recipe
            val matchesCategory = categoryId == null || r.categoryId == categoryId
            val matchesQuery = query.isEmpty() || r.title.contains(query, true)
            matchesCategory && matchesQuery
        }
        // Если включен режим «минимум покупок» — сортируем по missingCount
        if (_minPurchasesMode.value) {
            filtered.sortedBy { it.missingCount }
        } else {
            filtered
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        recalculateMissing()
    }

    /** Пересчитывает, сколько ингредиентов не хватает для каждого рецепта. */
    fun recalculateMissing() {
        viewModelScope.launch {
            val userIngredientIds = db.userProductDao().getAllIds().toSet()
            _minPurchasesMode.value = userIngredientIds.isNotEmpty()

            val allRecipes = db.recipeDao().getAllOnce()
            val allLinks = db.recipeIngredientDao().getAllOnce()
            val linksByRecipe = allLinks.groupBy { it.recipeId }

            val list = allRecipes.map { recipe ->
                val links = linksByRecipe[recipe.id].orEmpty()
                val total = links.size
                val have = links.count { it.ingredientId in userIngredientIds }
                val missing = total - have
                RecipeWithMissing(recipe, missing, total)
            }
            _allRecipesWithMissing.value = list
        }
    }

    fun selectCategory(id: Long?) { _selectedCategory.value = id }
    fun setSearchQuery(q: String) { _searchQuery.value = q }
}