package com.example.application

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.DatabaseSeeder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Создаём БД и наполняем её данными при первом запуске
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)

            // 1. Наполняем стартовыми данными (сработает один раз)
            DatabaseSeeder.seed(applicationContext, db)

            // 2. Реальный запрос — заставляет Room создать файл recipe_app.db
            withContext(Dispatchers.IO) {
                db.categoryDao().count()
                db.ingredientDao().count()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}