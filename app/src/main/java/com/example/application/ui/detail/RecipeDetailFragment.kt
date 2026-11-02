package com.example.application.ui.detail

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.application.R
import com.example.application.utils.TextUtils
import com.example.application.viewmodels.RecipeDetailViewModel
import kotlinx.coroutines.launch

class RecipeDetailFragment : Fragment() {

    private val viewModel: RecipeDetailViewModel by viewModels()
    private var isUpdatingServings = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_recipe_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recipeId = arguments?.getLong("recipeId") ?: return
        viewModel.load(recipeId)

        val servingsEditText = view.findViewById<EditText>(R.id.servingsValue)
        val servingsSeekBar = view.findViewById<SeekBar>(R.id.servingsSeekBar)

        view.findViewById<View>(R.id.backButton).setOnClickListener { findNavController().navigateUp() }

        // --- НАСТРОЙКА ПОРЦИЙ ---
        fun updateServingsUI(newValue: Int) {
            if (isUpdatingServings) return
            isUpdatingServings = true
            val clamped = newValue.coerceIn(1, 100)
            servingsEditText.setText(clamped.toString())
            servingsSeekBar.progress = clamped - 1
            isUpdatingServings = false
        }

        view.findViewById<View>(R.id.btnMinus).setOnClickListener { viewModel.decreaseServings() }
        view.findViewById<View>(R.id.btnPlus).setOnClickListener { viewModel.increaseServings() }

        servingsEditText.doOnTextChanged { text, _, _, _ ->
            if (isUpdatingServings) return@doOnTextChanged
            val parsed = text?.toString()?.toIntOrNull()
            if (parsed != null && parsed in 1..100) {
                viewModel.setServings(parsed)
            }
        }

        servingsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isUpdatingServings) {
                    viewModel.setServings(progress + 1)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        view.findViewById<View>(R.id.btnShare).setOnClickListener {
            val recipe = viewModel.recipe.value ?: return@setOnClickListener
            QrDialog.show(requireContext(), recipe)
        }

        view.findViewById<View>(R.id.btnCooked).setOnClickListener {
            val recipe = viewModel.recipe.value ?: return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                val xp = viewModel.awardCooked(recipe.id, viewModel.servings.value)
                val msg = if (xp > 0) "Приготовлено! +$xp XP" else "Записано. Лимит XP на сегодня исчерпан"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recipe.collect { recipe ->
                        recipe ?: return@collect
                        view.findViewById<TextView>(R.id.titleText).text = recipe.title
                        view.findViewById<TextView>(R.id.timeChip).text = "${recipe.cookingTimeMinutes} мин"
                        view.findViewById<TextView>(R.id.difficultyChip).text = starsFor(recipe.complexity)
                        view.findViewById<TextView>(R.id.instructionsText).text = recipe.instructions

                        if (!recipe.imageUrl.isNullOrEmpty()) {
                            view.findViewById<ImageView>(R.id.heroImage).load(recipe.imageUrl)
                        }
                    }
                }

                launch {
                    viewModel.categoryName.collect { catName ->
                        view.findViewById<TextView>(R.id.categoryText).text = catName
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.servings.collect { s ->
                    updateServingsUI(s)
                    rebuildIngredients(view, s)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ingredients.collect {
                    rebuildIngredients(view, viewModel.servings.value)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                com.example.application.utils.AchievementManager.newlyUnlocked.collect { list ->
                    list.forEach { id ->
                        val title = com.example.application.utils.AchievementManager.TITLES[id] ?: id
                        Toast.makeText(requireContext(), "🏆 Достижение: $title", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        viewModel.awardView()
    }

    private fun rebuildIngredients(view: View, servings: Int) {
        val container = view.findViewById<LinearLayout>(R.id.ingredientsList)
        container.removeAllViews()
        val list = viewModel.ingredients.value
        if (list.isEmpty()) return

        val baseServings = viewModel.recipe.value?.baseServings ?: servings
        val ratio = servings.toDouble() / baseServings.toDouble()

        list.forEachIndexed { idx, item ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(40, 40, 40, 40)
            }

            // Иконка категории (ТЕПЕРЬ БЕЗ ПРИНУДИТЕЛЬНОГО БЕЛОГО ЦВЕТА)
            val catColor = getCategoryColor(item.ingredient.category)
            val catIcon = getCategoryIcon(item.ingredient.category)

            val iconContainer = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(48, 48)
                gravity = Gravity.CENTER
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(catColor)
                }
                background = bg
            }

            val iconView = ImageView(requireContext()).apply {
                setImageResource(catIcon)
                // setColorFilter(Color.WHITE) <--- УДАЛИЛИ ЭТУ СТРОКУ
                layoutParams = LinearLayout.LayoutParams(28, 28) // Чуть увеличили для читаемости
            }
            iconContainer.addView(iconView)

            val name = TextView(requireContext()).apply {
                text = TextUtils.capitalize(item.ingredient.displayName)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                textSize = 14f
                setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.inter_regular))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(24, 0, 0, 0)
            }

            val qty = (item.quantity * ratio).let {
                if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it)
            }

            val measure = TextView(requireContext()).apply {
                text = if (item.quantity > 0) "$qty ${item.unit}" else item.unit
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                textSize = 13f
                setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.inter_regular))
                setPadding(0, 0, 20, 0)
            }

            // Статус: есть / нет / замена (УВЕЛИЧИЛИ РАЗМЕРЫ ИКОНОК)
            val statusContainer = LinearLayout(requireContext()).apply {
                gravity = Gravity.CENTER_VERTICAL
                orientation = LinearLayout.HORIZONTAL
            }

            if (item.haveIt) {
                val checkIcon = ImageView(requireContext()).apply {
                    setImageResource(R.drawable.ic_check_circle)
                    setColorFilter(Color.parseColor("#4CAF50")) // Зеленый
                    layoutParams = LinearLayout.LayoutParams(45, 45) // Увеличили с 24 до 32
                }
                statusContainer.addView(checkIcon)
            } else if (item.replacement != null) {
                val syncIcon = ImageView(requireContext()).apply {
                    setImageResource(R.drawable.ic_status_sync)
                    setColorFilter(Color.parseColor("#FFA000")) // Оранжевый
                    layoutParams = LinearLayout.LayoutParams(41, 41) // Увеличили с 20 до 28
                }
                val replaceText = TextView(requireContext()).apply {
                    text = " ${item.replacement}"
                    setTextColor(Color.parseColor("#FFA000"))
                    textSize = 14f // Чуть увеличили текст
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(8, 0, 0, 0)
                }
                statusContainer.addView(syncIcon)
                statusContainer.addView(replaceText)
            } else {
                val crossIcon = ImageView(requireContext()).apply {
                    setImageResource(R.drawable.ic_status_cross)
                    setColorFilter(Color.parseColor("#F44336")) // Красный
                    layoutParams = LinearLayout.LayoutParams(45, 45)
                }
                statusContainer.addView(crossIcon)
            }

            row.addView(iconContainer)
            row.addView(name)
            row.addView(measure)
            row.addView(statusContainer)
            container.addView(row)

            if (idx < list.size - 1) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 2
                    )
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_dark))
                }
                container.addView(divider)
            }
        }
    }

    private fun starsFor(complexity: Int): String = when (complexity) {
        1 -> "★☆☆"
        2 -> "★★☆"
        else -> "★★★"
    }

    // Вспомогательные функции
    private fun getCategoryIcon(category: String): Int {
        val c = category.lowercase()
        return when {
            c.contains("veg") || c.contains("овощ") -> R.drawable.ic_cat_vegetables
            c.contains("fru") || c.contains("фрукт") || c.contains("ягод") -> R.drawable.ic_cat_fruits
            c.contains("meat") || c.contains("мясо") || c.contains("птиц") -> R.drawable.ic_cat_meat
            c.contains("fish") || c.contains("рыб") -> R.drawable.ic_cat_fish
            c.contains("dairy") || c.contains("молоч") || c.contains("яйц") -> R.drawable.ic_cat_dairy
            c.contains("groc") || c.contains("бакал") || c.contains("круп") -> R.drawable.ic_cat_grocery
            c.contains("bak") || c.contains("хлеб") || c.contains("выпеч") -> R.drawable.ic_cat_bakery
            c.contains("green") || c.contains("зелен") -> R.drawable.ic_cat_greens
            c.contains("spic") || c.contains("специ") -> R.drawable.ic_cat_spices
            c.contains("sweet") || c.contains("слад") -> R.drawable.ic_cat_sweets
            else -> R.drawable.ic_cat_other
        }
    }

    private fun getCategoryColor(category: String): Int {
        val c = category.lowercase()
        return when {
            c.contains("veg") || c.contains("овощ") || c.contains("green") || c.contains("зелен") || c.contains("fru") || c.contains("фрукт") || c.contains("ягод") -> Color.parseColor("#4CAF50")
            c.contains("meat") || c.contains("мясо") || c.contains("fish") || c.contains("рыб") -> Color.parseColor("#F44336")
            c.contains("dairy") || c.contains("молоч") || c.contains("яйц") -> Color.parseColor("#03A9F4")
            c.contains("groc") || c.contains("бакал") || c.contains("bak") || c.contains("хлеб") || c.contains("выпеч") -> Color.parseColor("#FF9800")
            c.contains("spic") || c.contains("специ") || c.contains("sweet") || c.contains("слад") -> Color.parseColor("#E91E63")
            else -> Color.parseColor("#9C27B0")
        }
    }
}