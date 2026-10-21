package com.example.application.utils

object TextUtils {
    // Делает первую букву заглавной, остальные маленькими
    fun capitalize(text: String): String {
        if (text.isEmpty()) return text
        return text.substring(0, 1).uppercase() + text.substring(1).lowercase()
    }
}