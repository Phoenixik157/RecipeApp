package com.example.application.utils

import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Управление достижениями. Не связано с XP и дневными лимитами.
 * Проверяет факты в БД и раздаёт уведомления через SharedFlow.
 */
object AchievementManager {

    const val NOVICE = "novice"
    const val EXPERIMENTER = "experimenter"
    const val ECONOMIST = "economist"
    const val GOURMET = "gourmet"
    const val MARATHON = "marathon"
    const val BACKUP_MASTER = "backup_master"

    // Ключи для usage_counters
    const val KEY_MIN_PURCHASES = "min_purchases"

    val TITLES = mapOf(
        NOVICE to "🥉 Новичок",
        EXPERIMENTER to "🧪 Экспериментатор",
        ECONOMIST to "💰 Экономист",
        GOURMET to "👨‍🍳 Гурман",
        MARATHON to "🏃 Марафон",
        BACKUP_MASTER to "💾 Мастер бэкапа"
    )

    private val _newlyUnlocked = MutableSharedFlow<List<String>>(
        replay = 0, extraBufferCapacity = 1
    )
    val newlyUnlocked: SharedFlow<List<String>> = _newlyUnlocked

    val DESCRIPTIONS = mapOf(
        NOVICE to "Приготовьте свое первое блюдо (нажмите «Я приготовил!» на любом рецепте).",
        EXPERIMENTER to "Создайте 3 собственных рецепта.",
        ECONOMIST to "Включите режим «Минимум покупок» 10 раз.",
        GOURMET to "Приготовьте блюда из 3 разных категорий (например, суп, салат и десерт).",
        MARATHON to "Готовьте еду 5 дней подряд (записи должны быть в разные дни).",
        BACKUP_MASTER to "Сделайте экспорт резервной копии JSON в настройках."
    )

    /** Проверяет все условия и выдаёт достижения. Возвращает id новых. */
    suspend fun checkAll(db: AppDatabase): List<String> {
        val unlocked = db.achievementDao().getIdsOnce().toSet()
        val newOnes = mutableListOf<String>()

        // Новичок — 1 приготовленное блюдо
        if (NOVICE !in unlocked && db.cookingHistoryDao().count() >= 1) {
            newOnes.add(NOVICE)
        }

        // Экспериментатор — 3 своих рецепта
        if (EXPERIMENTER !in unlocked && db.recipeDao().countUserRecipesOnce() >= 3) {
            newOnes.add(EXPERIMENTER)
        }

        // Экономист — 10 использований «Минимум покупок»
        val minPurch = db.usageCounterDao().getCount(KEY_MIN_PURCHASES) ?: 0
        if (ECONOMIST !in unlocked && minPurch >= 10) {
            newOnes.add(ECONOMIST)
        }

        // Гурман — готовил блюда из 3+ разных категорий
        if (GOURMET !in unlocked && db.cookingHistoryDao().distinctCategoriesOnce() >= 3) {
            newOnes.add(GOURMET)
        }
        
        // Марафон — 5 дней ПОДРЯД
        val history = db.cookingHistoryDao().getAllOnce()
        val uniqueDays = history.map {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(it.cookedAt))
        }.distinct().sortedDescending()

        var maxStreak = 0
        var currentStreak = 0
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val calendar = java.util.Calendar.getInstance()

        for (i in uniqueDays.indices) {
            if (i == 0) {
                currentStreak = 1
            } else {
                val d1 = format.parse(uniqueDays[i - 1])
                val d2 = format.parse(uniqueDays[i])
                calendar.time = d1
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
                if (calendar.time == d2) {
                    currentStreak++
                } else {
                    currentStreak = 1
                }
            }
            maxStreak = maxOf(maxStreak, currentStreak)
        }

        if (maxStreak >= 5 && MARATHON !in unlocked) {
            newOnes.add(MARATHON)
        }

        // Мастер бэкапа — сделал экспорт JSON
        val stats = db.userStatsDao().get()
        if (stats?.hasExportedJson == true && BACKUP_MASTER !in unlocked) {
            newOnes.add(BACKUP_MASTER)
        }

        // Сохраняем новые
        newOnes.forEach { id ->
            db.achievementDao().insert(AchievementEntity(id = id))
        }


        if (newOnes.isNotEmpty()) {
            _newlyUnlocked.tryEmit(newOnes)
        }
        return newOnes
    }
}