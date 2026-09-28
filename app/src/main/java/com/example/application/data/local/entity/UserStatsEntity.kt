package com.example.application.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey
    val id: Int = 1,
    val totalXp: Int = 0,
    val level: Int = 1,
    val dishesCooked: Int = 0,
    val dailyXpGained: Int = 0,
    val lastResetDate: String = "",
    val dailyLogins: Int = 0,
    val dailyProductsAdded: Int = 0,
    val dailyRecipesViewed: Int = 0,
    val dailyDishesCooked: Int = 0,
    val dailyRecipesCreated: Int = 0,
    val dailyMinPurchasesUsed: Int = 0,
    val dailyExports: Int = 0,
    val unlockedAchievements: String = ""
)