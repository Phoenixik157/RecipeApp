package com.example.application.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.RecipeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MyRecipesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    // Потоки для поиска и сортировки
    private val _query = MutableStateFlow("")
    private val _sortType = MutableStateFlow(SortType.NAME)
    private val _isAscending = MutableStateFlow(true)

    // Геттеры для фрагмента (чтобы передавать текущее состояние в BottomSheet)
    val sortType: StateFlow<SortType> = _sortType
    val isAscending: StateFlow<Boolean> = _isAscending

    val recipes: StateFlow<List<RecipeEntity>> = combine(
        db.recipeDao().getUserRecipes(),
        _query,
        _sortType,
        _isAscending
    ) { list, q, sortType, isAscending ->
        // 1. Сначала фильтруем по поиску
        val filtered = list.filter { it.title.contains(q, ignoreCase = true) }

        // 2. Определяем базовый компаратор
        val comparator = when (sortType) {
            SortType.NAME -> compareBy<RecipeEntity> { it.title.lowercase() }
            SortType.TIME -> compareBy<RecipeEntity> { it.cookingTimeMinutes }
            SortType.COMPLEXITY -> compareBy<RecipeEntity> { it.complexity }
        }

        // 3. Применяем направление (по возрастанию/убыванию)
        val finalComparator = if (isAscending) comparator else comparator.reversed()

        // 4. Возвращаем отсортированный список
        filtered.sortedWith(finalComparator)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setSortType(type: SortType) { _sortType.value = type }
    fun setAscending(asc: Boolean) { _isAscending.value = asc }

    fun delete(recipe: RecipeEntity) {
        viewModelScope.launch { db.recipeDao().delete(recipe) }
    }
}