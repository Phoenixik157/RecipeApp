package com.example.application.utils

import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.UserStatsEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Управляет опытом (XP), уровнями и дневными лимитами.
 * Считает накопленный XP, пересчитывает уровень, сбрасывает дневные счётчики.
 */
object XpManager {

    const val ACTION_DAILY_LOGIN = "daily_login"
    const val ACTION_VIEW_RECIPE = "view_recipe"
    const val ACTION_COOKED = "cooked"
    const val ACTION_CREATE_RECIPE = "create_recipe"
    const val ACTION_ADD_PRODUCT = "add_product"
    const val ACTION_MIN_PURCHASES = "min_purchases"
    const val ACTION_EXPORT = "export"

    // action -> (сколько XP за раз, максимум раз в день)
    private val LIMITS = mapOf(
        ACTION_DAILY_LOGIN to (2 to 1),
        ACTION_VIEW_RECIPE to (1 to 3),
        ACTION_COOKED to (20 to 3),
        ACTION_CREATE_RECIPE to (15 to 2),
        ACTION_ADD_PRODUCT to (1 to 5),
        ACTION_MIN_PURCHASES to (3 to 1),
        ACTION_EXPORT to (5 to 1)
    )

    /** Сколько всего XP нужно накопить, чтобы достичь уровня level. */
    fun xpForLevel(level: Int): Int {
        var total = 0
        for (i in 1 until level) total += i * 100
        return total
    }

    /** Какой уровень соответствует накопленному XP. */
    fun levelForXp(xp: Int): Int {
        var level = 1
        while (xpForLevel(level + 1) <= xp) level++
        return level
    }

    /** Название уровня для UI. */
    fun levelName(level: Int): String = when {
        level <= 1 -> "🥚 Новичок"
        level == 2 -> "🥣 Ученик"
        level == 3 -> "🍳 Любитель"
        level == 4 -> "👨‍🍳 Повар"
        level == 5 -> "🔪 Су-шеф"
        level == 6 -> "🎩 Шеф-повар"
        level == 7 -> "🌟 Мастер"
        level == 8 -> "🏆 Кулинарный гуру"
        level == 9 -> "📜 Легенда кухни"
        level == 10 -> "💎 Мишленовский шеф"
        else -> "👑 Бессмертный кулинар (${level - 10} ст.)"
    }

    /** XP до следующего уровня (максимум) и текущий прогресс. */
    fun progressFor(stats: UserStatsEntity): Triple<Int, Int, Int> {
        val currentLevel = stats.level
        val xpStart = xpForLevel(currentLevel)
        val xpEnd = xpForLevel(currentLevel + 1)
        val current = stats.totalXp
        val inLevel = current - xpStart
        val levelRange = xpEnd - xpStart
        return Triple(inLevel, levelRange, current)
    }

    /** Сброс дневных счётчиков, если наступил новый день. */
    private fun ensureDailyReset(stats: UserStatsEntity): UserStatsEntity {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (stats.lastResetDate != today) {
            return stats.copy(
                lastResetDate = today,
                dailyXpGained = 0,
                dailyLogins = 0,
                dailyProductsAdded = 0,
                dailyRecipesViewed = 0,
                dailyDishesCooked = 0,
                dailyRecipesCreated = 0,
                dailyMinPurchasesUsed = 0,
                dailyExports = 0
            )
        }
        return stats
    }

    /**
     * Начисляет XP за действие с учётом дневного лимита.
     * Возвращает фактически начисленное XP (0 — если лимит исчерпан).
     */
    suspend fun award(db: AppDatabase, action: String): Int {
        val limitInfo = LIMITS[action] ?: return 0
        val (xpPerAction, maxPerDay) = limitInfo

        var stats = db.userStatsDao().get() ?: UserStatsEntity()
        stats = ensureDailyReset(stats)

        val currentCount = when (action) {
            ACTION_DAILY_LOGIN -> stats.dailyLogins
            ACTION_VIEW_RECIPE -> stats.dailyRecipesViewed
            ACTION_COOKED -> stats.dailyDishesCooked
            ACTION_CREATE_RECIPE -> stats.dailyRecipesCreated
            ACTION_ADD_PRODUCT -> stats.dailyProductsAdded
            ACTION_MIN_PURCHASES -> stats.dailyMinPurchasesUsed
            ACTION_EXPORT -> stats.dailyExports
            else -> 0
        }

        if (currentCount >= maxPerDay) {
            db.userStatsDao().save(stats)
            return 0
        }

        stats = when (action) {
            ACTION_DAILY_LOGIN -> stats.copy(dailyLogins = stats.dailyLogins + 1)
            ACTION_VIEW_RECIPE -> stats.copy(dailyRecipesViewed = stats.dailyRecipesViewed + 1)
            ACTION_COOKED -> stats.copy(
                dailyDishesCooked = stats.dailyDishesCooked + 1
            )
            ACTION_CREATE_RECIPE -> stats.copy(dailyRecipesCreated = stats.dailyRecipesCreated + 1)
            ACTION_ADD_PRODUCT -> stats.copy(dailyProductsAdded = stats.dailyProductsAdded + 1)
            ACTION_MIN_PURCHASES -> stats.copy(
                dailyMinPurchasesUsed = stats.dailyMinPurchasesUsed + 1
            )
            ACTION_EXPORT -> stats.copy(dailyExports = stats.dailyExports + 1)
            else -> stats
        }

        val newTotal = stats.totalXp + xpPerAction
        stats = stats.copy(
            totalXp = newTotal,
            dailyXpGained = stats.dailyXpGained + xpPerAction,
            level = levelForXp(newTotal)
        )
        db.userStatsDao().save(stats)
        return xpPerAction
    }

    /** Максимум XP в день (сумма всех лимитов). */
    fun dailyMaxXp(): Int = LIMITS.values.sumOf { (xp, count) -> xp * count }
}