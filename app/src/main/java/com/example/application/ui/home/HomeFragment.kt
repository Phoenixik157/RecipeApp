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
        val adapter = RecipeAdapter { recipe ->
            val bundle = Bundle().apply { putLong("recipeId", recipe.id) }
            findNavController().navigate(R.id.nav_recipe_detail, bundle)
        }
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

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
}