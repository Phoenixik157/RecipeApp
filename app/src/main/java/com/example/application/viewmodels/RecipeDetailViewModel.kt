package com.example.application.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.data.local.AppDatabase
import com.example.application.data.local.entity.IngredientEntity
import com.example.application.data.local.entity.RecipeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.application.utils.XpManager
import com.example.application.data.local.entity.CookingHistoryEntity
import com.example.application.data.local.entity.UserStatsEntity
import com.example.application.utils.AchievementManager
import com.example.application.utils.ReplacementManager

data class IngredientWithQty(
    val ingredient: IngredientEntity,
    val quantity: Double,
    val unit: String,
    val haveIt: Boolean,      // есть ли у пользователя в холодильнике
    val replacement: String? = null
)

class RecipeDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _recipe = MutableStateFlow<RecipeEntity?>(null)
    val recipe: StateFlow<RecipeEntity?> = _recipe

    private val _ingredients = MutableStateFlow<List<IngredientWithQty>>(emptyList())
    val ingredients: StateFlow<List<IngredientWithQty>> = _ingredients

    private val _servings = MutableStateFlow(2)
    val servings: StateFlow<Int> = _servings

    private val _categoryName = MutableStateFlow("")
    val categoryName: StateFlow<String> = _categoryName

    private var loaded = false

    fun load(recipeId: Long) {
        if (loaded) return
        loaded = true

        viewModelScope.launch {
            val recipe = db.recipeDao().getById(recipeId) ?: return@launch
            _recipe.value = recipe
            _servings.value = recipe.baseServings

            val category = db.categoryDao().getById(recipe.categoryId)
            _categoryName.value = category?.name ?: "Без категории"

            // Получаем ID и НАЗВАНИЯ продуктов из холодильника
            val userIds = db.userProductDao().getAllIdsOnce()
            val userProductsList = db.ingredientDao().getByIds(userIds)
            val userProductNames = userProductsList.map { it.name.lowercase() }.toSet()

            val links = db.recipeIngredientDao().getByRecipeId(recipeId)
            val ingIds = links.map { it.ingredientId }
            val ings = db.ingredientDao().getByIds(ingIds)
            val map = ings.associateBy { it.id }

            _ingredients.value = links.mapNotNull { link ->
                map[link.ingredientId]?.let { ing ->
                    val haveIt = link.ingredientId in userIds
                    var replacement: String? = null

                    // Если ингредиента нет, ищем ему замену
                    if (!haveIt) {
                        replacement = ReplacementManager.findReplacement(
                            ing.name,
                            userProductNames
                        )
                    }

                    IngredientWithQty(
                        ingredient = ing,
                        quantity = link.quantity,
                        unit = link.unit,
                        haveIt = haveIt,
                        replacement = replacement
                    )
                }
            }
        }
    }

    fun increaseServings() { _servings.value = _servings.value + 1 }
    fun decreaseServings() { if (_servings.value > 1) _servings.value = _servings.value - 1 }

    fun awardView() {
        viewModelScope.launch {
            XpManager.award(db, XpManager.ACTION_VIEW_RECIPE)
        }
    }

    suspend fun awardCooked(recipeId: Long, servings: Int): Int {
        val xp = XpManager.award(
            db,
            XpManager.ACTION_COOKED
        )

        // История — всегда
        db.cookingHistoryDao().insert(
            CookingHistoryEntity(
                recipeId = recipeId,
                servings = servings,
                xpEarned = xp
            )
        )

        // Общий счётчик приготовлений — независимо от XP
        val stats = db.userStatsDao().get()
            ?: UserStatsEntity()
        db.userStatsDao().save(stats.copy(dishesCooked = stats.dishesCooked + 1))

        AchievementManager.checkAll(db)

        return xp
    }
}