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

    private var downX = 0f
    private var downY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        // Применяем тему до создания Activity
        val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", true)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            if (isDark) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(300).start()

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

        // Список ID наших вкладок по порядку
        val tabs = listOf(R.id.nav_home, R.id.nav_myrecipes, R.id.nav_add_recipe, R.id.nav_stats, R.id.nav_settings)
        var lastSelectedId = R.id.nav_home

        // Умное переключение с анимацией сдвига
        binding.bottomNav.setOnItemSelectedListener { item ->
            val oldIndex = tabs.indexOf(lastSelectedId)
            val newIndex = tabs.indexOf(item.itemId)
            lastSelectedId = item.itemId

            // Определяем направление анимации (вправо или влево)
            val isForward = newIndex > oldIndex

            val enterAnim = if (isForward) R.anim.slide_in_right else R.anim.slide_in_left
            val exitAnim = if (isForward) R.anim.slide_out_left else R.anim.slide_out_right
            val popEnterAnim = if (isForward) R.anim.slide_in_left else R.anim.slide_in_right
            val popExitAnim = if (isForward) R.anim.slide_out_right else R.anim.slide_out_left

            val navOptions = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(navController.graph.startDestinationId, false)
                .setEnterAnim(enterAnim)
                .setExitAnim(exitAnim)
                .setPopEnterAnim(popEnterAnim)
                .setPopExitAnim(popExitAnim)
                .build()

            navController.navigate(item.itemId, null, navOptions)
            true
        }

        // Слушатель для подсветки (оставляем как было)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isFromMyRecipes = navController.previousBackStackEntry?.destination?.id == R.id.nav_myrecipes
            val menuItemId = when (destination.id) {
                R.id.nav_home -> R.id.nav_home
                R.id.nav_myrecipes -> R.id.nav_myrecipes
                R.id.nav_add_recipe -> R.id.nav_add_recipe
                R.id.nav_stats -> R.id.nav_stats
                R.id.nav_settings -> R.id.nav_settings
                R.id.nav_recipe_detail -> if (isFromMyRecipes) R.id.nav_myrecipes else R.id.nav_home
                else -> -1
            }
            if (menuItemId != -1 && binding.bottomNav.selectedItemId != menuItemId) {
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
        ev?.let {
            when (it.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    downX = it.x
                    downY = it.y
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val deltaX = it.x - downX
                    val deltaY = it.y - downY

                    // Если свайп горизонтальный и достаточно длинный (более 150 пикселей)
                    if (kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) && kotlin.math.abs(deltaX) > 150) {
                        val tabs = listOf(R.id.nav_home, R.id.nav_myrecipes, R.id.nav_add_recipe, R.id.nav_stats, R.id.nav_settings)
                        val current = binding.bottomNav.selectedItemId
                        val index = tabs.indexOf(current)

                        if (index != -1) {
                            if (deltaX > 0 && index > 0) {
                                // Свайп вправо -> предыдущая вкладка
                                binding.bottomNav.selectedItemId = tabs[index - 1]
                            } else if (deltaX < 0 && index < tabs.size - 1) {
                                // Свайп влево -> следующая вкладка
                                binding.bottomNav.selectedItemId = tabs[index + 1]
                            }
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}