package com.example.application.utils

object ReplacementManager {
    // Словарь: Ключ (что ищем) -> Список возможных замен (в порядке приоритета)
    private val replacements = mapOf(
        "сливки" to listOf("сметана", "молоко"),
        "сметана" to listOf("сливки", "йогурт"),
        "масло сливочное" to listOf("масло растительное", "маргарин"),
        "масло растительное" to listOf("масло сливочное", "оливковое масло"),
        "курица" to listOf("индейка", "свинина"),
        "говядина" to listOf("свинина", "курица"),
        "помидор" to listOf("томатная паста", "перец болгарский"),
        "лук" to listOf("лук зеленый", "чеснок"),
        "сыр" to listOf("творог", "брынза"),
        "молоко" to listOf("вода", "сливки", "кефир")
    )

    /**
     * Ищет замену для отсутствующего ингредиента.
     * @param missingIngredient название того, чего нет
     * @param availableIngredients список названий того, что ЕСТЬ в холодильнике
     * @return название подходящей замены или null, если замены нет
     */
    fun findReplacement(missingIngredient: String, availableIngredients: Set<String>): String? {
        val missingLower = missingIngredient.lowercase().trim()

        // Ищем в справочнике
        val possibleSubstitutes = replacements[missingLower] ?: return null

        // Проверяем, есть ли у пользователя что-то из списка замен
        for (substitute in possibleSubstitutes) {
            if (availableIngredients.contains(substitute.lowercase().trim())) {
                return substitute // Нашли замену!
            }
        }
        return null
    }
}