package com.example.application.utils

import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.UserStatsEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
object AchievementManager {

    const val NOVICE = "novice"
    const val MARATHON = "marathon"
    const val EXPERIMENTER = "experimenter"
    const val ECONOMIST = "economist"
    const val BACKUP_MASTER = "backup_master"
    const val GOURMET = "gourmet"

    val TITLES = mapOf(
        NOVICE to "🥉 Новичок",
        MARATHON to "🏃 Марафон",
        EXPERIMENTER to "🧪 Экспериментатор",
        ECONOMIST to "💰 Экономист",
        BACKUP_MASTER to "💾 Мастер бэкапа",
        GOURMET to "👨‍🍳 Гурман"
    )

    private val _newlyUnlocked = MutableSharedFlow<List<String>>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val newlyUnlocked: SharedFlow<List<String>> = _newlyUnlocked

    fun isUnlocked(stats: UserStatsEntity, id: String): Boolean =
        stats.unlockedAchievements.split(";").contains(id)

    /** Возвращает список id ачивок, которые открылись именно сейчас. */
    suspend fun checkAll(db: AppDatabase): List<String> {
        val stats = db.userStatsDao().get() ?: return emptyList()
        val unlocked = stats.unlockedAchievements.split(";").filter { it.isNotBlank() }.toMutableSet()
        val newlyUnlocked = mutableListOf<String>()

        // Новичок — 1 блюдо приготовлено
        if (stats.dishesCooked >= 1 && NOVICE !in unlocked) {
            newlyUnlocked.add(NOVICE)
        }

        // Экспериментатор — 3 своих рецепта
        val userRecipesCount = db.recipeDao().countUserRecipesOnce()
        if (userRecipesCount >= 3 && EXPERIMENTER !in unlocked) {
            newlyUnlocked.add(EXPERIMENTER)
        }

        // Экономист — 10 использований «Минимум покупок»
        if (stats.totalMinPurchasesUsed >= 10 && ECONOMIST !in unlocked) {
            newlyUnlocked.add(ECONOMIST)
        }

        // Гурман — приготовлено из 3+ разных категорий
        val distinctCategories = db.cookingHistoryDao().distinctCategoriesOnce()
        if (distinctCategories >= 3 && GOURMET !in unlocked) {
            newlyUnlocked.add(GOURMET)
        }

        // Марафон — 5 разных дней с записями в истории (проверяем по датам)
        val distinctDays = db.cookingHistoryDao().distinctDaysOnce()
        if (distinctDays >= 5 && MARATHON !in unlocked) {
            newlyUnlocked.add(MARATHON)
        }

        if (newlyUnlocked.isNotEmpty()) {
            val updated = (unlocked + newlyUnlocked).joinToString(";")
            db.userStatsDao().save(stats.copy(unlockedAchievements = updated))
            _newlyUnlocked.tryEmit(newlyUnlocked)
        }

        return newlyUnlocked
    }
}