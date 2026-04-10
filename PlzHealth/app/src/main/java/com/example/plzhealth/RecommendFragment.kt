package com.example.plzhealth

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.plzhealth.data.FoodItem
import com.example.plzhealth.utils.CsvFoodReader
import com.example.plzhealth.utils.HealthScore

// 대체 식품 추천 기능 테스트용 Fragment
// CSV 데이터에서 식품 정보를 읽고 건강점수를 계산한 뒤
// 기준 식품보다 점수가 높은 식품만 같은 카테고리 내에서 추천
class RecommendFragment : Fragment(R.layout.fragment_recommend) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvFood1 = view.findViewById<TextView>(R.id.tvFood1)
        val tvReason1 = view.findViewById<TextView>(R.id.tvReason1)
        val tvFood2 = view.findViewById<TextView>(R.id.tvFood2)
        val tvReason2 = view.findViewById<TextView>(R.id.tvReason2)
        val tvFood3 = view.findViewById<TextView>(R.id.tvFood3)
        val tvReason3 = view.findViewById<TextView>(R.id.tvReason3)

        val foods = CsvFoodReader.loadFoods(requireContext())

        if (foods.isEmpty()) {
            tvFood1.text = "추천 식품 없음"
            tvReason1.text = "불러온 식품 데이터가 없습니다."
            tvFood2.text = ""
            tvReason2.text = ""
            tvFood3.text = ""
            tvReason3.text = ""
            return
        }

        // 기준 식품 1개를 임시로 선택
        // 현재는 첫 번째 식품을 기준으로 사용
        val baseFood = foods.first()

        val baseScore = HealthScore.calculateScore(
            sodium = baseFood.sodium,
            sugar = baseFood.sugar,
            saturatedFat = baseFood.saturatedFat,
            protein = baseFood.protein,
            fiber = baseFood.fiber,
            kcal = baseFood.kcal
        )

        val recommended = foods
            .filter { food ->
                food.category == baseFood.category &&
                        food.name != baseFood.name
            }
            .map { food ->
                val score = HealthScore.calculateScore(
                    sodium = food.sodium,
                    sugar = food.sugar,
                    saturatedFat = food.saturatedFat,
                    protein = food.protein,
                    fiber = food.fiber,
                    kcal = food.kcal
                )
                Pair(food, score)
            }
            .filter { it.second > baseScore }
            .sortedByDescending { it.second }
            .take(3)

        if (recommended.isEmpty()) {
            tvFood1.text = "추천 식품 없음"
            tvReason1.text = "기준 식품보다 건강점수가 높은 대체 식품이 없습니다."
            tvFood2.text = ""
            tvReason2.text = ""
            tvFood3.text = ""
            tvReason3.text = ""
            return
        }

        if (recommended.isNotEmpty()) {
            tvFood1.text = "${recommended[0].first.name} (${recommended[0].second}점)"
            tvReason1.text = getReason(recommended[0].first, baseFood)
        }

        if (recommended.size > 1) {
            tvFood2.text = "${recommended[1].first.name} (${recommended[1].second}점)"
            tvReason2.text = getReason(recommended[1].first, baseFood)
        } else {
            tvFood2.text = ""
            tvReason2.text = ""
        }

        if (recommended.size > 2) {
            tvFood3.text = "${recommended[2].first.name} (${recommended[2].second}점)"
            tvReason3.text = getReason(recommended[2].first, baseFood)
        } else {
            tvFood3.text = ""
            tvReason3.text = ""
        }
    }

    private fun getReason(recommendedFood: FoodItem, baseFood: FoodItem): String {
        return when {
            recommendedFood.sodium < baseFood.sodium ->
                "기준 식품보다 나트륨이 낮아 더 건강한 선택입니다."

            recommendedFood.sugar < baseFood.sugar ->
                "기준 식품보다 당류가 낮아 대체 식품으로 적절합니다."

            recommendedFood.saturatedFat < baseFood.saturatedFat ->
                "기준 식품보다 포화지방이 낮아 추천합니다."

            recommendedFood.fiber > baseFood.fiber ->
                "기준 식품보다 식이섬유가 높아 추천합니다."

            recommendedFood.protein > baseFood.protein ->
                "기준 식품보다 단백질 함량이 높아 추천합니다."

            else ->
                "기준 식품보다 건강점수가 높아 더 적절한 대체 식품입니다."
        }
    }
}