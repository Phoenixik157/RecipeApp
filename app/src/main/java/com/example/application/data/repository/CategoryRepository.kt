package com.example.application.data.repository

import com.example.application.data.local.dao.CategoryDao
import com.example.application.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Прослойка между ViewModel и DAO.
 * Задача Repository — скрыть от UI детали источника данных.
 * В будущем здесь можно добавить кеш, сеть и т.д.
 */
class CategoryRepository(private val categoryDao: CategoryDao) {

    fun getAll(): Flow<List<CategoryEntity>> = categoryDao.getAll()

    suspend fun getById(id: Long): CategoryEntity? = categoryDao.getById(id)

    suspend fun count(): Int = categoryDao.count()
}