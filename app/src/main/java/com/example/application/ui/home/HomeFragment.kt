package com.example.application.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.application.R
import com.example.application.utils.TextUtils.capitalize
import com.example.application.utils.XpManager
import com.example.application.viewmodels.HomeViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Сбрасываем лимит и загружаем данные
        viewModel.resetLimit()
        viewModel.recalculateMissing()

        val mainRecycler = view.findViewById<RecyclerView>(R.id.mainRecycler)
        val fabTop = view.findViewById<FloatingActionButton>(R.id.fabScrollTop)

        // 1. СНАЧАЛА УСТАНАВЛИВАЕМ LAYOUT MANAGER (это предотвращает вылет!)
        mainRecycler.layoutManager = LinearLayoutManager(requireContext())

        // 2. ТЕПЕРЬ НАДУВАЕМ ШАПКУ (передаем mainRecycler, а не null, чтобы она заняла всю ширину!)
        val headerView = layoutInflater.inflate(R.layout.header_home, mainRecycler, false)

        // 3. Настраиваем элементы внутри шапки
        setupHeader(headerView)

        // 4. Адаптер для рецептов
        val recipeAdapter = RecipeAdapter(
            onClick = { recipe ->
                val bundle = Bundle().apply { putLong("recipeId", recipe.id) }
                findNavController().navigate(R.id.nav_recipe_detail, bundle)
            }
        )

        // 5. Склеиваем шапку и рецепты
        val headerAdapter = HeaderAdapter(headerView)
        val concatAdapter = ConcatAdapter(headerAdapter, recipeAdapter)

        // 6. Устанавливаем адаптер
        mainRecycler.adapter = concatAdapter

        // 7. Бесшовный бесконечный скролл
        mainRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // Кнопка наверх
                if (dy > 0 && recyclerView.computeVerticalScrollOffset() > 500) fabTop.show() else fabTop.hide()

                // Дозагрузка (только когда листаем вниз)
                if (dy > 0) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    // Если до конца списка осталось 3 элемента — грузим еще
                    if (lastVisibleItem >= totalItemCount - 3) {
                        val currentList = viewModel.recipes.value
                        val currentLimit = viewModel.displayLimit.value

                        // Защита от спама дозагрузкой
                        if (currentList.size >= currentLimit) {
                            viewModel.loadMore()
                        }
                    }
                }
            }
        })

        fabTop.setOnClickListener { mainRecycler.smoothScrollToPosition(0) }

        // FAB
        view.findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
            bottomNav.selectedItemId = R.id.nav_add_recipe
        }

        // Подписки на данные
        setupObservers(headerView, recipeAdapter)
    }

    private fun setupHeader(headerView: View) {
        // Поиск
        val searchInput = headerView.findViewById<EditText>(R.id.searchInput)
        searchInput.doOnTextChanged { text, _, _, _ ->
            viewModel.resetLimit()
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }

        // Фильтры
        headerView.findViewById<View>(R.id.filterBtn).setOnClickListener {
            val sheet = FilterBottomSheet()
            sheet.currentSort = viewModel.sortType.value
            sheet.currentMinMode = viewModel.minPurchasesMode.value
            sheet.currentAscending = viewModel.isAscending.value

            sheet.onApply = { sort, minMode, isAscending ->
                viewModel.resetLimit()
                viewModel.setSortType(sort)
                viewModel.setAscending(isAscending)
                viewModel.setMinPurchasesMode(minMode)
            }
            sheet.show(parentFragmentManager, "FilterBottomSheet")
        }

        // Ингредиенты
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val db = com.example.application.data.local.AppDatabase.getInstance(requireContext())

                db.ingredientDao().getAvailableIngredientsFlow().collect { ings ->
                    val ingredientsRow = headerView.findViewById<LinearLayout>(R.id.ingredientsRow)
                    ingredientsRow.removeAllViews()

                    // Кнопка "Добавить"
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

                    // Сами ингредиенты
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
    }

    private fun setupObservers(headerView: View, adapter: RecipeAdapter) {
        // Рецепты
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recipes.collect { list -> adapter.submitList(list) }
            }
        }

        // XP
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userStats.collect { stats ->
                    val s = stats ?: com.example.application.data.local.entity.UserStatsEntity()
                    headerView.findViewById<TextView>(R.id.levelBadge).text = "${XpManager.levelName(s.level)} ${s.level} ур."
                    val (inLevel, levelRange, _) = XpManager.progressFor(s)
                    headerView.findViewById<TextView>(R.id.xpCurrent).text = "$inLevel XP"
                    headerView.findViewById<TextView>(R.id.xpTotal).text = "$levelRange XP"
                    val progress = if (levelRange > 0) (inLevel * 100) / levelRange else 0
                    headerView.findViewById<android.widget.ProgressBar>(R.id.xpProgress).progress = progress
                    val dailyMax = XpManager.dailyMaxXp()
                    headerView.findViewById<TextView>(R.id.xpTodayText).text = "Сегодня: +${s.dailyXpGained}/$dailyMax XP"
                }
            }
        }

        // Категории (чипсы)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { categories ->
                    buildCategoryChips(headerView, categories, viewModel.selectedCategory.value)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedCategory.collect { selectedId ->
                    buildCategoryChips(headerView, viewModel.categories.value, selectedId)
                }
            }
        }

        // Достижения
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

    private fun getCategoryIcon(category: String): Int {
        val c = category.lowercase()
        return when {
            c.contains("veg") || c.contains("овощ") -> R.drawable.ic_cat_vegetables
            c.contains("fru") || c.contains("фрукт") || c.contains("ягод") -> R.drawable.ic_cat_fruits
            c.contains("meat") || c.contains("мясо") || c.contains("птиц") -> R.drawable.ic_cat_meat
            c.contains("fish") || c.contains("рыб") -> R.drawable.ic_cat_fish
            c.contains("dairy") || c.contains("молоч") || c.contains("яйц") -> R.drawable.ic_cat_dairy
            c.contains("groc") || c.contains("бакал") || c.contains("круп") -> R.drawable.ic_cat_grocery
            c.contains("bak") || c.contains("хлеб") || c.contains("выпеч") -> R.drawable.ic_cat_bakery
            c.contains("green") || c.contains("зелен") -> R.drawable.ic_cat_greens
            c.contains("spic") || c.contains("специ") -> R.drawable.ic_cat_spices
            c.contains("sweet") || c.contains("слад") -> R.drawable.ic_cat_sweets
            else -> R.drawable.ic_cat_other
        }
    }

    private fun getCategoryColor(category: String): Int {
        val c = category.lowercase()
        return when {
            c.contains("veg") || c.contains("овощ") || c.contains("green") || c.contains("зелен") || c.contains("fru") || c.contains("фрукт") || c.contains("ягод") -> Color.parseColor("#4CAF50")
            c.contains("meat") || c.contains("мясо") || c.contains("fish") || c.contains("рыб") -> Color.parseColor("#F44336")
            c.contains("dairy") || c.contains("молоч") || c.contains("яйц") -> Color.parseColor("#03A9F4")
            c.contains("groc") || c.contains("бакал") || c.contains("bak") || c.contains("хлеб") || c.contains("выпеч") -> Color.parseColor("#FF9800")
            c.contains("spic") || c.contains("специ") || c.contains("sweet") || c.contains("слад") -> Color.parseColor("#E91E63")
            else -> Color.parseColor("#9C27B0")
        }
    }

    private fun buildCategoryChips(headerView: View, categories: List<com.example.application.data.local.entity.CategoryEntity>, selectedId: Long?) {
        val chipsRow = headerView.findViewById<LinearLayout>(R.id.chipsRow)
        chipsRow.removeAllViews()

        chipsRow.addView(createChip("Все", selected = selectedId == null) {
            viewModel.resetLimit()
            viewModel.selectCategory(null)
        })

        categories.forEach { category ->
            chipsRow.addView(createChip(category.name, selected = selectedId == category.id) {
                viewModel.resetLimit()
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
            this.textSize = 13f
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