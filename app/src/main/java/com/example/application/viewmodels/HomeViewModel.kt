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

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _selectedCategory = MutableStateFlow<Long?>(null)
    val selectedCategory: StateFlow<Long?> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    /** Категории из БД. */
    val categories: StateFlow<List<CategoryEntity>> = db.categoryDao()
        .getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Список рецептов. Зависит от выбранной категории и строки поиска.
     */
    val recipes: StateFlow<List<RecipeEntity>> = combine(
        db.recipeDao().getAll(),
        _selectedCategory,
        _searchQuery
    ) { all, categoryId, query ->
        all.filter { recipe ->
            val matchesCategory = categoryId == null || recipe.categoryId == categoryId
            val matchesQuery = query.isEmpty() ||
                    recipe.title.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(id: Long?) {
        _selectedCategory.value = id
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }
}