package com.example.application

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.DatabaseSeeder
import com.example.application.ui.home.CategoryAdapter
import com.example.application.viewmodels.CategoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val viewModel: CategoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Инициализация БД при первом запуске
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            DatabaseSeeder.seed(applicationContext, db)
            withContext(Dispatchers.IO) {
                db.categoryDao().count()
            }
        }

        // RecyclerView + Adapter
        val recycler = findViewById<RecyclerView>(R.id.categoriesRecycler)
        val adapter = CategoryAdapter()
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // Подписываемся на категории из ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { list ->
                    adapter.submitList(list)
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}