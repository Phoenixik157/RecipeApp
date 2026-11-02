package com.example.application.ui.detail

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_recipe_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recipeId = arguments?.getLong("recipeId") ?: return
        viewModel.load(recipeId)

        view.findViewById<TextView>(R.id.backButton).setOnClickListener {
            findNavController().navigateUp()
        }

        // Порции
        view.findViewById<TextView>(R.id.btnMinus).setOnClickListener { viewModel.decreaseServings() }
        view.findViewById<TextView>(R.id.btnPlus).setOnClickListener { viewModel.increaseServings() }

        view.findViewById<TextView>(R.id.btnShare).setOnClickListener {
            val recipe = viewModel.recipe.value ?: return@setOnClickListener
            QrDialog.show(requireContext(), recipe)
        }

        view.findViewById<TextView>(R.id.btnCooked).setOnClickListener {
            val recipe = viewModel.recipe.value ?: return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                val xp = viewModel.awardCooked(recipe.id, viewModel.servings.value)
                val msg = if (xp > 0) "Приготовлено! +$xp XP" else "Записано. Лимит XP на сегодня исчерпан"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        // Подписка на рецепт
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recipe.collect { recipe ->
                        recipe ?: return@collect
                        view.findViewById<TextView>(R.id.titleText).text = recipe.title
                        view.findViewById<TextView>(R.id.timeChip).text = "⏱ ${recipe.cookingTimeMinutes} мин"
                        view.findViewById<TextView>(R.id.difficultyChip).text = starsFor(recipe.complexity)
                        view.findViewById<TextView>(R.id.instructionsText).text = recipe.instructions

                        if (!recipe.imageUrl.isNullOrEmpty()) {
                            view.findViewById<ImageView>(R.id.heroImage).load(recipe.imageUrl)
                        }
                    }
                }

                // Отдельная подписка на название категории (без вложенности)
                launch {
                    viewModel.categoryName.collect { catName ->
                        view.findViewById<TextView>(R.id.categoryText).text = catName
                    }
                }
            }
        }

        // Порции — отображение
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.servings.collect { s ->
                    view.findViewById<TextView>(R.id.servingsValue).text = s.toString()
                    rebuildIngredients(view, s)
                }
            }
        }

        // Ингредиенты
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ingredients.collect {
                    rebuildIngredients(view, viewModel.servings.value)
                }
            }
        }

        // Уведомления о достижениях
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

            // Иконка категории
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
                setColorFilter(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(24, 24)
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

            // Статус: есть / нет / замена
            val status = TextView(requireContext()).apply {
                textSize = 18f
                setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.inter_regular))
                if (item.haveIt) {
                    text = "✅"
                } else if (item.replacement != null) {
                    text = "🔄 ${item.replacement}"
                    setTextColor(Color.parseColor("#FFA000"))
                    textSize = 12f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    text = "❌"
                }
            }

            row.addView(iconContainer)
            row.addView(name)
            row.addView(measure)
            row.addView(status)
            container.addView(row)

            if (idx < list.size - 1) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 2
                    )
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_dark))
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

    // Вспомогательные функции для иконок категорий (скопированы из HomeFragment)
    private fun getCategoryIcon(category: String): Int {
        return when (category) {
            "vegetables" -> R.drawable.ic_cat_vegetables
            "fruits" -> R.drawable.ic_cat_fruits
            "meat" -> R.drawable.ic_cat_meat
            "fish" -> R.drawable.ic_cat_fish
            "dairy" -> R.drawable.ic_cat_dairy
            "grocery" -> R.drawable.ic_cat_grocery
            "bakery" -> R.drawable.ic_cat_bakery
            "greens" -> R.drawable.ic_cat_greens
            "spices" -> R.drawable.ic_cat_spices
            "sweets" -> R.drawable.ic_cat_sweets
            else -> R.drawable.ic_cat_other
        }
    }

    private fun getCategoryColor(category: String): Int {
        return when (category) {
            "vegetables", "greens", "fruits" -> Color.parseColor("#4CAF50")
            "meat", "fish" -> Color.parseColor("#F44336")
            "dairy" -> Color.parseColor("#03A9F4")
            "grocery", "bakery" -> Color.parseColor("#FF9800")
            "spices", "sweets" -> Color.parseColor("#E91E63")
            else -> Color.parseColor("#9C27B0")
        }
    }
}