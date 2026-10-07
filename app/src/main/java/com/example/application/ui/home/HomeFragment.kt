package com.example.application.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.application.R
import com.example.application.viewmodels.HomeViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView
        val recycler = view.findViewById<RecyclerView>(R.id.recipesRecycler)
        val adapter = RecipeAdapter(
            onClick = { recipe ->
                val bundle = Bundle().apply { putLong("recipeId", recipe.id) }
                findNavController().navigate(R.id.nav_recipe_detail, bundle)
            }
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // Ряд продуктов + кнопка "Добавить"
        val ingredientsRow = view.findViewById<LinearLayout>(R.id.ingredientsRow)
        val popular = listOf(
            Triple("🥚", "Яйца", "яйца"),
            Triple("🥛", "Молоко", "молоко"),
            Triple("🧅", "Лук", "лук"),
            Triple("🍅", "Помидор", "помидор"),
            Triple("🥩", "Мясо", "говядина")
        )
        popular.forEach { (emoji, label, _) ->
            val item = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                setPadding(0, 0, 24, 0)
            }
            val iconBox = TextView(requireContext()).apply {
                text = emoji
                textSize = 24f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(104, 104)
                setBackgroundColor(0xFF1E1E1E.toInt())
            }
            val text = TextView(requireContext()).apply {
                text = label
                textSize = 11f
                setTextColor(0xFF9E9E9E.toInt())
                gravity = android.view.Gravity.CENTER
            }
            item.addView(iconBox)
            item.addView(text)
            ingredientsRow.addView(item)
        }

        val addBtn = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }
        val addIcon = TextView(requireContext()).apply {
            text = "+"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(104, 104)
            setBackgroundColor(0xFFFF7043.toInt())
        }
        val addLabel = TextView(requireContext()).apply {
            text = "Добавить"
            textSize = 11f
            setTextColor(0xFFFF7043.toInt())
            gravity = android.view.Gravity.CENTER
        }
        addBtn.addView(addIcon)
        addBtn.addView(addLabel)
        addBtn.setOnClickListener { openProductsSheet() }
        ingredientsRow.addView(addBtn)

        // FAB
        view.findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            findNavController().navigate(R.id.nav_add_recipe)
        }

        // Поиск
        val searchInput = view.findViewById<android.widget.EditText>(R.id.searchInput)
        searchInput.doOnTextChanged { text, _, _, _ ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }

        // Подписка на рецепты
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recipes.collect { list ->
                    adapter.submitList(list)
                }
            }
        }

        // Подписка на категории — построим чипсы один раз
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { categories ->
                    buildCategoryChips(view, categories, viewModel.selectedCategory.value)
                }
            }
        }

        // Подписка на выбранную категорию — перерисовываем чипсы при переключении
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedCategory.collect { selectedId ->
                    buildCategoryChips(view, viewModel.categories.value, selectedId)
                }
            }
        }

        // Подписка на режим «Минимум покупок» — показ/скрытие чипса
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.minPurchasesMode.collect { enabled ->
                    view.findViewById<TextView>(R.id.minPurchasesChip).visibility =
                        if (enabled) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun buildCategoryChips(
        view: View,
        categories: List<com.example.application.data.local.entity.CategoryEntity>,
        selectedId: Long?
    ) {
        val chipsRow = view.findViewById<LinearLayout>(R.id.chipsRow)
        chipsRow.removeAllViews()

        chipsRow.addView(createChip("Все", selected = selectedId == null) {
            viewModel.selectCategory(null)
        })

        categories.forEach { category ->
            chipsRow.addView(createChip(category.name, selected = selectedId == category.id) {
                viewModel.selectCategory(category.id)
            })
        }
    }

    private fun createChip(text: String, selected: Boolean, onClick: () -> Unit): View {
        val ctx = requireContext()
        val chip = TextView(ctx).apply {
            this.text = text
            textSize = 13f
            setPadding(40, 20, 40, 20)
            setTextColor(if (selected) 0xFFFFFFFF.toInt() else 0xFF9E9E9E.toInt())
            setBackgroundColor(if (selected) 0xFFFF7043.toInt() else 0xFF1E1E1E.toInt())
            setOnClickListener { onClick() }
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.marginEnd = 16
        chip.layoutParams = params
        return chip
    }

    private fun openProductsSheet() {
        val sheet = ProductsBottomSheet()
        sheet.setOnDoneListener {
            viewModel.recalculateMissing()
        }
        sheet.show(parentFragmentManager, "ProductsBottomSheet")
    }
}