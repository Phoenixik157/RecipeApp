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
        val navController = navHostFragment.navController

        binding.bottomNav.setOnItemSelectedListener { item ->
            // Уже на этом экране — просто сбрасываем стек до корня вкладки
            if (navController.currentDestination?.id == item.itemId) {
                navController.popBackStack(item.itemId, inclusive = false)
                true
            } else {
                val options = androidx.navigation.NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(navController.graph.startDestinationId, inclusive = false)
                    .build()
                navController.navigate(item.itemId, null, options)
                true
            }
        }

        // Подсветка активного пункта BottomNav — с учётом вложенных экранов
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val itemId = when (destination.id) {
                R.id.nav_home, R.id.nav_recipe_detail, R.id.nav_add_recipe -> R.id.nav_home
                R.id.nav_myrecipes -> R.id.nav_myrecipes
                R.id.nav_stats -> R.id.nav_stats
                R.id.nav_settings -> R.id.nav_settings
                else -> R.id.nav_home
            }
            binding.bottomNav.selectedItemId = itemId
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}