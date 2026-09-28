package com.example.application.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingredients",
    indices = [Index(value = ["name"], unique = true)]
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val displayName: String,
    val emoji: String = "🥕",
    val synonyms: String = "",
    val category: String = "other"
)