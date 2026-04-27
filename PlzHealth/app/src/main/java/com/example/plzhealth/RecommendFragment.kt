package com.example.plzhealth

import android.graphics.Color
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

        val baseCategory    = arguments?.getString("category") ?: ""
        val baseName        = arguments?.getString("baseName") ?: ""
        val baseSodium      = arguments?.getDouble("sodium") ?: 0.0
        val baseSugar       = arguments?.getDouble("sugar") ?: 0.0
        val baseSaturatedFat = arguments?.getDouble("saturatedFat") ?: 0.0
        val baseProtein     = arguments?.getDouble("protein") ?: 0.0
        val baseFiber       = arguments?.getDouble("fiber") ?: 0.0
        val baseKcal        = arguments?.getDouble("kcal") ?: 0.0

        // 기준 식품 안내 카드에 식품명 표시
        view.findViewById<TextView>(R.id.tvPageDesc).text = "$baseName 대신"

        val foods = CsvFoodReader.loadFoods(requireContext())

        val baseScore = HealthScore.calculateScore(baseSodium, baseSugar, baseSaturatedFat, baseProtein, baseFiber, baseKcal)

        val recommended = foods
            .filter { it.category == baseCategory && it.name != baseName }
            .map { food ->
                val score = HealthScore.calculateScore(food.sodium, food.sugar, food.saturatedFat, food.protein, food.fiber, food.kcal)
                Pair(food, score)
            }
            .filter { it.second > baseScore }
            .sortedByDescending { it.second }
            .take(3)

        // 카드 슬롯 3개
        val slots = listOf(
            Triple(R.id.tvFood1, R.id.tvReason1, R.id.tvScoreDiff1),
            Triple(R.id.tvFood2, R.id.tvReason2, R.id.tvScoreDiff2),
            Triple(R.id.tvFood3, R.id.tvReason3, R.id.tvScoreDiff3)
        )
        val subSlots = listOf(R.id.tvReason1Sub, R.id.tvReason2Sub, R.id.tvReason3Sub)

        if (recommended.isEmpty()) {
            // 추천 없을 때: 첫 번째 카드에 안내 메시지
            view.findViewById<TextView>(R.id.tvFood1).text = "없음"
            view.findViewById<TextView>(R.id.tvReason1).text = "추천 식품 없음"
            view.findViewById<TextView>(R.id.tvReason1Sub).text =
                "현재 식품(${baseName})보다 건강점수가 높은 대체 식품이 같은 카테고리에 없습니다."
            view.findViewById<TextView>(R.id.tvScoreDiff1).text = ""
            // 2, 3번 카드 숨기기
            hideSlot(view, slots[1], subSlots[1])
            hideSlot(view, slots[2], subSlots[2])
            return
        }

        // 추천 결과 바인딩
        slots.forEachIndexed { i, (scoreId, nameId, diffId) ->
            if (i < recommended.size) {
                val (food, score) = recommended[i]
                val diff = score - baseScore
                val (grade, color) = gradeInfo(score)

                view.findViewById<TextView>(scoreId).apply {
                    text = score.toString()
                    setTextColor(Color.parseColor(color))
                }
                // 등급 텍스트 (score 뷰 바로 아래 static "우수" TextView는 동적으로 못 바꾸므로 생략)

                view.findViewById<TextView>(nameId).text = food.name
                view.findViewById<TextView>(subSlots[i]).text =
                    getReason(food, baseSodium, baseSugar, baseSaturatedFat, baseProtein, baseFiber)
                view.findViewById<TextView>(diffId).text = "+${diff}점"
            } else {
                hideSlot(view, slots[i], subSlots[i])
            }
        }
    }

    private fun hideSlot(view: View, slot: Triple<Int,Int,Int>, subId: Int) {
        view.findViewById<TextView>(slot.first).text = ""
        view.findViewById<TextView>(slot.second).text = ""
        view.findViewById<TextView>(subId).text = ""
        view.findViewById<TextView>(slot.third).text = ""
    }

    private fun gradeInfo(score: Int): Pair<String, String> = when {
        score >= 80 -> Pair("우수", "#4CAF50")
        score >= 60 -> Pair("양호", "#FF9800")
        score >= 40 -> Pair("보통", "#FF9800")
        else        -> Pair("주의", "#F44336")
    }

    private fun getReason(rec: FoodItem, bSodium: Double, bSugar: Double, bFat: Double, bProtein: Double, bFiber: Double): String {
        return when {
            rec.sodium < bSodium       -> "나트륨이 낮아 더 건강한 선택입니다."
            rec.sugar < bSugar         -> "당류가 낮아 대체 식품으로 적절합니다."
            rec.saturatedFat < bFat    -> "포화지방이 낮아 심혈관 건강에 도움을 줍니다."
            rec.fiber > bFiber         -> "식이섬유가 풍부하여 추천합니다."
            rec.protein > bProtein     -> "단백질 함량이 높아 근육 건강에 좋습니다."
            else                       -> "영양 성분 구성이 더 균형 잡혀 있습니다."
        }
    }
}