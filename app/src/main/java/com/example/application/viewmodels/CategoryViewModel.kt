package com.example.application.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.CategoryEntity
import com.example.application.data.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel хранит состояние экрана и переживает поворот экрана.
 * Данные не теряются при пересоздании Activity.
 */
class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CategoryRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = CategoryRepository(db.categoryDao())
    }

    /**
     * StateFlow — «горячий» поток. UI подписывается и получает обновления.
     * Если БД изменится — список автоматически обновится.
     */
    val categories: StateFlow<List<CategoryEntity>> = repository
        .getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}