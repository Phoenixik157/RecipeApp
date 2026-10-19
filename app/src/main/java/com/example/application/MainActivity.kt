package com.example.application

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.DatabaseSeeder
import com.example.application.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.application.data.local.RecipeJsonImporter
import com.example.application.utils.XpManager
import android.net.Uri
import android.content.Intent
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация БД и первичная загрузка рецептов
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            DatabaseSeeder.seed(applicationContext, db)
            XpManager.award(db, XpManager.ACTION_DAILY_LOGIN)
            withContext(Dispatchers.IO) {
                RecipeJsonImporter.importFromJson(applicationContext, db)
            }
        }

        // Навигация
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHostFragment.navController)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Обработка входящей ссылки (Deep Link)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent != null) handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data: android.net.Uri? = intent?.data
        if (data != null) {
            // Берем ID из ссылки
            val recipeId = data.getQueryParameter("id")?.toLongOrNull()
            if (recipeId != null) {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as androidx.navigation.fragment.NavHostFragment
                val bundle = Bundle().apply { putLong("recipeId", recipeId) }
                navHostFragment.navController.navigate(R.id.nav_recipe_detail, bundle)
            }
        }
    }
}