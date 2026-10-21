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
        if (db.ingredientDao().count() > 50) return // Если база уже заполнена, пропускаем

        val ingredients = listOf(
            // ОВОЩИ
            IngredientEntity(name = "лук", displayName = "Лук", emoji = "🧅", synonyms = "луковица", category = "vegetables"),
            IngredientEntity(name = "морковь", displayName = "Морковь", emoji = "🥕", synonyms = "морковка", category = "vegetables"),
            IngredientEntity(name = "картофель", displayName = "Картофель", emoji = "🥔", synonyms = "картошка", category = "vegetables"),
            IngredientEntity(name = "помидор", displayName = "Помидор", emoji = "🍅", synonyms = "томат|помидоры", category = "vegetables"),
            IngredientEntity(name = "огурец", displayName = "Огурец", emoji = "🥒", synonyms = "огурцы", category = "vegetables"),
            IngredientEntity(name = "чеснок", displayName = "Чеснок", emoji = "🧄", synonyms = "", category = "vegetables"),
            IngredientEntity(name = "капуста", displayName = "Капуста", emoji = "🥬", synonyms = "капуста белокочанная", category = "vegetables"),
            IngredientEntity(name = "перец болгарский", displayName = "Перец болгарский", emoji = "🫑", synonyms = "перец сладкий", category = "vegetables"),
            IngredientEntity(name = "свекла", displayName = "Свекла", emoji = "🍠", synonyms = "свёкла", category = "vegetables"),
            IngredientEntity(name = "тыква", displayName = "Тыква", emoji = "🎃", synonyms = "", category = "vegetables"),
            IngredientEntity(name = "кабачок", displayName = "Кабачок", emoji = "🥒", synonyms = "цукини", category = "vegetables"),
            IngredientEntity(name = "брокколи", displayName = "Брокколи", emoji = "🥦", synonyms = "", category = "vegetables"),

            // ФРУКТЫ И ЯГОДЫ
            IngredientEntity(name = "яблоко", displayName = "Яблоко", emoji = "🍎", synonyms = "яблоки", category = "fruits"),
            IngredientEntity(name = "банан", displayName = "Банан", emoji = "🍌", synonyms = "бананы", category = "fruits"),
            IngredientEntity(name = "апельсин", displayName = "Апельсин", emoji = "🍊", synonyms = "апельсины", category = "fruits"),
            IngredientEntity(name = "лимон", displayName = "Лимон", emoji = "🍋", synonyms = "лимоны", category = "fruits"),
            IngredientEntity(name = "груша", displayName = "Груша", emoji = "🍐", synonyms = "груши", category = "fruits"),
            IngredientEntity(name = "виноград", displayName = "Виноград", emoji = "🍇", synonyms = "", category = "fruits"),
            IngredientEntity(name = "клубника", displayName = "Клубника", emoji = "🍓", synonyms = "", category = "fruits"),
            IngredientEntity(name = "малина", displayName = "Малина", emoji = "🍓", synonyms = "", category = "fruits"),
            IngredientEntity(name = "черника", displayName = "Черника", emoji = "🫐", synonyms = "", category = "fruits"),

            // МЯСО И ПТИЦА
            IngredientEntity(name = "курица", displayName = "Курица", emoji = "🍗", synonyms = "куриное филе|курятина", category = "meat"),
            IngredientEntity(name = "говядина", displayName = "Говядина", emoji = "🥩", synonyms = "мясо", category = "meat"),
            IngredientEntity(name = "свинина", displayName = "Свинина", emoji = "🥓", synonyms = "", category = "meat"),
            IngredientEntity(name = "индейка", displayName = "Индейка", emoji = "🦃", synonyms = "филе индейки", category = "meat"),
            IngredientEntity(name = "фарш", displayName = "Фарш", emoji = "🥩", synonyms = "фарш мясной", category = "meat"),
            IngredientEntity(name = "колбаса", displayName = "Колбаса", emoji = "🌭", synonyms = "сосиски", category = "meat"),

            // РЫБА
            IngredientEntity(name = "лосось", displayName = "Лосось", emoji = "🐟", synonyms = "семга|форель", category = "fish"),
            IngredientEntity(name = "тунец", displayName = "Тунец", emoji = "🐟", synonyms = "тунец консервированный", category = "fish"),
            IngredientEntity(name = "треска", displayName = "Треска", emoji = "🐟", synonyms = "белая рыба", category = "fish"),
            IngredientEntity(name = "креветки", displayName = "Креветки", emoji = "🦐", synonyms = "", category = "fish"),

            // МОЛОЧКА
            IngredientEntity(name = "молоко", displayName = "Молоко", emoji = "🥛", synonyms = "", category = "dairy"),
            IngredientEntity(name = "яйца", displayName = "Яйца", emoji = "🥚", synonyms = "яйцо", category = "dairy"),
            IngredientEntity(name = "сыр", displayName = "Сыр", emoji = "🧀", synonyms = "сыр твердый", category = "dairy"),
            IngredientEntity(name = "сливки", displayName = "Сливки", emoji = "🥛", synonyms = "", category = "dairy"),
            IngredientEntity(name = "сметана", displayName = "Сметана", emoji = "🥛", synonyms = "", category = "dairy"),
            IngredientEntity(name = "масло сливочное", displayName = "Масло сливочное", emoji = "🧈", synonyms = "сливочное масло", category = "dairy"),
            IngredientEntity(name = "творог", displayName = "Творог", emoji = "🥛", synonyms = "", category = "dairy"),
            IngredientEntity(name = "кефир", displayName = "Кефир", emoji = "🥛", synonyms = "", category = "dairy"),
            IngredientEntity(name = "йогурт", displayName = "Йогурт", emoji = "🥛", synonyms = "греческий йогурт", category = "dairy"),
            IngredientEntity(name = "маскарпоне", displayName = "Маскарпоне", emoji = "🧀", synonyms = "", category = "dairy"),
            IngredientEntity(name = "сыр фета", displayName = "Сыр фета", emoji = "🧀", synonyms = "брынза", category = "dairy"),
            IngredientEntity(name = "творожный сыр", displayName = "Творожный сыр", emoji = "🧀", synonyms = "", category = "dairy"),

            // БАКАЛЕЯ
            IngredientEntity(name = "рис", displayName = "Рис", emoji = "🍚", synonyms = "", category = "grocery"),
            IngredientEntity(name = "макароны", displayName = "Макароны", emoji = "🍝", synonyms = "паста|спагетти", category = "grocery"),
            IngredientEntity(name = "мука", displayName = "Мука", emoji = "🌾", synonyms = "мука пшеничная", category = "grocery"),
            IngredientEntity(name = "сахар", displayName = "Сахар", emoji = "🍬", synonyms = "", category = "grocery"),
            IngredientEntity(name = "соль", displayName = "Соль", emoji = "🧂", synonyms = "", category = "grocery"),
            IngredientEntity(name = "масло растительное", displayName = "Масло растительное", emoji = "🫒", synonyms = "подсолнечное масло|оливковое масло", category = "grocery"),
            IngredientEntity(name = "гречка", displayName = "Гречка", emoji = "🌾", synonyms = "крупа гречневая", category = "grocery"),
            IngredientEntity(name = "овсяные хлопья", displayName = "Овсяные хлопья", emoji = "🌾", synonyms = "геркулес|овсянка", category = "grocery"),
            IngredientEntity(name = "лапша", displayName = "Лапша", emoji = "🍜", synonyms = "", category = "grocery"),
            IngredientEntity(name = "горошек", displayName = "Горошек", emoji = "🟢", synonyms = "горох консервированный", category = "grocery"),
            IngredientEntity(name = "кукуруза", displayName = "Кукуруза", emoji = "🌽", synonyms = "кукуруза консервированная", category = "grocery"),
            IngredientEntity(name = "разрыхлитель", displayName = "Разрыхлитель", emoji = "🥄", synonyms = "сода", category = "grocery"),
            IngredientEntity(name = "томатная паста", displayName = "Томатная паста", emoji = "🥫", synonyms = "паста томатная", category = "grocery"),

            // ХЛЕБ
            IngredientEntity(name = "хлеб", displayName = "Хлеб", emoji = "🍞", synonyms = "батон", category = "bakery"),
            IngredientEntity(name = "сухарики", displayName = "Сухарики", emoji = "🍞", synonyms = "гренки", category = "bakery"),
            IngredientEntity(name = "сухари панировочные", displayName = "Сухари панировочные", emoji = "🍞", synonyms = "панировка", category = "bakery"),
            IngredientEntity(name = "печенье", displayName = "Печенье", emoji = "🍪", synonyms = "", category = "bakery"),
            IngredientEntity(name = "печенье савоярди", displayName = "Печенье савоярди", emoji = "🍪", synonyms = "дамские пальчики", category = "bakery"),

            // ЗЕЛЕНЬ
            IngredientEntity(name = "укроп", displayName = "Укроп", emoji = "🌿", synonyms = "", category = "greens"),
            IngredientEntity(name = "петрушка", displayName = "Петрушка", emoji = "🌿", synonyms = "", category = "greens"),
            IngredientEntity(name = "лук зеленый", displayName = "Лук зеленый", emoji = "🌱", synonyms = "перо лука", category = "greens"),
            IngredientEntity(name = "базилик", displayName = "Базилик", emoji = "🌿", synonyms = "", category = "greens"),
            IngredientEntity(name = "салат романо", displayName = "Салат Романо", emoji = "🥬", synonyms = "листья салата", category = "greens"),

            // СПЕЦИИ
            IngredientEntity(name = "перец черный", displayName = "Перец черный", emoji = "🧂", synonyms = "черный перец", category = "spices"),
            IngredientEntity(name = "паприка", displayName = "Паприка", emoji = "🌶️", synonyms = "", category = "spices"),
            IngredientEntity(name = "корица", displayName = "Корица", emoji = "🥄", synonyms = "", category = "spices"),
            IngredientEntity(name = "специи", displayName = "Специи", emoji = "🧂", synonyms = "приправы", category = "spices"),

            // СЛАДОСТИ
            IngredientEntity(name = "мед", displayName = "Мёд", emoji = "🍯", synonyms = "", category = "sweets"),
            IngredientEntity(name = "шоколад темный", displayName = "Шоколад темный", emoji = "🍫", synonyms = "шоколад", category = "sweets"),
            IngredientEntity(name = "какао", displayName = "Какао", emoji = "🍫", synonyms = "какао-порошок", category = "sweets"),
            IngredientEntity(name = "сгущенка", displayName = "Сгущенка", emoji = "🥛", synonyms = "сгущенное молоко", category = "sweets"),

            // ПРОЧЕЕ
            IngredientEntity(name = "вода", displayName = "Вода", emoji = "💧", synonyms = "вода питьевая", category = "other"),
            IngredientEntity(name = "оливки", displayName = "Оливки", emoji = "🫒", synonyms = "маслины", category = "other"),
            IngredientEntity(name = "шампиньоны", displayName = "Шампиньоны", emoji = "🍄", synonyms = "грибы", category = "other"),
            IngredientEntity(name = "рыба консервированная", displayName = "Рыба консервированная", emoji = "🥫", synonyms = "тунец в масле", category = "other"),
            IngredientEntity(name = "капуста квашеная", displayName = "Капуста квашеная", emoji = "🥬", synonyms = "кислая капуста", category = "other"),
            IngredientEntity(name = "кофе", displayName = "Кофе", emoji = "☕", synonyms = "эспрессо", category = "other")
        )
        db.ingredientDao().insertAll(ingredients)
    }
}