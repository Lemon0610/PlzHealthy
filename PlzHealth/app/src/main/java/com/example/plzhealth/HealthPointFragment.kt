package com.example.plzhealth

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.plzhealth.utils.HealthScore
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HealthPointFragment : Fragment() {

    private val viewModel: MealViewModel by activityViewModels()

    private val defaultHealthTips = listOf(
        "가공식품을 선택할 때는 나트륨과 당류 함량을 함께 확인해보세요.",
        "비슷한 식품이라도 영양성분 차이가 있을 수 있습니다.",
        "단백질과 식이섬유를 함께 구성하면 식단 균형에 도움이 됩니다.",
        "식품 선택 시 칼로리뿐 아니라 당류와 포화지방도 함께 확인해보세요.",
        "영양성분표를 확인하는 습관은 균형 잡힌 식단 구성에 도움이 됩니다."
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_healthpoint, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvDate = view.findViewById<TextView>(R.id.tvHealthLogDate)
        val tvAverageScore = view.findViewById<TextView>(R.id.tvAverageHealthScore)
        val tvSummary = view.findViewById<TextView>(R.id.tvHealthSummary)
        val tvRisk = view.findViewById<TextView>(R.id.tvHealthRisk)
        val tvBreakfast = view.findViewById<TextView>(R.id.tvBreakfastLog)
        val tvLunch = view.findViewById<TextView>(R.id.tvLunchLog)
        val tvDinner = view.findViewById<TextView>(R.id.tvDinnerLog)
        val tvTip = view.findViewById<TextView>(R.id.tvHealthLogTip)
        val lineChart = view.findViewById<LineChart>(R.id.lineChart)

        tvDate.text = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))

        viewModel.selectedMeals.observe(viewLifecycleOwner) { meals ->
            if (meals.isEmpty()) {
                tvAverageScore.text = "0점"
                tvSummary.text = "아직 기록된 식단이 없습니다. 식품을 추가하면 건강점수 일지가 표시됩니다."
                tvRisk.text = "현재 특별한 위험 요소가 없습니다."
                tvBreakfast.text = "아침 | 기록 없음"
                tvLunch.text = "점심 | 기록 없음"
                tvDinner.text = "저녁 | 기록 없음"
                tvTip.text = defaultHealthTips.random()

                setupChart(lineChart, listOf(0), listOf("오늘"), showValues = false)
                return@observe
            }

            val averageScore = meals.map { calculateFoodScore(it) }
                .average()
                .toInt()

            val breakfastMeals = meals.filter { it.mealType == "아침" }
            val lunchMeals = meals.filter { it.mealType == "점심" }
            val dinnerMeals = meals.filter { it.mealType == "저녁" }

            val chartData = buildMealChartData(
                breakfastMeals = breakfastMeals,
                lunchMeals = lunchMeals,
                dinnerMeals = dinnerMeals
            )

            tvAverageScore.text = "${averageScore}점"
            tvSummary.text = getSummaryMessage(averageScore)
            tvRisk.text = getRiskMessage(meals)
            tvBreakfast.text = makeMealLogText("아침", breakfastMeals)
            tvLunch.text = makeMealLogText("점심", lunchMeals)
            tvDinner.text = makeMealLogText("저녁", dinnerMeals)
            tvTip.text = getHealthTipByMeals(meals, averageScore)

            setupChart(lineChart, chartData.scores, chartData.labels, showValues = true)
        }
    }

    private data class ChartData(
        val scores: List<Int>,
        val labels: List<String>
    )

    private fun buildMealChartData(
        breakfastMeals: List<SelectedMeal>,
        lunchMeals: List<SelectedMeal>,
        dinnerMeals: List<SelectedMeal>
    ): ChartData {
        val scores = mutableListOf<Int>()
        val labels = mutableListOf<String>()

        getMealAverageScore(breakfastMeals)?.let {
            scores.add(it)
            labels.add("아침")
        }

        getMealAverageScore(lunchMeals)?.let {
            scores.add(it)
            labels.add("점심")
        }

        getMealAverageScore(dinnerMeals)?.let {
            scores.add(it)
            labels.add("저녁")
        }

        return ChartData(scores, labels)
    }

    private fun calculateFoodScore(selectedMeal: SelectedMeal): Int {
        return HealthScore.calculateScore(
            sodium = selectedMeal.food.sodium,
            sugar = selectedMeal.food.sugar,
            saturatedFat = selectedMeal.food.saturatedFat,
            protein = selectedMeal.food.protein,
            fiber = selectedMeal.food.fiber,
            kcal = selectedMeal.food.kcal
        )
    }

    private fun getMealAverageScore(meals: List<SelectedMeal>): Int? {
        if (meals.isEmpty()) return null

        return meals.map { calculateFoodScore(it) }
            .average()
            .toInt()
    }

    private fun setupChart(
        chart: LineChart,
        scores: List<Int>,
        labels: List<String>,
        showValues: Boolean
    ) {
        val entries = scores.mapIndexed { index, score ->
            Entry(index.toFloat(), score.toFloat())
        }

        val dataSet = LineDataSet(entries, "건강점수").apply {
            color = Color.parseColor("#4CAF50")
            lineWidth = 3f

            setCircleColor(Color.parseColor("#4CAF50"))
            circleRadius = 4.5f
            circleHoleRadius = 2.2f

            setDrawValues(showValues)
            valueTextSize = 9f
            valueTextColor = Color.parseColor("#333333")
            valueFormatter = object : ValueFormatter() {
                override fun getPointLabel(entry: Entry?): String {
                    return "${entry?.y?.toInt() ?: 0}점"
                }
            }

            setDrawFilled(true)
            fillColor = Color.parseColor("#A5D6A7")
            fillAlpha = 55

            mode = LineDataSet.Mode.LINEAR
        }

        chart.data = LineData(dataSet)

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisRight.isEnabled = false

        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.axisMaximum = 100f
        chart.axisLeft.textSize = 9f
        chart.axisLeft.xOffset = 8f
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisLeft.setDrawGridLines(true)

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.granularity = 1f
        chart.xAxis.axisMinimum = -0.25f
        chart.xAxis.axisMaximum = if (scores.size == 1) {
            0.25f
        } else {
            scores.size - 1 + 0.25f
        }
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.xAxis.textSize = 10f
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return labels.getOrNull(index) ?: ""
            }
        }

        chart.setExtraOffsets(28f, 38f, 28f, 10f)
        chart.setTouchEnabled(false)
        chart.setPinchZoom(false)

        chart.animateY(700)
        chart.invalidate()
    }

    private fun makeMealLogText(
        mealName: String,
        meals: List<SelectedMeal>
    ): String {
        if (meals.isEmpty()) {
            return "$mealName | 기록 없음"
        }

        val averageScore = meals.map { calculateFoodScore(it) }
            .average()
            .toInt()

        val foodNames = meals.joinToString(", ") {
            it.food.name
        }

        return "$mealName | ${averageScore}점 | $foodNames"
    }

    private fun getSummaryMessage(score: Int): String {
        return when {
            score >= 80 -> "현재 기록된 식품 기준으로 건강점수가 높은 편입니다."
            score >= 60 -> "현재 기록된 식품 기준으로 평균 건강점수를 표시합니다."
            else -> "현재 기록된 식품 기준으로 나트륨, 당류, 지방 섭취를 확인해보세요."
        }
    }

    private fun getRiskMessage(meals: List<SelectedMeal>): String {
        val totalSodium = meals.sumOf { it.food.sodium }
        val totalSugar = meals.sumOf { it.food.sugar }
        val totalKcal = meals.sumOf { it.food.kcal }
        val totalSaturatedFat = meals.sumOf { it.food.saturatedFat }

        return when {
            totalSodium >= 2000 -> "나트륨 섭취량이 높은 편입니다."
            totalSugar >= 50 -> "당류 섭취량이 높은 편입니다."
            totalSaturatedFat >= 15 -> "포화지방 섭취량을 확인해볼 필요가 있습니다."
            totalKcal >= 2000 -> "총 칼로리가 높은 편입니다."
            else -> "현재 특별한 위험 요소가 없습니다."
        }
    }

    private fun getHealthTipByMeals(
        meals: List<SelectedMeal>,
        averageScore: Int
    ): String {
        val totalSodium = meals.sumOf { it.food.sodium }
        val totalSugar = meals.sumOf { it.food.sugar }
        val totalKcal = meals.sumOf { it.food.kcal }
        val totalProtein = meals.sumOf { it.food.protein }
        val totalFiber = meals.sumOf { it.food.fiber }

        return when {
            totalSodium >= 2000 ->
                "오늘은 나트륨 섭취량이 높은 편입니다. 다음 식사에서는 나트륨 함량이 낮은 식품이나 신선식품 위주로 균형을 맞춰보세요."

            totalSugar >= 50 ->
                "오늘은 당류 섭취량이 높은 편입니다. 식품 선택 시 당 함량을 함께 확인하고, 단 음료나 간식류 섭취는 조금 줄여보는 것도 좋습니다."

            totalKcal >= 2000 ->
                "오늘은 전체 칼로리 섭취량이 높은 편입니다. 남은 식사는 가볍고 균형 있게 구성해보세요."

            totalProtein > 0 && totalProtein < 20 ->
                "오늘은 단백질 섭취가 부족할 수 있습니다. 단백질이 포함된 식품을 함께 선택하면 식단 균형에 도움이 될 수 있습니다."

            totalFiber > 0 && totalFiber < 5 ->
                "오늘은 식이섬유 섭취가 부족할 수 있습니다. 채소, 곡류, 식이섬유가 포함된 식품을 함께 고려해보세요."

            averageScore >= 80 ->
                "오늘 식단은 전반적으로 균형이 좋은 편입니다. 지금처럼 영양성분을 함께 확인하며 식품을 선택해보세요."

            else ->
                "비슷한 식품이라도 영양성분 차이가 있을 수 있습니다. 식품 선택 시 나트륨, 당류, 포화지방 함량을 함께 비교해보세요."
        }
    }
}