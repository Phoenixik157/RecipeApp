package com.example.application.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.CategoryEntity
import com.example.application.data.local.entity.RecipeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.application.utils.XpManager
import com.example.application.utils.AchievementManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RecipeWithMissing(
    val recipe: RecipeEntity,
    val missingCount: Int,
    val totalCount: Int
)

enum class SortType { NAME, TIME, COMPLEXITY }

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _selectedCategory = MutableStateFlow<Long?>(null)
    val selectedCategory: StateFlow<Long?> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Режим «минимум покупок» — включается, если выбраны продукты
    private val _minPurchasesMode = MutableStateFlow(false)
    val minPurchasesMode: StateFlow<Boolean> = _minPurchasesMode

    /** Есть ли у пользователя отмеченные продукты вообще. */
    private val _userHasProducts = MutableStateFlow(false)
    val userHasProducts: StateFlow<Boolean> = _userHasProducts

    // Все рецепты + посчитанные «не хватает»
    private val _allRecipesWithMissing = MutableStateFlow<List<RecipeWithMissing>>(emptyList())

    private val _sortType = MutableStateFlow(SortType.NAME)
    val sortType: StateFlow<SortType> = _sortType

    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending

    // Лимит отображения (сколько рецептов показываем за раз)
    private val _displayLimit = MutableStateFlow(5)
    val displayLimit: StateFlow<Int> = _displayLimit

    // Функция для загрузки следующей порции
    fun loadMore() {
        _displayLimit.value = _displayLimit.value + 5
    }

    // Функция для сброса (при поиске или фильтрации)
    fun resetLimit() {
        _displayLimit.value = 5
    }

    val categories: StateFlow<List<CategoryEntity>> = db.categoryDao()
        .getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Итоговый список для отображения: фильтр по категории + поиск + сортировка.
     */
    val recipes: StateFlow<List<RecipeWithMissing>> = combine(
        _allRecipesWithMissing,
        _selectedCategory,
        _searchQuery,
        _minPurchasesMode,
        _sortType,
        _isAscending,
        _displayLimit // <--- ВАЖНО: добавили в combine, чтобы список обновлялся
    ) { values ->
        val all = values[0] as List<RecipeWithMissing>
        val categoryId = values[1] as Long?
        val query = values[2] as String
        val minMode = values[3] as Boolean
        val sort = values[4] as SortType
        val asc = values[5] as Boolean
        val limit = values[6] as Int

        val filtered = all.filter { item ->
            val r = item.recipe
            val matchesCategory = categoryId == null || r.categoryId == categoryId
            val matchesQuery = query.isEmpty() || r.title.contains(query, true)
            matchesCategory && matchesQuery
        }

        // Создаем базовый компаратор для выбранной сортировки
        val comparator = when (sort) {
            SortType.NAME -> compareBy<RecipeWithMissing> { it.recipe.title.lowercase() }
            SortType.TIME -> compareBy<RecipeWithMissing> { it.recipe.cookingTimeMinutes }
            SortType.COMPLEXITY -> compareBy<RecipeWithMissing> { it.recipe.complexity }
        }

        // Меняем направление, если нужно
        val finalComparator = if (asc) comparator else comparator.reversed()

        // Применяем сортировку
        val sorted = if (minMode) {
            filtered.sortedWith(compareBy<RecipeWithMissing> { it.missingCount }.then(finalComparator))
        } else {
            filtered.sortedWith(finalComparator)
        }

        // Обрезаем список по лимиту отображения!
        sorted.take(limit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var hasLoadedOnce = false

    fun ensureLoaded() {
        if (!hasLoadedOnce) {
            hasLoadedOnce = true
            recalculateMissing()
        }
    }

//    init {
//        // Отслеживаем только изменение КОЛИЧЕСТВА рецептов (добавление / удаление),
//        // чтобы не пересчитывать всю базу при каждом редактировании.
//        viewModelScope.launch {
//            db.recipeDao().getRecipeCountFlow().collect {
//                if (hasLoadedOnce) {
//                    recalculateMissing()
//                }
//            }
//        }
//    }

    /** Пересчитывает, сколько ингредиентов не хватает для каждого рецепта. */
    fun recalculateMissing() {
        viewModelScope.launch {
            // Тяжелые вычисления уводим в фоновый поток
            val result = withContext(Dispatchers.Default) {
                val userIngredientIds = db.userProductDao().getAllIds().toSet()
                val allRecipes = db.recipeDao().getAllOnce()
                val allLinks = db.recipeIngredientDao().getAllLinksOnce()
                val linksByRecipe = allLinks.groupBy { it.recipeId }

                allRecipes.map { recipe ->
                    val links = linksByRecipe[recipe.id].orEmpty()
                    val total = links.size
                    val have = links.count { it.ingredientId in userIngredientIds }
                    RecipeWithMissing(recipe, total - have, total)
                }
            }

            // Возвращаемся на главный поток для обновления UI
            _minPurchasesMode.value = db.userProductDao().getAllIds().isNotEmpty()
            _allRecipesWithMissing.value = result
        }
    }

    /** Переключение режима вручную (по клику на чипс). */
    fun setMinPurchasesMode(enabled: Boolean) {
        if (_minPurchasesMode.value == enabled) return
        _minPurchasesMode.value = enabled

        if (enabled) {
            viewModelScope.launch {
                db.usageCounterDao().increment(AchievementManager.KEY_MIN_PURCHASES)
                XpManager.award(db, XpManager.ACTION_MIN_PURCHASES)
                AchievementManager.checkAll(db)
            }
        }
    }

    fun selectCategory(id: Long?) { _selectedCategory.value = id }
    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun setAscending(asc: Boolean) {
        _isAscending.value = asc
    }

    fun setSortType(type: SortType) {
        _sortType.value = type
    }

    val userStats: StateFlow<com.example.application.data.local.entity.UserStatsEntity?> =
        db.userStatsDao().observe()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}