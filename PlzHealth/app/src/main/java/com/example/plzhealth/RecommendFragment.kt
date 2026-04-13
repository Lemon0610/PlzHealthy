package com.example.plzhealth

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.plzhealth.data.FoodItem
import com.example.plzhealth.utils.CsvFoodReader
import com.example.plzhealth.utils.HealthScore

class RecommendFragment : Fragment(R.layout.fragment_recommend) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvFood1 = view.findViewById<TextView>(R.id.tvFood1)
        val tvReason1 = view.findViewById<TextView>(R.id.tvReason1)
        val tvFood2 = view.findViewById<TextView>(R.id.tvFood2)
        val tvReason2 = view.findViewById<TextView>(R.id.tvReason2)
        val tvFood3 = view.findViewById<TextView>(R.id.tvFood3)
        val tvReason3 = view.findViewById<TextView>(R.id.tvReason3)

        val baseCategory = arguments?.getString("category") ?: ""
        val baseName = arguments?.getString("baseName") ?: ""
        val baseSodium = arguments?.getDouble("sodium") ?: 0.0
        val baseSugar = arguments?.getDouble("sugar") ?: 0.0
        val baseSaturatedFat = arguments?.getDouble("saturatedFat") ?: 0.0
        val baseProtein = arguments?.getDouble("protein") ?: 0.0
        val baseFiber = arguments?.getDouble("fiber") ?: 0.0
        val baseKcal = arguments?.getDouble("kcal") ?: 0.0

        val foods = CsvFoodReader.loadFoods(requireContext())

        if (foods.isEmpty()) {
            tvFood1.text = "데이터 로드 실패"
            tvReason1.text = "CSV 파일을 읽어올 수 없습니다."
            return
        }

        val baseScore = HealthScore.calculateScore(
            sodium = baseSodium,
            sugar = baseSugar,
            saturatedFat = baseSaturatedFat,
            protein = baseProtein,
            fiber = baseFiber,
            kcal = baseKcal
        )

        val recommended = foods
            .filter { food ->
                food.category == baseCategory && food.name != baseName
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
            tvReason1.text = "현재 식품(${baseName})보다 건강점수가 높은 대체 식품이 같은 카테고리에 없습니다."
            tvFood2.text = ""
            tvReason2.text = ""
            tvFood3.text = ""
            tvReason3.text = ""
            return
        }

        tvFood1.text = "${recommended[0].first.name} (${recommended[0].second}점)"
        tvReason1.text = getReason(recommended[0].first, baseSodium, baseSugar, baseSaturatedFat, baseProtein, baseFiber)

        if (recommended.size > 1) {
            tvFood2.text = "${recommended[1].first.name} (${recommended[1].second}점)"
            tvReason2.text = getReason(recommended[1].first, baseSodium, baseSugar, baseSaturatedFat, baseProtein, baseFiber)
        } else {
            tvFood2.text = ""
            tvReason2.text = ""
        }

        if (recommended.size > 2) {
            tvFood3.text = "${recommended[2].first.name} (${recommended[2].second}점)"
            tvReason3.text = getReason(recommended[2].first, baseSodium, baseSugar, baseSaturatedFat, baseProtein, baseFiber)
        } else {
            tvFood3.text = ""
            tvReason3.text = ""
        }
    }

    private fun getReason(
        rec: FoodItem,
        bSodium: Double,
        bSugar: Double,
        bFat: Double,
        bProtein: Double,
        bFiber: Double
    ): String {
        return when {
            rec.sodium < bSodium -> "기준 식품보다 나트륨이 낮아 더 건강한 선택입니다."
            rec.sugar < bSugar -> "기준 식품보다 당류가 낮아 대체 식품으로 적절합니다."
            rec.saturatedFat < bFat -> "기준 식품보다 포화지방이 낮아 심혈관 건강에 도움을 줍니다."
            rec.fiber > bFiber -> "기준 식품보다 식이섬유가 풍부하여 추천합니다."
            rec.protein > bProtein -> "기준 식품보다 단백질 함량이 높아 근육 건강에 좋습니다."
            else -> "영양 성분 구성이 기준 식품보다 균형 잡혀 있습니다."
        }
    }
}