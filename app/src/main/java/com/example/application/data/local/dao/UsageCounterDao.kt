package com.example.application.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.application.data.local.entity.UsageCounterEntity

@Dao
interface UsageCounterDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(counter: UsageCounterEntity)

    @Query("UPDATE usage_counters SET count = count + 1 WHERE key = :key")
    suspend fun incrementByKey(key: String)

    @Query("SELECT count FROM usage_counters WHERE key = :key")
    suspend fun getCount(key: String): Int?

    @Query("DELETE FROM usage_counters")
    suspend fun clear()

    @Transaction
    suspend fun increment(key: String) {
        insertIfAbsent(UsageCounterEntity(key = key, count = 0))
        incrementByKey(key)
    }
}