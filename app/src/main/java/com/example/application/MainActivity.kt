package com.example.application

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.DatabaseSeeder
import com.example.application.data.local.RecipeJsonImporter
import com.example.application.databinding.ActivityMainBinding
import com.example.application.utils.XpManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var downX = 0f
    private var downY = 0f
    private var isTouchOnScrollable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Применяем тему
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

        // Инициализация БД
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            DatabaseSeeder.seed(applicationContext, db)
            XpManager.award(db, XpManager.ACTION_DAILY_LOGIN)
            withContext(Dispatchers.IO) {
                RecipeJsonImporter.importFromJson(applicationContext, db)
            }
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        val tabs = listOf(R.id.nav_home, R.id.nav_myrecipes, R.id.nav_add_recipe, R.id.nav_stats, R.id.nav_settings)
        var lastSelectedId = R.id.nav_home

        binding.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == lastSelectedId && item.itemId != R.id.nav_home) return@setOnItemSelectedListener true

            val oldIndex = tabs.indexOf(lastSelectedId)
            val newIndex = tabs.indexOf(item.itemId)
            lastSelectedId = item.itemId

            val isForward = newIndex > oldIndex

            val enterAnim = if (isForward) R.anim.slide_in_right else R.anim.slide_in_left
            val exitAnim = if (isForward) R.anim.slide_out_left else R.anim.slide_out_right
            val popEnterAnim = if (isForward) R.anim.slide_in_left else R.anim.slide_in_right
            val popExitAnim = if (isForward) R.anim.slide_out_right else R.anim.slide_out_left

            val inclusive = item.itemId == R.id.nav_home

            val navOptions = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(navController.graph.startDestinationId, inclusive)
                .setEnterAnim(enterAnim)
                .setExitAnim(exitAnim)
                .setPopEnterAnim(popEnterAnim)
                .setPopExitAnim(popExitAnim)
                .build()

            navController.navigate(item.itemId, null, navOptions)
            true
        }

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
            if (menuItemId != -1) {
                lastSelectedId = menuItemId
                if (binding.bottomNav.selectedItemId != menuItemId) {
                    binding.bottomNav.menu.findItem(menuItemId)?.isChecked = true
                }
            }
        }

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
            val recipeId = data.getQueryParameter("id")?.toLongOrNull()
            if (recipeId != null) {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
                val bundle = Bundle().apply { putLong("recipeId", recipeId) }
                navHostFragment.navController.navigate(R.id.nav_recipe_detail, bundle)
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.x
                    downY = ev.y
                    isTouchOnScrollable = isTouchOnScrollable(window.decorView as ViewGroup, ev.rawX, ev.rawY)

                    // СКРЫТИЕ КЛАВИАТУРЫ ПРИ КЛИКЕ ВНЕ EDITEXT
                    val focusView = currentFocus
                    if (focusView is EditText) {
                        val rect = Rect()
                        focusView.getGlobalVisibleRect(rect)
                        if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                            focusView.clearFocus()
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(focusView.windowToken, 0)
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!isTouchOnScrollable) {
                        val deltaX = ev.x - downX
                        val deltaY = ev.y - downY

                        if (abs(deltaX) > abs(deltaY) && abs(deltaX) > 80) {
                            val tabs = listOf(R.id.nav_home, R.id.nav_myrecipes, R.id.nav_add_recipe, R.id.nav_stats, R.id.nav_settings)
                            val current = binding.bottomNav.selectedItemId
                            val index = tabs.indexOf(current)

                            if (index != -1) {
                                if (deltaX > 0 && index > 0) {
                                    binding.bottomNav.selectedItemId = tabs[index - 1]
                                } else if (deltaX < 0 && index < tabs.size - 1) {
                                    binding.bottomNav.selectedItemId = tabs[index + 1]
                                }
                            }
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun isTouchOnScrollable(parent: ViewGroup, rawX: Float, rawY: Float): Boolean {
        val rect = Rect()
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i) ?: continue
            if (child.visibility != View.VISIBLE) continue

            child.getGlobalVisibleRect(rect)
            if (rect.contains(rawX.toInt(), rawY.toInt())) {
                if (child is HorizontalScrollView) return true
                if (child is SeekBar) return true
                if (child is EditText) return true
                if (child is ViewGroup && isTouchOnScrollable(child, rawX, rawY)) return true
            }
        }
        return false
    }
}