package com.example.application.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recipes",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId"), Index("apiId"), Index("isUserRecipe")]
)
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val imageUrl: String? = null,
    val imagePath: String? = null,
    val cookingTimeMinutes: Int = 0,
    val complexity: Int = 1,
    val baseServings: Int = 2,
    val instructions: String = "",
    val categoryId: Long,
    val isUserRecipe: Boolean = true,
    val apiId: String? = null,
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)