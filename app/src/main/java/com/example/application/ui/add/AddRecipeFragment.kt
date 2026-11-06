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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.application.R
import com.example.application.viewmodels.AddRecipeViewModel
import com.example.application.viewmodels.IngredientRow
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.launch

class AddRecipeFragment : Fragment() {

    private val viewModel: AddRecipeViewModel by activityViewModels()
    private var isUpdatingServings = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_add_recipe, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)

        val backBtn = view.findViewById<View>(R.id.backBtn)
        val cancelBtn = view.findViewById<View>(R.id.cancelBtn)
        val saveBtn = view.findViewById<View>(R.id.saveBtn)
        val ingredientsContainer = view.findViewById<LinearLayout>(R.id.ingredientsContainer)
        val categoryDropdown = view.findViewById<MaterialAutoCompleteTextView>(R.id.categoryDropdown)
        val servingsEditText = view.findViewById<EditText>(R.id.servingsValue)
        val servingsSeekBar = view.findViewById<SeekBar>(R.id.servingsSeekBar)

        val inputTitle = view.findViewById<EditText>(R.id.inputTitle)
        val inputTime = view.findViewById<EditText>(R.id.inputTime)
        val inputInstructions = view.findViewById<EditText>(R.id.inputInstructions)

        // 1. Восстанавливаем текстовые поля из ViewModel
        inputTitle.setText(viewModel.title.value)
        inputTime.setText(viewModel.timeMinutes.value)
        inputInstructions.setText(viewModel.instructions.value)

        inputTitle.doOnTextChanged { text, _, _, _ -> viewModel.title.value = text.toString() }
        inputTime.doOnTextChanged { text, _, _, _ -> viewModel.timeMinutes.value = text.toString() }
        inputInstructions.doOnTextChanged { text, _, _, _ -> viewModel.instructions.value = text.toString() }

        // Навигация
        backBtn.setOnClickListener { bottomNav.selectedItemId = R.id.nav_home }
        cancelBtn.setOnClickListener {
            viewModel.clearState() // Очищаем форму по кнопке Отмена
            bottomNav.selectedItemId = R.id.nav_home
        }

        // --- Сложность ---
        val easy = view.findViewById<TextView>(R.id.diffEasy)
        val medium = view.findViewById<TextView>(R.id.diffMedium)
        val hard = view.findViewById<TextView>(R.id.diffHard)

        fun updateDiffUI(level: Int) {
            listOf(easy to 1, medium to 2, hard to 3).forEach { (tv, l) ->
                val active = level == l
                tv.isSelected = active
                tv.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), if (active) R.color.text_primary else R.color.text_secondary))
            }
        }
        updateDiffUI(viewModel.difficulty.value)

        easy.setOnClickListener { viewModel.difficulty.value = 1; updateDiffUI(1) }
        medium.setOnClickListener { viewModel.difficulty.value = 2; updateDiffUI(2) }
        hard.setOnClickListener { viewModel.difficulty.value = 3; updateDiffUI(3) }

        // --- Порции ---
        fun updateServingsUI(newValue: Int) {
            if (isUpdatingServings) return
            isUpdatingServings = true
            val clamped = newValue.coerceIn(1, 100)
            viewModel.servings.value = clamped
            servingsEditText.setText(clamped.toString())
            servingsEditText.setSelection(servingsEditText.text.length)
            servingsSeekBar.progress = clamped - 1
            isUpdatingServings = false
        }
        updateServingsUI(viewModel.servings.value)

        view.findViewById<View>(R.id.btnMinus).setOnClickListener { updateServingsUI(viewModel.servings.value - 1) }
        view.findViewById<View>(R.id.btnPlus).setOnClickListener { updateServingsUI(viewModel.servings.value + 1) }

        servingsEditText.doOnTextChanged { text, _, _, _ ->
            if (isUpdatingServings) return@doOnTextChanged
            val parsed = text?.toString()?.toIntOrNull()
            if (parsed != null && parsed in 1..100) {
                viewModel.servings.value = parsed
                isUpdatingServings = true
                servingsSeekBar.progress = parsed - 1
                isUpdatingServings = false
            }
        }

        servingsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isUpdatingServings) updateServingsUI(progress + 1)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // --- Ингредиенты ---
        fun renderIngredients() {
            ingredientsContainer.removeAllViews()
            viewModel.ingredientRows.value.forEach { rowData ->
                val row = layoutInflater.inflate(R.layout.item_ingredient_row, ingredientsContainer, false)
                val nameInput = row.findViewById<EditText>(R.id.ingName)
                val qtyInput = row.findViewById<EditText>(R.id.ingQty)
                val unitInput = row.findViewById<EditText>(R.id.ingUnit)

                nameInput.setText(rowData.name)
                qtyInput.setText(rowData.quantity)
                unitInput.setText(rowData.unit)

                nameInput.doOnTextChanged { text, _, _, _ -> rowData.name = text.toString() }
                qtyInput.doOnTextChanged { text, _, _, _ -> rowData.quantity = text.toString() }
                unitInput.doOnTextChanged { text, _, _, _ -> rowData.unit = text.toString() }

                row.findViewById<View>(R.id.deleteBtn).setOnClickListener {
                    val newList = viewModel.ingredientRows.value.toMutableList()
                    newList.remove(rowData)
                    viewModel.ingredientRows.value = newList
                    renderIngredients()
                }
                ingredientsContainer.addView(row)
            }
        }

        renderIngredients() // Отрисовываем сохраненные ингредиенты при входе

        view.findViewById<View>(R.id.addIngredientBtn).setOnClickListener {
            val newList = viewModel.ingredientRows.value.toMutableList()
            newList.add(IngredientRow())
            viewModel.ingredientRows.value = newList
            renderIngredients()
        }

        // --- Категории ---
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { cats ->
                    val names = cats.map { it.name }
                    categoryDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names))

                    if (viewModel.selectedCategoryName.value.isNotEmpty()) {
                        categoryDropdown.setText(viewModel.selectedCategoryName.value, false)
                    } else if (categoryDropdown.text.isNullOrEmpty() && names.isNotEmpty()) {
                        categoryDropdown.setText(names[0], false)
                        viewModel.selectedCategoryName.value = names[0]
                    }
                }
            }
        }

        categoryDropdown.doOnTextChanged { text, _, _, _ ->
            viewModel.selectedCategoryName.value = text.toString()
        }

        // --- Режим редактирования ---
        val editId = arguments?.getLong("recipeId", -1L) ?: -1L
        if (editId > 0 && viewModel.editingRecipeId != editId) {
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(300)
                viewModel.loadForEdit(editId)

                inputTitle.setText(viewModel.title.value)
                inputTime.setText(viewModel.timeMinutes.value)
                inputInstructions.setText(viewModel.instructions.value)
                updateDiffUI(viewModel.difficulty.value)
                updateServingsUI(viewModel.servings.value)
                categoryDropdown.setText(viewModel.selectedCategoryName.value, false)
                renderIngredients()
            }
        } else if (editId <= 0 && viewModel.editingRecipeId != null) {
            // Если переключились с редактирования на создание, очищаем форму
            viewModel.clearState()
            inputTitle.setText("")
            inputTime.setText("")
            inputInstructions.setText("")
            updateDiffUI(2)
            updateServingsUI(1)
            renderIngredients()
        }

        // --- Сохранить ---
        saveBtn.setOnClickListener {
            val title = viewModel.title.value.trim()
            val timeStr = viewModel.timeMinutes.value.trim()
            val instructions = viewModel.instructions.value.trim()
            val categoryName = viewModel.selectedCategoryName.value

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Введите название", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (timeStr.isEmpty()) {
                Toast.makeText(requireContext(), "Введите время", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedCategoryId = viewModel.categories.value.find { it.name == categoryName }?.id
            if (selectedCategoryId == null) {
                Toast.makeText(requireContext(), "Выберите категорию", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rows = viewModel.ingredientRows.value.filter { it.name.isNotBlank() }

            viewModel.save(
                title = title,
                timeMinutes = timeStr.toIntOrNull() ?: 30,
                complexity = viewModel.difficulty.value,
                servings = viewModel.servings.value,
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
                        viewModel.clearState() // Очищаем форму после успешного сохранения
                        bottomNav.selectedItemId = R.id.nav_home
                    }
                }
            }
        }

        // --- Достижения ---
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
    }
}