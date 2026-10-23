package com.example.application.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.application.data.local.dao.CategoryDao
import com.example.application.data.local.dao.CookingHistoryDao
import com.example.application.data.local.dao.IngredientDao
import com.example.application.data.local.dao.RecipeDao
import com.example.application.data.local.dao.RecipeIngredientDao
import com.example.application.data.local.dao.TagDao
import com.example.application.data.local.dao.UserStatsDao
import com.example.application.data.local.dao.UserProductDao
import com.example.application.data.local.dao.AchievementDao
import com.example.application.data.local.dao.UsageCounterDao
import com.example.application.data.local.entity.CategoryEntity
import com.example.application.data.local.entity.CookingHistoryEntity
import com.example.application.data.local.entity.IngredientEntity
import com.example.application.data.local.entity.RecipeEntity
import com.example.application.data.local.entity.RecipeIngredientEntity
import com.example.application.data.local.entity.RecipeTagCrossRef
import com.example.application.data.local.entity.TagEntity
import com.example.application.data.local.entity.UserStatsEntity
import com.example.application.data.local.entity.UserProductEntity
import com.example.application.data.local.entity.AchievementEntity
import com.example.application.data.local.entity.UsageCounterEntity

@Database(
    entities = [
        CategoryEntity::class,
        RecipeEntity::class,
        IngredientEntity::class,
        RecipeIngredientEntity::class,
        TagEntity::class,
        RecipeTagCrossRef::class,
        UserStatsEntity::class,
        CookingHistoryEntity::class,
        UserProductEntity::class,
        AchievementEntity::class,
        UsageCounterEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun recipeDao(): RecipeDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao
    abstract fun tagDao(): TagDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun cookingHistoryDao(): CookingHistoryDao
    abstract fun userProductDao(): UserProductDao
    abstract fun achievementDao(): AchievementDao
    abstract fun usageCounterDao(): UsageCounterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recipe_app.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}