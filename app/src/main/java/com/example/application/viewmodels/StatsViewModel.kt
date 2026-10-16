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
import kotlinx.coroutines.flow.map

data class QuickMetrics(
    val favouriteCategory: String = "—",
    val averageTimeMinutes: Int = 0,
    val favouriteComplexity: String = "—"
)

data class CategorySlice(val name: String, val count: Int, val color: Int)

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

    private val _categorySlices = MutableStateFlow<List<CategorySlice>>(emptyList())
    val categorySlices: StateFlow<List<CategorySlice>> = _categorySlices

    private val _weekBars = MutableStateFlow<List<Int>>(emptyList())  // 7 значений, Пн..Вс
    val weekBars: StateFlow<List<Int>> = _weekBars

    val unlockedAchievements: StateFlow<List<String>> =
        db.achievementDao().getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            .let { flow ->
                flow.map { list -> list.map { it.id } }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            }

    init {
        viewModelScope.launch {
            db.cookingHistoryDao().getAll().collect { list ->
                _history.value = list
                _metrics.value = computeMetrics(list)
                _categorySlices.value = computeCategorySlices(list)
                _weekBars.value = computeWeekBars(list)
            }
            com.example.application.utils.AchievementManager.checkAll(db)
        }
    }

    private suspend fun computeMetrics(history: List<CookingHistoryEntity>): QuickMetrics {
        if (history.isEmpty()) return QuickMetrics()

        val allRecipes = db.recipeDao().getAllOnce()
        val recipeById = allRecipes.associateBy { it.id }

        // Среднее время — по каждому приготовлению (с повторами)
        val times = history.mapNotNull { recipeById[it.recipeId]?.cookingTimeMinutes }
        val avgTime = if (times.isNotEmpty()) times.average().toInt() else 0

        // Категории — считаем по каждому приготовлению
        val categoryCount = mutableMapOf<Long, Int>()
        history.forEach { h ->
            val recipe = recipeById[h.recipeId] ?: return@forEach
            categoryCount[recipe.categoryId] = (categoryCount[recipe.categoryId] ?: 0) + 1
        }
        val maxCatCount = categoryCount.values.maxOrNull() ?: 0
        val topCategoryNames = if (maxCatCount > 0) {
            categoryCount.filter { it.value == maxCatCount }.keys
                .mapNotNull { db.categoryDao().getById(it)?.name }
                .sorted()
        } else emptyList()
        val favCategoryName = if (topCategoryNames.isEmpty()) "—" else topCategoryNames.joinToString(", ")

        // Сложность — считаем по каждому приготовлению
        val complexityCount = mutableMapOf<Int, Int>()
        history.forEach { h ->
            val recipe = recipeById[h.recipeId] ?: return@forEach
            complexityCount[recipe.complexity] = (complexityCount[recipe.complexity] ?: 0) + 1
        }
        val maxComplCount = complexityCount.values.maxOrNull() ?: 0
        val topComplexities = if (maxComplCount > 0) {
            complexityCount.filter { it.value == maxComplCount }.keys.sorted()
        } else emptyList()
        val favComplexity = if (topComplexities.isEmpty()) "—" else {
            topComplexities.joinToString(", ") { level ->
                when (level) {
                    1 -> "Легко"
                    2 -> "Средне"
                    3 -> "Сложно"
                    else -> "?"
                }
            }
        }

        return QuickMetrics(
            favouriteCategory = favCategoryName,
            averageTimeMinutes = avgTime,
            favouriteComplexity = favComplexity
        )
    }

    private suspend fun computeCategorySlices(history: List<CookingHistoryEntity>): List<CategorySlice> {
        if (history.isEmpty()) return emptyList()
        val recipes = db.recipeDao().getAllOnce().associateBy { it.id }
        val counts = mutableMapOf<Long, Int>()
        history.forEach { h ->
            val r = recipes[h.recipeId] ?: return@forEach
            counts[r.categoryId] = (counts[r.categoryId] ?: 0) + 1
        }
        val palette = listOf(
            0xFFFF7043.toInt(),  // оранжевый
            0xFF42A5F5.toInt(),  // синий
            0xFF66BB6A.toInt(),  // зелёный
            0xFFAB47BC.toInt(),  // пурпурный
            0xFFFFA726.toInt()   // жёлто-оранжевый
        )
        return counts.entries
            .sortedByDescending { it.value }
            .mapIndexed { idx, (catId, count) ->
                CategorySlice(
                    name = db.categoryDao().getById(catId)?.name ?: "—",
                    count = count,
                    color = palette[idx % palette.size]
                )
            }
    }

    private fun computeWeekBars(history: List<CookingHistoryEntity>): List<Int> {
        // Последние 7 календарных дней (сегодня — последний элемент)
        val cal = java.util.Calendar.getInstance()
        // Смещаемся к началу сегодняшнего дня
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        val dayMillis = 86400000L
        val result = MutableList(7) { 0 }

        history.forEach { h ->
            val dayIndex = ((h.cookedAt - (todayStart - 6 * dayMillis)) / dayMillis).toInt()
            if (dayIndex in 0..6) result[dayIndex]++
        }
        return result
    }

    fun dailyMaxXp(): Int = XpManager.dailyMaxXp()

    fun setShowDailyLimits(show: Boolean) {
        _showDailyLimits.value = show
    }
}