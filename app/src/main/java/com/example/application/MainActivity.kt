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
        val navController = navHostFragment.navController

        // 1. Стандартная привязка (обрабатывает клики по вкладкам и сохраняет стек)
        binding.bottomNav.setupWithNavController(navController)

        // 2. Слушатель для умной подсветки вкладок
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Проверяем, откуда мы пришли на экран рецепта (с Главной или из Моих рецептов)
            val isFromMyRecipes = navController.previousBackStackEntry?.destination?.id == R.id.nav_myrecipes

            val menuItemId = when (destination.id) {
                R.id.nav_home -> R.id.nav_home
                R.id.nav_myrecipes -> R.id.nav_myrecipes
                R.id.nav_stats -> R.id.nav_stats
                R.id.nav_settings -> R.id.nav_settings

                // Если мы на экране рецепта или добавления, подсвечиваем ту вкладку, откуда пришли
                R.id.nav_recipe_detail -> if (isFromMyRecipes) R.id.nav_myrecipes else R.id.nav_home
                R.id.nav_add_recipe -> R.id.nav_home

                else -> -1
            }

            // Если вкладка определена и она еще не нажата, подсвечиваем её
            if (menuItemId != -1 && binding.bottomNav.selectedItemId != menuItemId) {
                // ВАЖНО: isChecked = true меняет цвет, но НЕ вызывает клик (нет зацикливания)
                binding.bottomNav.menu.findItem(menuItemId)?.isChecked = true
            }
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

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev?.action == android.view.MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is android.widget.EditText) {
                val outRect = android.graphics.Rect()
                view.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    view.clearFocus()
                    val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}