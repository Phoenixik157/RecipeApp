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
import com.example.application.utils.TextUtils.capitalize
import com.example.application.utils.XpManager

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
        // Подписка на продукты пользователя, чтобы обновлять ряд
        // Подписка на продукты, которые есть в холодильнике
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val db = com.example.application.data.local.AppDatabase.getInstance(requireContext())

                // Используем наш новый метод с JOIN
                db.ingredientDao().getAvailableIngredientsFlow().collect { ings ->
                    val ingredientsRow = view.findViewById<LinearLayout>(R.id.ingredientsRow)
                    ingredientsRow.removeAllViews()

                    // 1. Кнопка "Добавить" ВСЕГДА первая
                    val addBtn = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = android.view.Gravity.CENTER_HORIZONTAL
                        setPadding(0, 0, 24, 0)
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
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { gravity = android.view.Gravity.CENTER_HORIZONTAL }
                    }
                    addBtn.addView(addIcon)
                    addBtn.addView(addLabel)
                    addBtn.setOnClickListener { openProductsSheet() }
                    ingredientsRow.addView(addBtn)

                    // 2. Отмеченные ингредиенты
                    ings.forEach { ing ->
                        val item = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = android.view.Gravity.CENTER_HORIZONTAL
                            setPadding(0, 0, 24, 0)
                        }
                        val iconBox = TextView(requireContext()).apply {
                            text = ing.emoji
                            textSize = 24f
                            gravity = android.view.Gravity.CENTER
                            layoutParams = LinearLayout.LayoutParams(104, 104)
                            setBackgroundColor(0xFF1E1E1E.toInt())
                        }
                        val shortName = capitalize(ing.displayName).split(" ").firstOrNull() ?: capitalize(ing.displayName)
                        val text = TextView(requireContext()).apply {
                            text = shortName
                            textSize = 11f
                            setTextColor(0xFF9E9E9E.toInt())
                            gravity = android.view.Gravity.CENTER
                            maxLines = 1
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            layoutParams = LinearLayout.LayoutParams(104, LinearLayout.LayoutParams.WRAP_CONTENT)
                        }
                        item.addView(iconBox)
                        item.addView(text)

                        // ДИАЛОГ УДАЛЕНИЯ
                        item.setOnClickListener {
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle(capitalize(ing.displayName))
                                .setPositiveButton("ОК", null)
                                .setNegativeButton("Удалить") { _, _ ->
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        db.userProductDao().deleteById(ing.id)
                                        // Ничего не перерисовываем вручную! Flow сделает всё сам.
                                    }
                                }
                                .show()
                        }
                        ingredientsRow.addView(item)
                    }
                }
            }
        }

        // FAB
        view.findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            findNavController().navigate(R.id.nav_add_recipe)
        }

        // Поиск
        val searchInput = view.findViewById<android.widget.EditText>(R.id.searchInput)
        searchInput.doOnTextChanged { text, _, _, _ ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }

        // Кнопка наверх
        val scrollView = view.findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)
        val fabTop = view.findViewById<FloatingActionButton>(R.id.fabScrollTop)

        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 500) fabTop.show() else fabTop.hide()
        }
        fabTop.setOnClickListener {
            scrollView.smoothScrollTo(0, 0)
        }

        view.findViewById<TextView>(R.id.filterBtn).setOnClickListener {
            val sheet = FilterBottomSheet()
            sheet.currentSort = viewModel.sortType.value
            sheet.currentMinMode = viewModel.minPurchasesMode.value
            sheet.currentAscending = viewModel.isAscending.value

            sheet.onApply = { sort, minMode, isAscending ->
                viewModel.setSortType(sort)
                viewModel.setAscending(isAscending)

                if (minMode != viewModel.minPurchasesMode.value) {
                    viewModel.toggleMinPurchasesMode()
                }
            }
            sheet.show(parentFragmentManager, "FilterBottomSheet")
        }

        // Подписка на рецепты
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recipes.collect { list ->
                    adapter.submitList(list)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userStats.collect { stats ->
                    val s = stats ?: com.example.application.data.local.entity.UserStatsEntity()

                    // Название уровня
                    view.findViewById<TextView>(R.id.levelBadge).text =
                        "🔥 ${XpManager.levelName(s.level)} ${s.level} ур."

                    // Прогресс уровня
                    val (inLevel, levelRange, _) =
                        com.example.application.utils.XpManager.progressFor(s)
                    view.findViewById<TextView>(R.id.xpCurrent).text = "$inLevel XP"
                    view.findViewById<TextView>(R.id.xpTotal).text = "$levelRange XP"
                    val progress = if (levelRange > 0) (inLevel * 100) / levelRange else 0
                    view.findViewById<android.widget.ProgressBar>(R.id.xpProgress).progress = progress

                    // Дневной лимит
                    val dailyMax = com.example.application.utils.XpManager.dailyMaxXp()
                    view.findViewById<TextView>(R.id.xpTodayText).text =
                        "Сегодня: +${s.dailyXpGained}/$dailyMax XP"
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                com.example.application.utils.AchievementManager.newlyUnlocked.collect { list ->
                    list.forEach { id ->
                        val title = com.example.application.utils.AchievementManager.TITLES[id] ?: id
                        android.widget.Toast.makeText(
                            requireContext(),
                            "🏆 Достижение: $title",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
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