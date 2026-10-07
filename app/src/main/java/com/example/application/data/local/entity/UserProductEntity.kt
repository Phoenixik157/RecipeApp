package com.example.application.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Продукты, которые пользователь отметил как «есть в холодильнике».
 * Одна строка = один продукт. ingredientId — FK на ingredients.
 */
@Entity(
    tableName = "user_products",
    foreignKeys = [
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ingredientId")]
)
data class UserProductEntity(
    @PrimaryKey
    val ingredientId: Long
)