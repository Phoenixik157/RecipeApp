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
            CategoryEntity(name = "Завтраки", sortOrder = 1),
            CategoryEntity(name = "Супы", sortOrder = 2),
            CategoryEntity(name = "Салаты", sortOrder = 3),
            CategoryEntity(name = "Основные", sortOrder = 4),
            CategoryEntity(name = "Десерты", sortOrder = 5)
        )
        db.categoryDao().insertAll(categories)
    }

    private suspend fun seedIngredients(db: AppDatabase) {
        if (db.ingredientDao().count() > 50) return // Если база уже заполнена, пропускаем

        val ingredients = listOf(
            // ОВОЩИ
            IngredientEntity(name = "лук", displayName = "Лук", synonyms = "луковица", category = "vegetables"),
            IngredientEntity(name = "морковь", displayName = "Морковь", synonyms = "морковка", category = "vegetables"),
            IngredientEntity(name = "картофель", displayName = "Картофель", synonyms = "картошка", category = "vegetables"),
            IngredientEntity(name = "помидор", displayName = "Помидор", synonyms = "томат|помидоры", category = "vegetables"),
            IngredientEntity(name = "огурец", displayName = "Огурец", synonyms = "огурцы", category = "vegetables"),
            IngredientEntity(name = "чеснок", displayName = "Чеснок", synonyms = "", category = "vegetables"),
            IngredientEntity(name = "капуста", displayName = "Капуста", synonyms = "капуста белокочанная", category = "vegetables"),
            IngredientEntity(name = "перец болгарский", displayName = "Перец болгарский", synonyms = "перец сладкий", category = "vegetables"),
            IngredientEntity(name = "свекла", displayName = "Свекла", synonyms = "свёкла", category = "vegetables"),
            IngredientEntity(name = "тыква", displayName = "Тыква", synonyms = "", category = "vegetables"),
            IngredientEntity(name = "кабачок", displayName = "Кабачок", synonyms = "цукини", category = "vegetables"),
            IngredientEntity(name = "брокколи", displayName = "Брокколи", synonyms = "", category = "vegetables"),

            // ФРУКТЫ И ЯГОДЫ
            IngredientEntity(name = "яблоко", displayName = "Яблоко", synonyms = "яблоки", category = "fruits"),
            IngredientEntity(name = "банан", displayName = "Банан", synonyms = "бананы", category = "fruits"),
            IngredientEntity(name = "апельсин", displayName = "Апельсин", synonyms = "апельсины", category = "fruits"),
            IngredientEntity(name = "лимон", displayName = "Лимон", synonyms = "лимоны", category = "fruits"),
            IngredientEntity(name = "груша", displayName = "Груша", synonyms = "груши", category = "fruits"),
            IngredientEntity(name = "виноград", displayName = "Виноград", synonyms = "", category = "fruits"),
            IngredientEntity(name = "клубника", displayName = "Клубника", synonyms = "", category = "fruits"),
            IngredientEntity(name = "малина", displayName = "Малина", synonyms = "", category = "fruits"),
            IngredientEntity(name = "черника", displayName = "Черника", synonyms = "", category = "fruits"),

            // МЯСО И ПТИЦА
            IngredientEntity(name = "курица", displayName = "Курица", synonyms = "куриное филе|курятина", category = "meat"),
            IngredientEntity(name = "говядина", displayName = "Говядина", synonyms = "мясо", category = "meat"),
            IngredientEntity(name = "свинина", displayName = "Свинина", synonyms = "", category = "meat"),
            IngredientEntity(name = "индейка", displayName = "Индейка", synonyms = "филе индейки", category = "meat"),
            IngredientEntity(name = "фарш", displayName = "Фарш", synonyms = "фарш мясной", category = "meat"),
            IngredientEntity(name = "колбаса", displayName = "Колбаса", synonyms = "сосиски", category = "meat"),

            // РЫБА
            IngredientEntity(name = "лосось", displayName = "Лосось", synonyms = "семга|форель", category = "fish"),
            IngredientEntity(name = "тунец", displayName = "Тунец", synonyms = "тунец консервированный", category = "fish"),
            IngredientEntity(name = "треска", displayName = "Треска", synonyms = "белая рыба", category = "fish"),
            IngredientEntity(name = "креветки", displayName = "Креветки", synonyms = "", category = "fish"),

            // МОЛОЧКА
            IngredientEntity(name = "молоко", displayName = "Молоко", synonyms = "", category = "dairy"),
            IngredientEntity(name = "яйца", displayName = "Яйца", synonyms = "яйцо", category = "dairy"),
            IngredientEntity(name = "сыр", displayName = "Сыр", synonyms = "сыр твердый", category = "dairy"),
            IngredientEntity(name = "сливки", displayName = "Сливки", synonyms = "", category = "dairy"),
            IngredientEntity(name = "сметана", displayName = "Сметана", synonyms = "", category = "dairy"),
            IngredientEntity(name = "масло сливочное", displayName = "Масло сливочное", synonyms = "сливочное масло", category = "dairy"),
            IngredientEntity(name = "творог", displayName = "Творог", synonyms = "", category = "dairy"),
            IngredientEntity(name = "кефир", displayName = "Кефир", synonyms = "", category = "dairy"),
            IngredientEntity(name = "йогурт", displayName = "Йогурт", synonyms = "греческий йогурт", category = "dairy"),
            IngredientEntity(name = "маскарпоне", displayName = "Маскарпоне", synonyms = "", category = "dairy"),
            IngredientEntity(name = "сыр фета", displayName = "Сыр фета", synonyms = "брынза", category = "dairy"),
            IngredientEntity(name = "творожный сыр", displayName = "Творожный сыр", synonyms = "", category = "dairy"),

            // БАКАЛЕЯ
            IngredientEntity(name = "рис", displayName = "Рис", synonyms = "", category = "grocery"),
            IngredientEntity(name = "макароны", displayName = "Макароны", synonyms = "паста|спагетти", category = "grocery"),
            IngredientEntity(name = "мука", displayName = "Мука", synonyms = "мука пшеничная", category = "grocery"),
            IngredientEntity(name = "сахар", displayName = "Сахар", synonyms = "", category = "grocery"),
            IngredientEntity(name = "соль", displayName = "Соль", synonyms = "", category = "grocery"),
            IngredientEntity(name = "масло растительное", displayName = "Масло растительное", synonyms = "подсолнечное масло|оливковое масло", category = "grocery"),
            IngredientEntity(name = "гречка", displayName = "Гречка", synonyms = "крупа гречневая", category = "grocery"),
            IngredientEntity(name = "овсяные хлопья", displayName = "Овсяные хлопья", synonyms = "геркулес|овсянка", category = "grocery"),
            IngredientEntity(name = "лапша", displayName = "Лапша", synonyms = "", category = "grocery"),
            IngredientEntity(name = "горошек", displayName = "Горошек", synonyms = "горох консервированный", category = "grocery"),
            IngredientEntity(name = "кукуруза", displayName = "Кукуруза", synonyms = "кукуруза консервированная", category = "grocery"),
            IngredientEntity(name = "разрыхлитель", displayName = "Разрыхлитель", synonyms = "сода", category = "grocery"),
            IngredientEntity(name = "томатная паста", displayName = "Томатная паста", synonyms = "паста томатная", category = "grocery"),

            // ХЛЕБ
            IngredientEntity(name = "хлеб", displayName = "Хлеб", synonyms = "батон", category = "bakery"),
            IngredientEntity(name = "сухарики", displayName = "Сухарики", synonyms = "гренки", category = "bakery"),
            IngredientEntity(name = "сухари панировочные", displayName = "Сухари панировочные", synonyms = "панировка", category = "bakery"),
            IngredientEntity(name = "печенье", displayName = "Печенье", synonyms = "", category = "bakery"),
            IngredientEntity(name = "печенье савоярди", displayName = "Печенье савоярди", synonyms = "дамские пальчики", category = "bakery"),

            // ЗЕЛЕНЬ
            IngredientEntity(name = "укроп", displayName = "Укроп", synonyms = "", category = "greens"),
            IngredientEntity(name = "петрушка", displayName = "Петрушка", synonyms = "", category = "greens"),
            IngredientEntity(name = "лук зеленый", displayName = "Лук зеленый", synonyms = "перо лука", category = "greens"),
            IngredientEntity(name = "базилик", displayName = "Базилик", synonyms = "", category = "greens"),
            IngredientEntity(name = "салат романо", displayName = "Салат Романо", synonyms = "листья салата", category = "greens"),

            // СПЕЦИИ
            IngredientEntity(name = "перец черный", displayName = "Перец черный", synonyms = "черный перец", category = "spices"),
            IngredientEntity(name = "паприка", displayName = "Паприка", synonyms = "", category = "spices"),
            IngredientEntity(name = "корица", displayName = "Корица", synonyms = "", category = "spices"),
            IngredientEntity(name = "специи", displayName = "Специи", synonyms = "приправы", category = "spices"),

            // СЛАДОСТИ
            IngredientEntity(name = "мед", displayName = "Мёд", synonyms = "", category = "sweets"),
            IngredientEntity(name = "шоколад темный", displayName = "Шоколад темный", synonyms = "шоколад", category = "sweets"),
            IngredientEntity(name = "какао", displayName = "Какао", synonyms = "какао-порошок", category = "sweets"),
            IngredientEntity(name = "сгущенка", displayName = "Сгущенка", synonyms = "сгущенное молоко", category = "sweets"),

            // ПРОЧЕЕ
            IngredientEntity(name = "вода", displayName = "Вода", synonyms = "вода питьевая", category = "other"),
            IngredientEntity(name = "оливки", displayName = "Оливки", synonyms = "маслины", category = "other"),
            IngredientEntity(name = "шампиньоны", displayName = "Шампиньоны", synonyms = "грибы", category = "other"),
            IngredientEntity(name = "рыба консервированная", displayName = "Рыба консервированная", synonyms = "тунец в масле", category = "other"),
            IngredientEntity(name = "капуста квашеная", displayName = "Капуста квашеная", synonyms = "кислая капуста", category = "other"),
            IngredientEntity(name = "кофе", displayName = "Кофе", synonyms = "эспрессо", category = "other")
        )
        db.ingredientDao().insertAll(ingredients)
    }
}