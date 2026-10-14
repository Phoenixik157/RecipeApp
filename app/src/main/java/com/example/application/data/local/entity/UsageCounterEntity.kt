package com.example.application.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_counters")
data class UsageCounterEntity(
    @PrimaryKey val key: String,
    val count: Int = 0
)