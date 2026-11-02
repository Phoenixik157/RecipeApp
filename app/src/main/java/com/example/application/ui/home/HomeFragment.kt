package com.example.application.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import com.example.application.utils.TextUtils.capitalize
import com.example.application.utils.XpManager
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

        // Подписка на продукты, которые есть в холодильнике
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val db = com.example.application.data.local.AppDatabase.getInstance(requireContext())

                db.ingredientDao().getAvailableIngredientsFlow().collect { ings ->
                    val ingredientsRow = view.findViewById<LinearLayout>(R.id.ingredientsRow)
                    ingredientsRow.removeAllViews()

                    // 1. Кнопка "Добавить"
                    val addBtn = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = android.view.Gravity.CENTER_HORIZONTAL
                        setPadding(0, 0, 24, 0)
                    }

                    val addIcon = ImageView(requireContext()).apply {
                        setImageResource(R.drawable.ic_plus)
                        setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_primary))
                        layoutParams = LinearLayout.LayoutParams(104, 104)
                        setPadding(24, 24, 24, 24)

                        val bg = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(ContextCompat.getColor(requireContext(), R.color.purple_primary))
                        }
                        background = bg
                    }

                    val addLabel = TextView(requireContext()).apply {
                        text = "Добавить"
                        textSize = 11f
                        setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.inter_regular))
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_primary))
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

                    // 2. Ингредиенты
                    ings.forEach { ing ->
                        val item = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = android.view.Gravity.CENTER_HORIZONTAL
                            setPadding(0, 0, 24, 0)
                        }

                        val bgColor = getCategoryColor(ing.category)
                        val specificIcon = getCategoryIcon(ing.category)

                        val iconContainer = LinearLayout(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(104, 104)
                            gravity = android.view.Gravity.CENTER
                            val bg = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(bgColor)
                            }
                            background = bg
                        }

                        if (specificIcon != 0) {
                            val img = ImageView(requireContext()).apply {
                                setImageResource(specificIcon)
                                layoutParams = LinearLayout.LayoutParams(56, 56)
                            }
                            iconContainer.addView(img)
                        } else {
                            val letter = TextView(requireContext()).apply {
                                text = capitalize(ing.displayName).take(1)
                                textSize = 24f
                                setTextColor(Color.WHITE)
                                setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.montserrat_bold))
                            }
                            iconContainer.addView(letter)
                        }

                        val shortName = capitalize(ing.displayName).split(" ").firstOrNull() ?: capitalize(ing.displayName)
                        val text = TextView(requireContext()).apply {
                            text = shortName
                            textSize = 11f
                            setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.inter_regular))
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                            gravity = android.view.Gravity.CENTER
                            maxLines = 1
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            layoutParams = LinearLayout.LayoutParams(104, LinearLayout.LayoutParams.WRAP_CONTENT)
                        }

                        item.addView(iconContainer)
                        item.addView(text)

                        item.setOnClickListener {
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle(capitalize(ing.displayName))
                                .setPositiveButton("ОК", null)
                                .setNegativeButton("Удалить") { _, _ ->
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        db.userProductDao().deleteById(ing.id)
                                    }
                                }
                                .show()
                        }
                        ingredientsRow.addView(item)
                    }
                }
            }
        }

        // FAB, Поиск, Фильтры и остальной код...
        view.findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
            bottomNav.selectedItemId = R.id.nav_add_recipe
        }

        val searchInput = view.findViewById<android.widget.EditText>(R.id.searchInput)
        searchInput.doOnTextChanged { text, _, _, _ ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }

        val scrollView = view.findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)
        val fabTop = view.findViewById<FloatingActionButton>(R.id.fabScrollTop)

        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 500) fabTop.show() else fabTop.hide()
        }
        fabTop.setOnClickListener {
            scrollView.smoothScrollTo(0, 0)
        }

        view.findViewById<View>(R.id.filterBtn).setOnClickListener {
            val sheet = FilterBottomSheet()
            sheet.currentSort = viewModel.sortType.value
            sheet.currentMinMode = viewModel.minPurchasesMode.value
            sheet.currentAscending = viewModel.isAscending.value

            sheet.onApply = { sort, minMode, isAscending ->
                viewModel.setSortType(sort)
                viewModel.setAscending(isAscending)
                viewModel.setMinPurchasesMode(minMode)
            }
            sheet.show(parentFragmentManager, "FilterBottomSheet")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recipes.collect { list -> adapter.submitList(list) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userStats.collect { stats ->
                    val s = stats ?: com.example.application.data.local.entity.UserStatsEntity()
                    view.findViewById<TextView>(R.id.levelBadge).text = "${XpManager.levelName(s.level)} ${s.level} ур."
                    val (inLevel, levelRange, _) = XpManager.progressFor(s)
                    view.findViewById<TextView>(R.id.xpCurrent).text = "$inLevel XP"
                    view.findViewById<TextView>(R.id.xpTotal).text = "$levelRange XP"
                    val progress = if (levelRange > 0) (inLevel * 100) / levelRange else 0
                    view.findViewById<android.widget.ProgressBar>(R.id.xpProgress).progress = progress
                    val dailyMax = XpManager.dailyMaxXp()
                    view.findViewById<TextView>(R.id.xpTodayText).text = "Сегодня: +${s.dailyXpGained}/$dailyMax XP"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { categories ->
                    buildCategoryChips(view, categories, viewModel.selectedCategory.value)
                }
            }
        }

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
                        android.widget.Toast.makeText(requireContext(), "🏆 Достижение: $title", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // --- Вспомогательные функции ---

    // ФУНКЦИЯ ПОИСКА ИКОНКИ
    private fun getCategoryIcon(category: String): Int {
        return when (category) {
            "vegetables" -> R.drawable.ic_cat_vegetables
            "fruits" -> R.drawable.ic_cat_fruits
            "meat" -> R.drawable.ic_cat_meat
            "fish" -> R.drawable.ic_cat_fish
            "dairy" -> R.drawable.ic_cat_dairy
            "grocery" -> R.drawable.ic_cat_grocery
            "bakery" -> R.drawable.ic_cat_bakery
            "greens" -> R.drawable.ic_cat_greens
            "spices" -> R.drawable.ic_cat_spices
            "sweets" -> R.drawable.ic_cat_sweets
            else -> R.drawable.ic_cat_other
        }
    }

    private fun getCategoryColor(category: String): Int {
        return when (category) {
            "vegetables" -> Color.parseColor("#9ACD32")
            "meat" -> Color.parseColor("#A52A2A")
            "dairy" -> Color.parseColor("#C0C0C0")
            "greens" -> Color.parseColor("#808000")
            "fruits" -> Color.parseColor("#FF0000")
            "fish" -> Color.parseColor("#008080")
            "bakery" -> Color.parseColor("#CD853F")
            "sweets" -> Color.parseColor("#DA70D6")
            "grocery" -> Color.parseColor("#FFE4B5")
            "spices" -> Color.parseColor("#800000")
            "other" -> Color.parseColor("#696969")
            else -> Color.parseColor("#FFFFFF")
        }
    }

    private fun buildCategoryChips(view: View, categories: List<com.example.application.data.local.entity.CategoryEntity>, selectedId: Long?) {
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
        val background = GradientDrawable()
        background.cornerRadius = 50f
        background.setColor(ContextCompat.getColor(ctx, if (selected) R.color.purple_primary else R.color.surface_dark))

        val chip = TextView(ctx).apply {
            this.text = text
            textSize = 13f
            setTypeface(androidx.core.content.res.ResourcesCompat.getFont(ctx, R.font.inter_regular))
            setPadding(50, 20, 50, 20)
            setTextColor(ContextCompat.getColor(ctx, if (selected) R.color.text_primary else R.color.text_secondary))
            this.background = background
            setOnClickListener { onClick() }
        }

        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.marginEnd = 12
        chip.layoutParams = params
        return chip
    }

    private fun openProductsSheet() {
        val sheet = ProductsBottomSheet()
        sheet.setOnDoneListener { viewModel.recalculateMissing() }
        sheet.show(parentFragmentManager, "ProductsBottomSheet")
    }
}