package com.example.application.ui.detail

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.application.R
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

        // Заглушки для кнопок
        view.findViewById<TextView>(R.id.btnShare).setOnClickListener {
            Toast.makeText(requireContext(), "QR-код скоро будет", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<TextView>(R.id.btnCooked).setOnClickListener {
            Toast.makeText(requireContext(), "Записано в историю", Toast.LENGTH_SHORT).show()
        }

        // Рецепт
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recipe.collect { recipe ->
                    recipe ?: return@collect
                    view.findViewById<TextView>(R.id.titleText).text = recipe.title
                    view.findViewById<TextView>(R.id.timeChip).text = "⏱ ${recipe.cookingTimeMinutes} мин"
                    view.findViewById<TextView>(R.id.difficultyChip).text = starsFor(recipe.complexity)
                    view.findViewById<TextView>(R.id.instructionsText).text = recipe.instructions

                    if (!recipe.imageUrl.isNullOrEmpty()) {
                        view.findViewById<android.widget.ImageView>(R.id.heroImage).load(recipe.imageUrl)
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
            val emoji = TextView(requireContext()).apply {
                text = item.ingredient.emoji
                textSize = 20f
            }
            val name = TextView(requireContext()).apply {
                text = item.ingredient.displayName
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(28, 0, 0, 0)
            }
            val qty = (item.quantity * ratio).let {
                if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it)
            }
            val measure = TextView(requireContext()).apply {
                text = if (item.quantity > 0) "$qty ${item.unit}" else item.unit
                setTextColor(0xFF9E9E9E.toInt())
                textSize = 13f
            }
            row.addView(emoji)
            row.addView(name)
            row.addView(measure)
            container.addView(row)

            if (idx < list.size - 1) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 2
                    )
                    setBackgroundColor(0xFF2C2C2C.toInt())
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
}