package com.example.plzhealth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.plzhealth.utils.HealthScore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HealthPointFragment : Fragment() {

    private val viewModel: MealViewModel by activityViewModels()

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

        val today = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))
        tvDate.text = today

        viewModel.selectedMeals.observe(viewLifecycleOwner) { meals ->
            if (meals.isEmpty()) {
                tvAverageScore.text = "0점"
                tvSummary.text = "아직 기록된 식단이 없습니다. 식품을 추가하면 건강점수 일지가 표시됩니다."
                tvRisk.text = "현재 특별한 위험 요소가 없습니다."

                tvBreakfast.text = "아침  |  기록 없음"
                tvLunch.text = "점심  |  기록 없음"
                tvDinner.text = "저녁  |  기록 없음"

                tvTip.text = "식품을 추가하면 오늘의 식단을 바탕으로 건강 팁을 확인할 수 있습니다."
                return@observe
            }

            val scores = meals.map { selectedMeal ->
                calculateFoodScore(selectedMeal)
            }

            val averageScore = scores.sum() / scores.size

            tvAverageScore.text = "${averageScore}점"
            tvSummary.text = getSummaryMessage(averageScore)
            tvRisk.text = getRiskMessage(meals)

            val breakfastMeals = meals.filter { it.mealType == "아침" }
            val lunchMeals = meals.filter { it.mealType == "점심" }
            val dinnerMeals = meals.filter { it.mealType == "저녁" }

            tvBreakfast.text = makeMealLogText("아침", breakfastMeals)
            tvLunch.text = makeMealLogText("점심", lunchMeals)
            tvDinner.text = makeMealLogText("저녁", dinnerMeals)

            tvTip.text = getHealthTipByMeals(meals, averageScore)
        }
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

    private fun makeMealLogText(
        mealName: String,
        meals: List<SelectedMeal>
    ): String {
        if (meals.isEmpty()) {
            return "$mealName  |  기록 없음"
        }

        val averageScore = meals.map { selectedMeal ->
            calculateFoodScore(selectedMeal)
        }.average().toInt()

        val foodNames = meals.joinToString(", ") {
            it.food.name
        }

        return "$mealName  |  ${averageScore}점  |  $foodNames"
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
            totalSodium >= 2000 ->
                "나트륨 섭취량이 높은 편입니다."

            totalSugar >= 50 ->
                "당류 섭취량이 높은 편입니다."

            totalSaturatedFat >= 15 ->
                "포화지방 섭취량을 확인해볼 필요가 있습니다."

            totalKcal >= 2000 ->
                "총 칼로리가 높은 편입니다."

            else ->
                "현재 특별한 위험 요소가 없습니다."
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
                "오늘은 나트륨 섭취량이 높은 편입니다. 다음 식사에서는 나트륨이 낮은 식품을 선택해보세요."

            totalSugar >= 50 ->
                "오늘은 당류 섭취량이 높은 편입니다. 단 음료나 간식 섭취를 조금 줄여보세요."

            totalKcal >= 2000 ->
                "오늘 총 칼로리가 높은 편입니다. 남은 식사는 가볍게 조절해보세요."

            totalProtein < 20 ->
                "오늘은 단백질 섭취가 부족할 수 있습니다. 단백질이 포함된 식품을 함께 선택해보세요."

            totalFiber < 5 ->
                "오늘은 식이섬유가 부족할 수 있습니다. 채소, 곡류, 식이섬유가 있는 식품을 함께 고려해보세요."

            averageScore >= 80 ->
                "좋은 흐름이에요. 지금처럼 균형 잡힌 식품 선택을 유지해보세요."

            else ->
                "비슷한 식품이라면 나트륨과 당류가 낮은 제품을 선택해보세요."
        }
    }
}