package com.example.application.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.application.R
import kotlinx.coroutines.launch

class AddRecipeFragment : Fragment() {

    private val viewModel: AddRecipeViewModel by viewModels()
    private var currentDifficulty = 2
    private var currentServings = 2
    private val ingredientRows = mutableListOf<View>()
    private var categoriesIds: List<Long> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_add_recipe, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.backBtn).setOnClickListener { findNavController().navigateUp() }
        view.findViewById<TextView>(R.id.cancelBtn).setOnClickListener { findNavController().navigateUp() }

        // Сложность
        val easy = view.findViewById<TextView>(R.id.diffEasy)
        val medium = view.findViewById<TextView>(R.id.diffMedium)
        val hard = view.findViewById<TextView>(R.id.diffHard)
        fun updateDiffUI() {
            listOf(easy to 1, medium to 2, hard to 3).forEach { (tv, level) ->
                val active = currentDifficulty == level
                tv.setBackgroundColor(if (active) 0xFFFF7043.toInt() else 0xFF1E1E1E.toInt())
                tv.setTextColor(if (active) 0xFFFFFFFF.toInt() else 0xFF9E9E9E.toInt())
            }
        }
        easy.setOnClickListener { currentDifficulty = 1; updateDiffUI() }
        medium.setOnClickListener { currentDifficulty = 2; updateDiffUI() }
        hard.setOnClickListener { currentDifficulty = 3; updateDiffUI() }
        updateDiffUI()

        // Порции
        val servingsText = view.findViewById<TextView>(R.id.servingsValue)
        view.findViewById<TextView>(R.id.btnMinus).setOnClickListener {
            if (currentServings > 1) { currentServings--; servingsText.text = currentServings.toString() }
        }
        view.findViewById<TextView>(R.id.btnPlus).setOnClickListener {
            if (currentServings < 12) { currentServings++; servingsText.text = currentServings.toString() }
        }

        // Ингредиенты
        val ingredientsContainer = view.findViewById<LinearLayout>(R.id.ingredientsContainer)
        fun addIngredientRow() {
            val row = layoutInflater.inflate(R.layout.item_ingredient_row, ingredientsContainer, false)
            row.findViewById<TextView>(R.id.deleteBtn).setOnClickListener {
                ingredientsContainer.removeView(row)
                ingredientRows.remove(row)
            }
            ingredientsContainer.addView(row)
            ingredientRows.add(row)
        }
        // Три стартовые строки
        repeat(3) { addIngredientRow() }
        view.findViewById<TextView>(R.id.addIngredientBtn).setOnClickListener { addIngredientRow() }

        // Категории — заполним spinner
        val spinner = view.findViewById<Spinner>(R.id.categorySpinner)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { cats ->
                    categoriesIds = cats.map { it.id }
                    val names = cats.map { it.name }
                    spinner.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        names
                    )
                }
            }
        }

        // Сохранить
        view.findViewById<TextView>(R.id.saveBtn).setOnClickListener {
            val title = view.findViewById<EditText>(R.id.inputTitle).text.toString().trim()
            val timeStr = view.findViewById<EditText>(R.id.inputTime).text.toString().trim()
            val instructions = view.findViewById<EditText>(R.id.inputInstructions).text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Введите название", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (timeStr.isEmpty()) {
                Toast.makeText(requireContext(), "Введите время", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedCategoryId = categoriesIds.getOrNull(spinner.selectedItemPosition)
            if (selectedCategoryId == null) {
                Toast.makeText(requireContext(), "Выберите категорию", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rows = ingredientRows.mapNotNull { row ->
                val name = row.findViewById<EditText>(R.id.ingName).text.toString()
                val qty = row.findViewById<EditText>(R.id.ingQty).text.toString()
                val unit = row.findViewById<EditText>(R.id.ingUnit).text.toString()
                if (name.isBlank()) null else IngredientRow(name, qty, unit)
            }

            viewModel.save(
                title = title,
                timeMinutes = timeStr.toIntOrNull() ?: 30,
                complexity = currentDifficulty,
                servings = currentServings,
                instructions = instructions,
                categoryId = selectedCategoryId,
                ingredients = rows
            )
        }

        // Подписка на saved
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saved.collect { done ->
                    if (done) {
                        Toast.makeText(requireContext(), "Сохранено", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }
}