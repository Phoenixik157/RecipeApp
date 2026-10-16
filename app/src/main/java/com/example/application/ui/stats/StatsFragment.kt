package com.example.application.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.application.R
import com.example.application.utils.XpManager
import com.example.application.viewmodels.StatsViewModel
import kotlinx.coroutines.launch

class StatsFragment : Fragment() {

    private val viewModel: StatsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Подписка на userStats — уровень, XP, дневной лимит
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userStats.collect { stats ->
                    stats ?: return@collect

                    // Уровень
                    val levelName = XpManager.levelName(stats.level)
                    view.findViewById<TextView>(R.id.levelTitle).text = "🔥 $levelName ${stats.level} ур."

                    // Прогресс
                    val (inLevel, levelRange, _) = XpManager.progressFor(stats)
                    view.findViewById<TextView>(R.id.levelXpIn).text = "$inLevel XP"
                    view.findViewById<TextView>(R.id.levelXpTotal).text = "$levelRange XP"
                    val levelProgress = if (levelRange > 0) (inLevel * 100) / levelRange else 0
                    view.findViewById<ProgressBar>(R.id.levelProgress).progress = levelProgress

                    // Всего приготовлено
                    view.findViewById<TextView>(R.id.cookedTotal).text =
                        "🍳 Всего приготовлено: ${stats.dishesCooked} блюд"

                    // Дневной лимит
                    val dailyMax = XpManager.dailyMaxXp()
                    view.findViewById<TextView>(R.id.dailyXpBadge).text =
                        "${stats.dailyXpGained}/$dailyMax"
                    val dailyProgress = if (dailyMax > 0) (stats.dailyXpGained * 100) / dailyMax else 0
                    view.findViewById<ProgressBar>(R.id.dailyProgress).progress = dailyProgress

                    // Список задач
                    view.findViewById<TextView>(R.id.taskLogin).text =
                        "✅ Ежедневный вход — ${stats.dailyLogins}/1"
                    view.findViewById<TextView>(R.id.taskProducts).text =
                        "⚠️ Добавление продуктов — ${stats.dailyProductsAdded}/5"
                    view.findViewById<TextView>(R.id.taskCooked).text =
                        "✅ Приготовлено — ${stats.dailyDishesCooked}/3"
                    view.findViewById<TextView>(R.id.taskCreated).text =
                        "⏸ Создание рецептов — ${stats.dailyRecipesCreated}/2"

                    // Пустое состояние
                    val empty = view.findViewById<LinearLayout>(R.id.emptyBlock)
                    empty.visibility = if (stats.dishesCooked == 0) View.VISIBLE else View.GONE
                }
            }
        }

        // Быстрые метрики
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.metrics.collect { m ->
                    view.findViewById<TextView>(R.id.metricCategory).text = m.favouriteCategory
                    view.findViewById<TextView>(R.id.metricTime).text =
                        if (m.averageTimeMinutes > 0) "${m.averageTimeMinutes} мин" else "—"
                    view.findViewById<TextView>(R.id.metricComplexity).text = m.favouriteComplexity
                }
            }
        }
    }
}