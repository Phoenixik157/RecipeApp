package com.example.application.data.remote.api

import com.example.application.data.remote.dto.MealResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MealApiService {

    @GET("search.php")
    suspend fun searchByName(@Query("s") query: String): MealResponse

    @GET("lookup.php")
    suspend fun lookupById(@Query("i") id: String): MealResponse

    @GET("random.php")
    suspend fun getRandom(): MealResponse
}