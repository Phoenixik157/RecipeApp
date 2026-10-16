package com.example.application.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.CookingHistoryEntity
import com.example.application.data.local.entity.UserStatsEntity
import com.example.application.utils.XpManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuickMetrics(
    val favouriteCategory: String = "—",
    val averageTimeMinutes: Int = 0,
    val favouriteComplexity: String = "—"
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    val userStats: StateFlow<UserStatsEntity?> = db.userStatsDao().observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _metrics = MutableStateFlow(QuickMetrics())
    val metrics: StateFlow<QuickMetrics> = _metrics

    private val _history = MutableStateFlow<List<CookingHistoryEntity>>(emptyList())
    val history: StateFlow<List<CookingHistoryEntity>> = _history

    // Показывать ли блок «Дневной лимит» (галочка в настройках, по умолчанию true)
    private val _showDailyLimits = MutableStateFlow(true)
    val showDailyLimits: StateFlow<Boolean> = _showDailyLimits

    init {
        viewModelScope.launch {
            db.cookingHistoryDao().getAll().collect { list ->
                _history.value = list
                _metrics.value = computeMetrics(list)
            }
        }
    }

    private suspend fun computeMetrics(history: List<CookingHistoryEntity>): QuickMetrics {
        if (history.isEmpty()) return QuickMetrics()

        // Среднее время готовки
        val recipeIds = history.map { it.recipeId }.distinct()
        val recipes = db.recipeDao().getAllOnce().filter { it.id in recipeIds }
        val avgTime = if (recipes.isNotEmpty()) {
            recipes.map { it.cookingTimeMinutes }.average().toInt()
        } else 0

        // Любимая категория
        val categoryCount = mutableMapOf<Long, Int>()
        history.forEach { h ->
            val recipe = recipes.find { it.id == h.recipeId } ?: return@forEach
            categoryCount[recipe.categoryId] = (categoryCount[recipe.categoryId] ?: 0) + 1
        }
        val favCatId = categoryCount.maxByOrNull { it.value }?.key
        val favCategoryName = if (favCatId != null) {
            db.categoryDao().getById(favCatId)?.name ?: "—"
        } else "—"

        // Любимая сложность (чаще всего выбирает)
        val complexityCount = mutableMapOf<Int, Int>()
        recipes.forEach { r ->
            complexityCount[r.complexity] = (complexityCount[r.complexity] ?: 0) + 1
        }
        val favComplexity = when (complexityCount.maxByOrNull { it.value }?.key) {
            1 -> "Легко"
            2 -> "Средне"
            3 -> "Сложно"
            else -> "—"
        }

        return QuickMetrics(
            favouriteCategory = favCategoryName,
            averageTimeMinutes = avgTime,
            favouriteComplexity = favComplexity
        )
    }

    fun dailyMaxXp(): Int = XpManager.dailyMaxXp()

    fun setShowDailyLimits(show: Boolean) {
        _showDailyLimits.value = show
    }
}