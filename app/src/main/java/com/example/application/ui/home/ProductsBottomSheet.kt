package com.example.application.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.application.R
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.UserProductEntity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ProductsBottomSheet : BottomSheetDialogFragment() {

    // Все ингредиенты, полученные из БД один раз при открытии
    private var allIngredients: List<com.example.application.data.local.entity.IngredientEntity> = emptyList()

    // id продуктов, которые сейчас отмечены
    private val selectedIds = mutableSetOf<Long>()

    // Колбэк, который вызовется после сохранения — чтобы обновить главный экран
    private var onDone: (() -> Unit)? = null

    fun setOnDoneListener(listener: () -> Unit) {
        onDone = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_products, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        val groupsContainer = view.findViewById<LinearLayout>(R.id.groupsContainer)
        val searchInput = view.findViewById<EditText>(R.id.searchInput)
        val showBtn = view.findViewById<TextView>(R.id.showRecipesBtn)

        // Кнопка закрытия
        view.findViewById<TextView>(R.id.closeBtn).setOnClickListener { dismiss() }

        // Загружаем данные из БД
        lifecycleScope.launch {
            allIngredients = db.ingredientDao().getAllOnce()
            selectedIds.clear()
            selectedIds.addAll(db.userProductDao().getAllIds())

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

        // Кнопка «Показать рецепты (N)»
        showBtn.setOnClickListener {
            lifecycleScope.launch {
                db.userProductDao().clear()
                selectedIds.forEach { id ->
                    db.userProductDao().insert(UserProductEntity(id))
                }
                onDone?.invoke()
                dismiss()
            }
        }
    }

    /** Перестраивает список групп и продуктов внутри панели. */
    private fun buildGroups(container: LinearLayout, query: String) {
        container.removeAllViews()

        // Фильтрация по поиску и группировка по категориям
        val grouped = allIngredients
            .filter { query.isEmpty() || it.displayName.contains(query, true) }
            .groupBy { it.category }

        val titles = mapOf(
            "vegetables" to "🥬 ОВОЩИ",
            "meat" to "🥩 МЯСО И РЫБА",
            "dairy" to "🥛 МОЛОЧКА",
            "grocery" to "🍚 БАКАЛЕЯ",
            "other" to "ПРОЧЕЕ"
        )

        titles.forEach { (key, title) ->
            val items = grouped[key] ?: return@forEach
            if (items.isEmpty()) return@forEach

            // Заголовок группы
            val header = TextView(requireContext()).apply {
                text = title
                setTextColor(0xFF9E9E9E.toInt())
                textSize = 12f
                setPadding(0, 30, 0, 12)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            container.addView(header)

            // Строки продуктов
            items.forEach { ingredient ->
                val row = layoutInflater.inflate(R.layout.item_product_checkbox, container, false)
                val checkBox = row.findViewById<View>(R.id.checkBox)
                val name = row.findViewById<TextView>(R.id.productName)
                name.text = ingredient.displayName

                // Функция обновления визуального состояния строки
                fun update() {
                    val checked = selectedIds.contains(ingredient.id)
                    checkBox.setBackgroundColor(
                        if (checked) 0xFFFF7043.toInt() else 0xFF2C2C2C.toInt()
                    )
                    name.setTextColor(
                        if (checked) 0xFFFFFFFF.toInt() else 0xFF9E9E9E.toInt()
                    )
                }
                update()

                // Клик по строке — переключаем выбор
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

    /** Обновляет текст кнопки: «Показать рецепты (N)». */
    private fun updateCounter(btn: TextView?) {
        btn?.text = "Показать рецепты (${selectedIds.size})"
    }

    override fun onStart() {
        super.onStart()
        // Раскрываем панель на полную высоту
        (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)?.behavior
            ?.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
    }
}