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
import android.graphics.Color
import android.view.Gravity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.widget.GridLayout

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
                    val s = stats ?: com.example.application.data.local.entity.UserStatsEntity()

                    // Уровень
                    val levelName = XpManager.levelName(s.level)
                    view.findViewById<TextView>(R.id.levelTitle).text = "🔥 $levelName ${s.level} ур."

                    // Прогресс
                    val (inLevel, levelRange, _) = XpManager.progressFor(s)
                    view.findViewById<TextView>(R.id.levelXpIn).text = "$inLevel XP"
                    view.findViewById<TextView>(R.id.levelXpTotal).text = "$levelRange XP"
                    val levelProgress = if (levelRange > 0) (inLevel * 100) / levelRange else 0
                    view.findViewById<ProgressBar>(R.id.levelProgress).progress = levelProgress

                    // Всего приготовлено
                    view.findViewById<TextView>(R.id.cookedTotal).text =
                        "🍳 Всего приготовлено: ${s.dishesCooked} блюд"

                    // Дневной лимит
                    val dailyMax = XpManager.dailyMaxXp()
                    view.findViewById<TextView>(R.id.dailyXpBadge).text =
                        "${s.dailyXpGained}/$dailyMax"
                    val dailyProgress = if (dailyMax > 0) (s.dailyXpGained * 100) / dailyMax else 0
                    view.findViewById<ProgressBar>(R.id.dailyProgress).progress = dailyProgress

                    // Список задач
                    view.findViewById<TextView>(R.id.taskLogin).text =
                        "✅ Ежедневный вход — ${s.dailyLogins}/1"
                    view.findViewById<TextView>(R.id.taskProducts).text =
                        "⚠️ Добавление продуктов — ${s.dailyProductsAdded}/5"
                    view.findViewById<TextView>(R.id.taskCooked).text =
                        "✅ Приготовлено — ${s.dailyDishesCooked}/3"
                    view.findViewById<TextView>(R.id.taskCreated).text =
                        "⏸ Создание рецептов — ${s.dailyRecipesCreated}/2"

                    // Пустое состояние
                    val empty = view.findViewById<LinearLayout>(R.id.emptyBlock)
                    empty.visibility = if (s.dishesCooked == 0) View.VISIBLE else View.GONE
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

        // Круговая диаграмма
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categorySlices.collect { slices ->
                    renderPieChart(view, slices)
                }
            }
        }

        // Столбчатая диаграмма
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weekBars.collect { bars ->
                    renderBarChart(view, bars)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.unlockedAchievements.collect { unlocked ->
                    renderAchievements(view, unlocked)
                }
            }
        }
    }

    private fun renderPieChart(view: View, slices: List<com.example.application.viewmodels.CategorySlice>) {
        val chart = view.findViewById<PieChart>(R.id.pieChart)
        val legend = view.findViewById<LinearLayout>(R.id.pieLegend)
        legend.removeAllViews()

        if (slices.isEmpty()) {
            chart.clear()
            chart.setNoDataText("Нет данных")
            chart.setNoDataTextColor(0xFF9E9E9E.toInt())
            chart.invalidate()
            return
        }

        val entries = slices.map { PieEntry(it.count.toFloat(), it.name) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = slices.map { it.color }
            setDrawValues(false)
        }
        chart.data = PieData(dataSet).apply {
            setDrawValues(false)
        }
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setDrawEntryLabels(false)
        chart.isRotationEnabled = false
        chart.setHoleColor(0xFF1E1E1E.toInt())
        chart.holeRadius = 45f
        chart.transparentCircleRadius = 0f
        chart.setHoleColor(0xFF1E1E1E.toInt())
        chart.setDrawHoleEnabled(true)
        chart.invalidate()

        // Легенда справа
        val total = slices.sumOf { it.count }
        slices.forEach { slice ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 10, 0, 10)
            }
            val dot = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(20, 20)
                setBackgroundColor(slice.color)
            }
            val name = TextView(requireContext()).apply {
                text = " ${slice.name}"
                setTextColor(0xFF9E9E9E.toInt())
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val pct = if (total > 0) (slice.count * 100 / total) else 0
            val percent = TextView(requireContext()).apply {
                text = "$pct%"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            row.addView(dot)
            row.addView(name)
            row.addView(percent)
            legend.addView(row)
        }
    }

    private fun renderBarChart(view: View, bars: List<Int>) {
        val chart = view.findViewById<BarChart>(R.id.barChart)
        if (bars.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        val entries = bars.mapIndexed { idx, value -> BarEntry(idx.toFloat(), value.toFloat()) }
        val dataSet = BarDataSet(entries, "").apply {
            color = 0xFFFF7043.toInt()
            setDrawValues(true)
            valueTextColor = 0xFFFFFFFF.toInt()
            valueTextSize = 10f
        }
        chart.data = BarData(dataSet).apply { barWidth = 0.6f }

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.axisRight.isEnabled = false
        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.textColor = 0xFF9E9E9E.toInt()
        chart.axisLeft.gridColor = 0xFF2C2C2C.toInt()
        chart.axisLeft.granularity = 1f

        val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        // Определяем день недели первого столбца — 6 дней назад от сегодня
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        // Calendar.DAY_OF_WEEK: 1 = Sunday, 2 = Monday, ... 7 = Saturday
        // Наш массив days: 0=Пн, 1=Вт, ..., 6=Вс
        val todayIdx = when (today) {
            java.util.Calendar.MONDAY -> 0
            java.util.Calendar.TUESDAY -> 1
            java.util.Calendar.WEDNESDAY -> 2
            java.util.Calendar.THURSDAY -> 3
            java.util.Calendar.FRIDAY -> 4
            java.util.Calendar.SATURDAY -> 5
            else -> 6  // SUNDAY
        }
        val firstDayIdx = (todayIdx - 6 + 7) % 7
        val xLabels = (0..6).map { days[(firstDayIdx + it) % 7] }

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            textColor = 0xFF9E9E9E.toInt()
            granularity = 1f
            valueFormatter = IndexAxisValueFormatter(xLabels)
        }

        chart.invalidate()
    }

    private fun renderAchievements(view: View, unlockedIds: List<String>) {
        val grid = view.findViewById<LinearLayout>(R.id.achievementsGrid)
        grid.removeAllViews()

        val all = com.example.application.utils.AchievementManager.TITLES

        // Разбиваем на ряды по 3
        val entries = all.entries.toList()
        var index = 0
        while (index < entries.size) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            repeat(3) {
                val (id, title) = entries.getOrNull(index) ?: run {
                    // Пустая ячейка для выравнивания
                    val spacer = View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    }
                    row.addView(spacer)
                    return@repeat
                }

                val isUnlocked = unlockedIds.contains(id)

                val cell = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    setPadding(24, 32, 24, 32)
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    ).apply { marginEnd = 16 }
                    setBackgroundColor(
                        if (isUnlocked) 0x26FF7043.toInt() else 0xFF252525.toInt()
                    )
                    alpha = if (isUnlocked) 1f else 0.55f
                }

                val name = TextView(requireContext()).apply {
                    text = title
                    textSize = 11f
                    gravity = android.view.Gravity.CENTER
                    setTextColor(if (isUnlocked) 0xFFFF7043.toInt() else 0xFF9E9E9E.toInt())
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                cell.addView(name)

                if (!isUnlocked) {
                    val lock = TextView(requireContext()).apply {
                        text = "🔒"
                        textSize = 12f
                        gravity = android.view.Gravity.CENTER
                        setPadding(0, 6, 0, 0)
                    }
                    cell.addView(lock)
                }

                row.addView(cell)
                index++
            }

            grid.addView(row)

            // Отступ между рядами
            val gap = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 16
                )
            }
            grid.addView(gap)
        }
    }
}