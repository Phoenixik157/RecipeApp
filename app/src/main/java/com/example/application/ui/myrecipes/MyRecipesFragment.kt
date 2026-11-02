package com.example.application.ui.myrecipes

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.example.application.data.local.entity.RecipeEntity
import com.example.application.ui.home.FilterBottomSheet
import com.example.application.ui.home.RecipeAdapter
import com.example.application.viewmodels.MyRecipesViewModel
import com.example.application.viewmodels.RecipeWithMissing
import kotlinx.coroutines.launch

class MyRecipesFragment : Fragment() {

    private val viewModel: MyRecipesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_my_recipes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emptyState = view.findViewById<LinearLayout>(R.id.emptyState)
        val contentBlock = view.findViewById<LinearLayout>(R.id.contentBlock)
        val countText = view.findViewById<TextView>(R.id.countText)
        val recycler = view.findViewById<RecyclerView>(R.id.recipesRecycler)

        val adapter = RecipeAdapter(
            onClick = { recipe -> openDetail(recipe) },
            onLongClick = { recipe -> openActionsDialog(recipe) }
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // FAB (+)
        view.findViewById<View>(R.id.fabAdd).setOnClickListener {
            val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
            bottomNav.selectedItemId = R.id.nav_add_recipe
        }

        // Кнопка в пустом состоянии
        view.findViewById<View>(R.id.createFirstBtn).setOnClickListener {
            val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
            bottomNav.selectedItemId = R.id.nav_add_recipe
        }

        // Поиск
        view.findViewById<EditText>(R.id.searchInput).doOnTextChanged { t, _, _, _ ->
            viewModel.setQuery(t?.toString().orEmpty())
        }

        // Фильтры (ЕДИНСТВЕННЫЙ ОБРАБОТЧИК)
        view.findViewById<View>(R.id.filterBtn).setOnClickListener {
            val sheet = FilterBottomSheet()
            sheet.showMinPurchases = false
            sheet.currentSort = viewModel.sortType.value
            sheet.currentAscending = viewModel.isAscending.value
            sheet.currentMinMode = false

            sheet.onApply = { sort, _, isAscending ->
                viewModel.setSortType(sort)
                viewModel.setAscending(isAscending)
            }
            sheet.show(parentFragmentManager, "FilterBottomSheet")
        }

        // Подписка на список рецептов
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recipes.collect { list ->
                    adapter.submitList(
                        list.map {
                            RecipeWithMissing(
                                recipe = it,
                                missingCount = 0,
                                totalCount = 0
                            )
                        }
                    )
                    countText.text = "Всего: ${list.size}"
                    if (list.isEmpty()) {
                        emptyState.visibility = View.VISIBLE
                        contentBlock.visibility = View.GONE
                    } else {
                        emptyState.visibility = View.GONE
                        contentBlock.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun openDetail(recipe: RecipeEntity) {
        val bundle = Bundle().apply { putLong("recipeId", recipe.id) }
        findNavController().navigate(R.id.nav_recipe_detail, bundle)
    }

    private fun openActionsDialog(recipe: RecipeEntity) {
        val options = arrayOf("Редактировать", "Удалить")
        AlertDialog.Builder(requireContext())
            .setTitle(recipe.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val bundle = Bundle().apply { putLong("recipeId", recipe.id) }
                        val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
                        bottomNav.selectedItemId = R.id.nav_add_recipe
                        findNavController().navigate(R.id.nav_add_recipe, bundle)
                    }
                    1 -> confirmDelete(recipe)
                }
            }
            .show()
    }

    private fun confirmDelete(recipe: RecipeEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить рецепт?")
            .setMessage("«${recipe.title}» будет удалён безвозвратно.")
            .setPositiveButton("Удалить") { _, _ -> viewModel.delete(recipe) }
            .setNegativeButton("Отмена", null)
            .show()
    }
}