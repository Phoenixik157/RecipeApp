package com.example.application.data.local

import android.content.Context
import com.example.application.data.local.entity.CategoryEntity
import com.example.application.data.local.entity.IngredientEntity

object DatabaseSeeder {

    suspend fun seed(context: Context, db: AppDatabase) {
        seedCategories(db)
        seedIngredients(db)
    }

    private suspend fun seedCategories(db: AppDatabase) {
        if (db.categoryDao().count() > 0) return

        val categories = listOf(
            CategoryEntity(name = "Завтраки", emoji = "🍳", sortOrder = 1),
            CategoryEntity(name = "Супы", emoji = "🍲", sortOrder = 2),
            CategoryEntity(name = "Салаты", emoji = "🥗", sortOrder = 3),
            CategoryEntity(name = "Основные", emoji = "🍝", sortOrder = 4),
            CategoryEntity(name = "Десерты", emoji = "🍰", sortOrder = 5)
        )
        db.categoryDao().insertAll(categories)
    }

    private suspend fun seedIngredients(db: AppDatabase) {
        if (db.ingredientDao().count() > 0) return

        val ingredients = listOf(
            IngredientEntity(name = "лук", displayName = "Лук репчатый", emoji = "🧅",
                synonyms = "лук репчатый|луковица|onion", category = "vegetables"),
            IngredientEntity(name = "морковь", displayName = "Морковь", emoji = "🥕",
                synonyms = "морковка|carrot", category = "vegetables"),
            IngredientEntity(name = "картофель", displayName = "Картофель", emoji = "🥔",
                synonyms = "картошка|potato", category = "vegetables"),
            IngredientEntity(name = "помидор", displayName = "Помидор", emoji = "🍅",
                synonyms = "томат|помидоры|tomato", category = "vegetables"),
            IngredientEntity(name = "огурец", displayName = "Огурец", emoji = "🥒",
                synonyms = "огурцы|cucumber", category = "vegetables"),
            IngredientEntity(name = "чеснок", displayName = "Чеснок", emoji = "🧄",
                synonyms = "garlic", category = "vegetables"),
            IngredientEntity(name = "капуста", displayName = "Капуста", emoji = "🥬",
                synonyms = "cabbage", category = "vegetables"),
            IngredientEntity(name = "курица", displayName = "Курица", emoji = "🍗",
                synonyms = "куриное филе|курятина|chicken", category = "meat"),
            IngredientEntity(name = "говядина", displayName = "Говядина", emoji = "🥩",
                synonyms = "beef", category = "meat"),
            IngredientEntity(name = "свинина", displayName = "Свинина", emoji = "🥓",
                synonyms = "pork", category = "meat"),
            IngredientEntity(name = "лосось", displayName = "Лосось", emoji = "🐟",
                synonyms = "семга|salmon", category = "meat"),
            IngredientEntity(name = "молоко", displayName = "Молоко", emoji = "🥛",
                synonyms = "milk", category = "dairy"),
            IngredientEntity(name = "яйца", displayName = "Яйца", emoji = "🥚",
                synonyms = "яйцо|egg|eggs", category = "dairy"),
            IngredientEntity(name = "сыр", displayName = "Сыр", emoji = "🧀",
                synonyms = "cheese", category = "dairy"),
            IngredientEntity(name = "сливки", displayName = "Сливки", emoji = "🥛",
                synonyms = "cream", category = "dairy"),
            IngredientEntity(name = "сметана", displayName = "Сметана", emoji = "🥛",
                synonyms = "sour cream", category = "dairy"),
            IngredientEntity(name = "масло сливочное", displayName = "Масло сливочное", emoji = "🧈",
                synonyms = "butter", category = "dairy"),
            IngredientEntity(name = "рис", displayName = "Рис", emoji = "🍚",
                synonyms = "rice", category = "grocery"),
            IngredientEntity(name = "макароны", displayName = "Макароны", emoji = "🍝",
                synonyms = "паста|спагетти|pasta", category = "grocery"),
            IngredientEntity(name = "мука", displayName = "Мука", emoji = "🌾",
                synonyms = "flour", category = "grocery"),
            IngredientEntity(name = "сахар", displayName = "Сахар", emoji = "🍬",
                synonyms = "sugar", category = "grocery"),
            IngredientEntity(name = "соль", displayName = "Соль", emoji = "🧂",
                synonyms = "salt", category = "grocery"),
            IngredientEntity(name = "масло растительное", displayName = "Масло растительное", emoji = "🫒",
                synonyms = "подсолнечное масло|oil", category = "grocery")
        )
        db.ingredientDao().insertAll(ingredients)
    }
}