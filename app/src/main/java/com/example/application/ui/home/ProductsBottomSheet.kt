package com.example.application.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.application.R
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.IngredientEntity
import com.example.application.data.local.entity.UserProductEntity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ProductsBottomSheet : BottomSheetDialogFragment() {

    private var allIngredients: List<IngredientEntity> = emptyList()
    private val selectedIds = mutableSetOf<Long>()
    private var onDone: (() -> Unit)? = null

    fun setOnDoneListener(listener: () -> Unit) {
        onDone = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_products, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        val groupsContainer = view.findViewById<LinearLayout>(R.id.groupsContainer)
        val searchInput = view.findViewById<EditText>(R.id.searchInput)
        val showBtn = view.findViewById<TextView>(R.id.showRecipesBtn)

        // ВАЖНО: используем viewLifecycleOwner.lifecycleScope, чтобы UI обновлялся корректно
        viewLifecycleOwner.lifecycleScope.launch {
            // 1. Сначала загружаем все ингредиенты
            allIngredients = db.ingredientDao().getAllOnce()

            // 2. Загружаем ID сохраненных продуктов из БД
            selectedIds.clear()
            selectedIds.addAll(db.userProductDao().getAllIds())

            // 3. Только теперь отрисовываем список
            buildGroups(groupsContainer, "")
            updateCounter(showBtn)
        }

        // Поиск
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                buildGroups(groupsContainer, s?.toString().orEmpty())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Сбросить
        view.findViewById<TextView>(R.id.clearAllBtn).setOnClickListener {
            selectedIds.clear()
            buildGroups(groupsContainer, searchInput.text.toString())
            updateCounter(showBtn)
        }

        // Показать рецепты
        showBtn.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                db.userProductDao().clear()
                selectedIds.forEach { id ->
                    db.userProductDao().insert(UserProductEntity(id))
                }
                onDone?.invoke()
                dismiss()
            }
        }
    }

    private fun buildGroups(container: LinearLayout, query: String) {
        container.removeAllViews()

        val grouped = allIngredients
            .filter { query.isEmpty() || it.displayName.contains(query, true) }
            .groupBy { it.category }

        val titles = mapOf(
            "vegetables" to "🥬 ОВОЩИ",
            "fruits" to "🍎 ФРУКТЫ И ЯГОДЫ",
            "meat" to "🥩 МЯСО И ПТИЦА",
            "fish" to "🐟 РЫБА И МОРЕПРОДУКТЫ",
            "dairy" to "🥛 МОЛОЧКА И ЯЙЦА",
            "grocery" to "🍚 БАКАЛЕЯ",
            "bakery" to "🍞 ХЛЕБ И ВЫПЕЧКА",
            "greens" to "🌿 ЗЕЛЕНЬ",
            "spices" to "🧂 СПЕЦИИ И ПРИПРАВЫ",
            "sweets" to "🍬 СЛАДОСТИ",
            "other" to "🍽 ПРОЧЕЕ"
        )

        titles.forEach { (key, title) ->
            val items = grouped[key] ?: return@forEach
            if (items.isEmpty()) return@forEach

            val header = TextView(requireContext()).apply {
                text = title
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                textSize = 12f
                setPadding(0, 30, 0, 12)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            container.addView(header)

            items.forEach { ingredient ->
                val row = layoutInflater.inflate(R.layout.item_product_checkbox, container, false)
                val checkBox = row.findViewById<ImageView>(R.id.checkBox)
                val name = row.findViewById<TextView>(R.id.productName)
                name.text = ingredient.displayName

                fun update() {
                    val checked = selectedIds.contains(ingredient.id)
                    if (checked) {
                        checkBox.setImageResource(R.drawable.ic_check_circle)
                        checkBox.setColorFilter(ContextCompat.getColor(requireContext(), R.color.purple_primary))
                        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    } else {
                        checkBox.setImageResource(R.drawable.ic_circle_outline)
                        checkBox.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
                update() // Вызывается сразу при создании строки

                row.setOnClickListener {
                    if (selectedIds.contains(ingredient.id)) {
                        selectedIds.remove(ingredient.id)
                    } else {
                        selectedIds.add(ingredient.id)
                    }
                    update()
                    updateCounter(view?.findViewById(R.id.showRecipesBtn))
                }

                container.addView(row)
            }
        }
    }

    private fun updateCounter(btn: TextView?) {
        btn?.text = "Показать рецепты (${selectedIds.size})"
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        if (bottomSheet != null) {
            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
            val screenHeight = resources.displayMetrics.heightPixels
            bottomSheet.layoutParams.height = (screenHeight * 0.75).toInt()
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            // Убираем блокировку свайпа, раз мы удалили крестик,
            // но оставляем расширенное состояние
            behavior.isDraggable = true
        }
    }
}