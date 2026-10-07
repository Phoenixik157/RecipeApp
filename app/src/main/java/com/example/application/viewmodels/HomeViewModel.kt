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

    /** Есть ли у пользователя отмеченные продукты вообще. */
    private val _userHasProducts = MutableStateFlow(false)
    val userHasProducts: StateFlow<Boolean> = _userHasProducts

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
        _searchQuery,
        _minPurchasesMode
    ) { all, categoryId, query, minMode ->
        val filtered = all.filter { item ->
            val r = item.recipe
            val matchesCategory = categoryId == null || r.categoryId == categoryId
            val matchesQuery = query.isEmpty() || r.title.contains(query, true)
            matchesCategory && matchesQuery
        }
        if (minMode) {
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
            val hadProducts = _userHasProducts.value
            _userHasProducts.value = userIngredientIds.isNotEmpty()

            // Если продуктов не было — выключаем режим.
            // Если продуктов не было, а теперь появились — включаем автоматически.
            if (!_userHasProducts.value) {
                _minPurchasesMode.value = false
            } else if (!hadProducts) {
                _minPurchasesMode.value = true
            }
            // Если продукты уже были — оставляем текущее состояние (вкл или выкл).

            val allRecipes = db.recipeDao().getAllOnce()
            val allLinks = db.recipeIngredientDao().getAllOnce()
            val linksByRecipe = allLinks.groupBy { it.recipeId }

            val list = allRecipes.map { recipe ->
                val links = linksByRecipe[recipe.id].orEmpty()
                val total = links.size
                val have = links.count { it.ingredientId in userIngredientIds }
                RecipeWithMissing(recipe, total - have, total)
            }
            _allRecipesWithMissing.value = list
        }
    }

    /** Переключение режима вручную (по клику на чипс). */
    fun toggleMinPurchasesMode() {
        if (_userHasProducts.value) {
            _minPurchasesMode.value = !_minPurchasesMode.value
        }
    }

    fun selectCategory(id: Long?) { _selectedCategory.value = id }
    fun setSearchQuery(q: String) { _searchQuery.value = q }
}