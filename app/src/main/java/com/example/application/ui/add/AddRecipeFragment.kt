package com.example.application.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.application.R
import com.example.application.viewmodels.AddRecipeViewModel
import com.example.application.viewmodels.IngredientRow
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.launch

class AddRecipeFragment : Fragment() {

    private val viewModel: AddRecipeViewModel by viewModels()
    private var currentDifficulty = 2
    private var currentServings = 1
    private val ingredientRows = mutableListOf<View>()
    private var categoriesIds: List<Long> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_add_recipe, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Объявляем переменные
        val backBtn = view.findViewById<View>(R.id.backBtn)
        val cancelBtn = view.findViewById<View>(R.id.cancelBtn)
        val saveBtn = view.findViewById<View>(R.id.saveBtn)
        val ingredientsContainer = view.findViewById<LinearLayout>(R.id.ingredientsContainer)
        val categoryDropdown = view.findViewById<MaterialAutoCompleteTextView>(R.id.categoryDropdown)
        val servingsEditText = view.findViewById<EditText>(R.id.servingsValue)
        val servingsSeekBar = view.findViewById<SeekBar>(R.id.servingsSeekBar)
        var isUpdatingServings = false

        // Кнопки "Назад" и "Отмена"
        backBtn.setOnClickListener { findNavController().navigateUp() }
        cancelBtn.setOnClickListener { findNavController().navigateUp() }

        // --- Сложность ---
        val easy = view.findViewById<TextView>(R.id.diffEasy)
        val medium = view.findViewById<TextView>(R.id.diffMedium)
        val hard = view.findViewById<TextView>(R.id.diffHard)
        fun updateDiffUI() {
            listOf(easy to 1, medium to 2, hard to 3).forEach { (tv, level) ->
                val active = currentDifficulty == level
                tv.isSelected = active
                tv.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), if (active) R.color.text_primary else R.color.text_secondary))
            }
        }
        easy.setOnClickListener { currentDifficulty = 1; updateDiffUI() }
        medium.setOnClickListener { currentDifficulty = 2; updateDiffUI() }
        hard.setOnClickListener { currentDifficulty = 3; updateDiffUI() }
        updateDiffUI()

        // --- Порции ---
        fun updateServingsUI(newValue: Int) {
            if (isUpdatingServings) return
            isUpdatingServings = true
            currentServings = newValue.coerceIn(1, 1488)
            servingsEditText.setText(currentServings.toString())
            servingsEditText.setSelection(servingsEditText.text.length)
            servingsSeekBar.progress = currentServings - 1
            isUpdatingServings = false
        }

        updateServingsUI(1)

        view.findViewById<View>(R.id.btnMinus).setOnClickListener { updateServingsUI(currentServings - 1) }
        view.findViewById<View>(R.id.btnPlus).setOnClickListener { updateServingsUI(currentServings + 1) }

        servingsEditText.doOnTextChanged { text, _, _, _ ->
            if (isUpdatingServings) return@doOnTextChanged
            val parsed = text?.toString()?.toIntOrNull()
            if (parsed != null && parsed in 1..1488) {
                currentServings = parsed
                isUpdatingServings = true
                servingsSeekBar.progress = currentServings - 1
                isUpdatingServings = false
            }
        }

        servingsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isUpdatingServings) {
                    currentServings = progress + 1
                    isUpdatingServings = true
                    servingsEditText.setText(currentServings.toString())
                    servingsEditText.setSelection(servingsEditText.text.length)
                    isUpdatingServings = false
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Умная проверка при потере фокуса
        servingsEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = servingsEditText.text.toString()
                val parsed = text.toIntOrNull()

                if (parsed == null || parsed < 1) {
                    // Если пусто или меньше 1 — ставим 1
                    updateServingsUI(1)
                } else if (parsed > 1488) {
                    // Если больше 1488 — ставим 1488
                    updateServingsUI(1488)
                    Toast.makeText(requireContext(), "Максимум 1488 порций", Toast.LENGTH_SHORT).show()
                } else {
                    // Если в пределах нормы, просто синхронизируем ползунок
                    updateServingsUI(parsed)
                }
            }
        }

        // --- Ингредиенты ---
        fun addIngredientRow() {
            val row = layoutInflater.inflate(R.layout.item_ingredient_row, ingredientsContainer, false)
            row.findViewById<TextView>(R.id.deleteBtn).setOnClickListener {
                ingredientsContainer.removeView(row)
                ingredientRows.remove(row)
            }
            ingredientsContainer.addView(row)
            ingredientRows.add(row)
        }
        repeat(3) { addIngredientRow() }
        view.findViewById<View>(R.id.addIngredientBtn).setOnClickListener { addIngredientRow() }

        // --- Категории ---
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { cats ->
                    categoriesIds = cats.map { it.id }
                    val names = cats.map { it.name }
                    categoryDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names))
                    if (categoryDropdown.text.isNullOrEmpty() && names.isNotEmpty()) {
                        categoryDropdown.setText(names[0], false)
                    }
                }
            }
        }

        // --- Сохранить ---
        saveBtn.setOnClickListener {
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

            val selectedCategoryName = categoryDropdown.text.toString()
            val selectedCategoryId = viewModel.categories.value.find { it.name == selectedCategoryName }?.id

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

        // --- Подписка на saved ---
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

        // --- РЕЖИМ РЕДАКТИРОВАНИЯ ---
        val editId = arguments?.getLong("recipeId", -1L) ?: -1L
        if (editId > 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(300)
                val recipe = viewModel.loadForEdit(editId) ?: return@launch

                view.findViewById<EditText>(R.id.inputTitle).setText(recipe.title)
                view.findViewById<EditText>(R.id.inputTime).setText(recipe.cookingTimeMinutes.toString())
                view.findViewById<EditText>(R.id.inputInstructions).setText(recipe.instructions)

                currentDifficulty = recipe.complexity
                updateDiffUI()

                // Обновляем порции (вместо servingsText)
                updateServingsUI(recipe.baseServings)

                // Обновляем категорию (вместо spinner)
                val catName = viewModel.categories.value.find { it.id == recipe.categoryId }?.name ?: ""
                categoryDropdown.setText(catName, false)

                val db = com.example.application.data.local.AppDatabase.getInstance(requireContext())
                val links = db.recipeIngredientDao().getByRecipeId(editId)
                val ids = links.map { it.ingredientId }
                val ings = db.ingredientDao().getByIds(ids)
                val map = ings.associateBy { it.id }

                ingredientsContainer.removeAllViews()
                ingredientRows.clear()

                links.forEach { link ->
                    val row = layoutInflater.inflate(R.layout.item_ingredient_row, ingredientsContainer, false)
                    val ing = map[link.ingredientId]
                    if (ing != null) {
                        row.findViewById<EditText>(R.id.ingName).setText(ing.displayName)
                        val qtyStr = if (link.quantity % 1.0 == 0.0) link.quantity.toInt().toString() else link.quantity.toString()
                        row.findViewById<EditText>(R.id.ingQty).setText(qtyStr)
                        row.findViewById<EditText>(R.id.ingUnit).setText(link.unit)
                    }
                    row.findViewById<TextView>(R.id.deleteBtn).setOnClickListener {
                        ingredientsContainer.removeView(row)
                        ingredientRows.remove(row)
                    }
                    ingredientsContainer.addView(row)
                    ingredientRows.add(row)
                }
            }
        }
    }
}