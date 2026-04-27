package com.example.plzhealth

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.plzhealth.data.AppDatabase
import com.example.plzhealth.data.FoodItem
import com.example.plzhealth.utils.CsvFoodReader
import com.example.plzhealth.utils.HealthScore
import kotlinx.coroutines.launch
import kotlin.math.abs

class RecommendFragment : Fragment(R.layout.fragment_recommend) {

    private val db by lazy { AppDatabase.getDatabase(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val layoutGuideBox = view.findViewById<View>(R.id.layoutGuideBox)
        val tvGuideMessage = view.findViewById<TextView>(R.id.tvGuideMessage)

        val cards = listOf(
            view.findViewById<View>(R.id.cardRec1),
            view.findViewById<View>(R.id.cardRec2),
            view.findViewById<View>(R.id.cardRec3)
        )

        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val baseCategory = arguments?.getString("category") ?: ""
        val baseName = arguments?.getString("baseName") ?: ""
        val baseSodium = arguments?.getDouble("sodium") ?: 0.0
        val baseSugar = arguments?.getDouble("sugar") ?: 0.0
        val baseSaturatedFat = arguments?.getDouble("saturatedFat") ?: 0.0
        val baseProtein = arguments?.getDouble("protein") ?: 0.0
        val baseFiber = arguments?.getDouble("fiber") ?: 0.0
        val baseKcal = arguments?.getDouble("kcal") ?: 0.0

        val baseScore = HealthScore.calculateScore(
            sodium = baseSodium, sugar = baseSugar, saturatedFat = baseSaturatedFat,
            protein = baseProtein, fiber = baseFiber, kcal = baseKcal
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val myInfo = db.userDao().getMyInfo()
            val allergyList = myInfo?.allergies?.split(", ")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            val diseaseList = myInfo?.diseases?.split(", ")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

            val foods = CsvFoodReader.loadFoods(requireContext())

            if (foods.isEmpty()) {
                layoutGuideBox.visibility = View.GONE
                showErrorOnFirstCard(cards[0], "데이터 로드 실패", "CSV 파일을 읽어올 수 없습니다.")
                hideEmptyCards(cards, 1)
                return@launch
            }

            val normalizedBaseCategory = normalizeText(baseCategory)
            val cleanedBaseName = cleanDisplayName(baseName)

            val scoredFoods = foods
                .filter { food -> cleanDisplayName(food.name) != cleanedBaseName }
                .filter { food -> !containsAllergy(food, allergyList) }
                .map { food ->
                    food to HealthScore.calculateScore(
                        sodium = food.sodium,
                        sugar = food.sugar,
                        saturatedFat = food.saturatedFat,
                        protein = food.protein,
                        fiber = food.fiber,
                        kcal = food.kcal
                    )
                }

            val sameCategoryFoods = scoredFoods
                .filter { (food, _) ->
                    normalizeText(food.category) == normalizedBaseCategory
                }
                .sortedWith(
                    compareByDescending<Pair<FoodItem, Int>> { it.second >= baseScore }
                        .thenByDescending { it.second }.
                        thenBy { abs(it.second - baseScore) })

            val higherSameCategory = sameCategoryFoods
                .filter { (_, score) -> score >= baseScore }
            val closeSameCategory = sameCategoryFoods
                .filter { (_, score) -> score >= baseScore - 10 }
            val similarNameFoods = scoredFoods
                .filter { (food, score) ->
                    hasKeywordMatch(baseName, food.name) && score >= baseScore - 10
                }
                .sortedWith(
                    compareByDescending<Pair<FoodItem, Int>> { it.second >= baseScore }
                        .thenByDescending { it.second }
                        .thenBy { abs(it.second - baseScore) }
                )
            val closeScoreFoods = scoredFoods
                .filter { (_, score) -> score >= baseScore - 10 }
                .sortedWith(
                    compareByDescending<Pair<FoodItem, Int>> { it.second >= baseScore }
                        .thenByDescending { it.second }
                        .thenBy { abs(it.second - baseScore) }
                )

            val rawRecommended: List<Pair<FoodItem, Int>>
            val headerMessage: String

            when {
                higherSameCategory.isNotEmpty() -> {
                    rawRecommended = higherSameCategory
                    headerMessage = "현재 식품보다 건강점수가 같거나 높은 같은 카테고리 식품을 추천합니다."
                }
                closeSameCategory.isNotEmpty() -> {
                    rawRecommended = closeSameCategory
                    headerMessage = "같은 카테고리 내에서 현재 식품과 점수대가 비슷한 참고 후보를 추천합니다."
                }
                similarNameFoods.isNotEmpty() -> {
                    rawRecommended = similarNameFoods
                    headerMessage = "같은 카테고리 비교 데이터가 부족하여, 이름이 유사하고 점수대가 비슷한 참고 후보를 추천합니다."
                }
                closeScoreFoods.isNotEmpty() -> {
                    rawRecommended = closeScoreFoods
                    headerMessage = "같은 카테고리 비교 데이터가 부족하여, 현재 식품과 점수대가 비슷한 영양성분 기준 참고 후보를 추천합니다."
                }
                else -> {
                    rawRecommended = emptyList()
                    headerMessage = "추천 가능한 식품 데이터가 부족합니다."
                }
            }

            val recommended = selectFinalRecommendations(
                rawRecommended,
                baseScore,
                3
            )

            if (recommended.isEmpty()) {
                layoutGuideBox.visibility = View.VISIBLE
                tvGuideMessage.text = "현재 식품과 비교 가능한 추천 후보가 부족합니다."
                showErrorOnFirstCard(cards[0], "추천 식품 없음", "비교 가능한 식품 데이터가 부족합니다.")
                hideEmptyCards(cards, 1)
                return@launch
            }

            val personalizedMessage = buildPersonalizedMessage(allergyList, diseaseList)
            layoutGuideBox.visibility = View.VISIBLE
            tvGuideMessage.text = if (personalizedMessage.isNotBlank()) "$headerMessage\n$personalizedMessage" else headerMessage

            // 2. 표시 부분 수정: 반복문을 통해 각 카드에 데이터 바인딩
            for (i in cards.indices) {
                val cardView = cards[i]
                val item = recommended.getOrNull(i)

                if (item != null) {
                    cardView.visibility = View.VISIBLE
                    // 분리된 레이아웃 내부의 뷰들을 부모(cardView)에서 찾음
                    bindToCard(
                        cardView = cardView,
                        item = item,
                        baseScore = baseScore,
                        baseSodium = baseSodium,
                        baseSugar = baseSugar,
                        baseFat = baseSaturatedFat,
                        baseProtein = baseProtein,
                        baseFiber = baseFiber,
                        baseKcal = baseKcal
                    )

                    cardView.setOnClickListener {
                        moveToFoodDetail(item.first)
                    }
                } else {
                    cardView.visibility = View.GONE
                }
            }
        }
    }

    // 카드 내부의 뷰에 데이터를 직접 꽂아주는 함수
    private fun bindToCard(
        cardView: View,
        item: Pair<FoodItem, Int>,
        baseScore: Int,
        baseSodium: Double, baseSugar: Double, baseFat: Double,
        baseProtein: Double, baseFiber: Double, baseKcal: Double
    ) {
        val food = item.first
        val score = item.second

        // 분리된 XML(item_recommend_card.xml)의 ID들을 참조
        val tvScore = cardView.findViewById<TextView>(R.id.tvFoodScore)
        val tvName = cardView.findViewById<TextView>(R.id.tvFoodName)
        val tvReason = cardView.findViewById<TextView>(R.id.tvRecommendReason)
        val tvDiff = cardView.findViewById<TextView>(R.id.tvScoreDiff)

        tvScore.text = score.toString()
        tvName.text = cleanDisplayName(food.name)

        // 추천 사유 생성 및 적용
        tvReason.text = getReason(
            rec = food, recScore = score, baseScore = baseScore,
            bSodium = baseSodium, bSugar = baseSugar, bFat = baseFat,
            bProtein = baseProtein, bFiber = baseFiber, bKcal = baseKcal
        )

        // 점수 차이 계산 (+5점 등)
        val diff = score - baseScore
        tvDiff.text = if (diff >= 0) "+${diff}점" else "${diff}점"
    }

    private fun showErrorOnFirstCard(cardView: View, title: String, reason: String) {
        cardView.visibility = View.VISIBLE
        cardView.findViewById<TextView>(R.id.tvFoodName).text = title
        cardView.findViewById<TextView>(R.id.tvRecommendReason).text = reason
        cardView.findViewById<TextView>(R.id.tvFoodScore).text = "-"
        cardView.findViewById<TextView>(R.id.tvScoreDiff).text = "0점"
    }

    // --- 이하 보조 함수들은 기존 로직 그대로 유지 ---

    private fun getReason(
        rec: FoodItem, recScore: Int, baseScore: Int,
        bSodium: Double, bSugar: Double, bFat: Double,
        bProtein: Double, bFiber: Double, bKcal: Double
    ): String {
        val reasons = mutableListOf<String>()
        if (recScore > baseScore) reasons.add("건강점수가 높고")
        if (rec.sodium < bSodium) reasons.add("나트륨이 낮고")
        if (rec.sugar < bSugar) reasons.add("당류가 낮고")
        if (rec.saturatedFat < bFat) reasons.add("포화지방이 낮고")
        if (rec.kcal < bKcal) reasons.add("칼로리가 낮고")
        if (rec.protein > bProtein) reasons.add("단백질이 많고")
        if (rec.fiber > bFiber) reasons.add("식이섬유가 많고")

        if (reasons.isEmpty()) return "전체 영양 성분을 비교했을 때 참고할 수 있는 후보입니다."
        val reasonText = reasons
            .take(3)
            .joinToString(" ")

        return "기준 식품보다 $reasonText 더 적절한 선택입니다."
    }

    private fun selectFinalRecommendations(
        candidates: List<Pair<FoodItem, Int>>,
        baseScore: Int,
        limit: Int
    ): List<Pair<FoodItem, Int>> {
        val sorted = candidates.sortedWith(
            compareByDescending<Pair<FoodItem, Int>> { it.second >= baseScore }
                .thenByDescending { it.second }
                .thenBy { abs(it.second - baseScore) })
        val result = mutableListOf<Pair<FoodItem, Int>>()
        val seenBrandGroups = mutableSetOf<String>()
        var perfectScoreAdded = false
        for (item in sorted) {
            val food = item.first
            val score = item.second
            val brandKey = getBrandGroupKey(food.name)
            if (brandKey.isNotBlank() && seenBrandGroups.contains(brandKey)) continue
            if (score == 100 && perfectScoreAdded) continue
            if (brandKey.isNotBlank()) seenBrandGroups.add(brandKey)
            if (score == 100) perfectScoreAdded = true
            result.add(item)
            if (result.size == limit) break
        }
        if (result.size < limit) {
            for (item in sorted) {
                if (result.contains(item)) continue
                val score = item.second
                if (score == 100 && perfectScoreAdded) continue
                if (score == 100) perfectScoreAdded = true
                result.add(item)
                if (result.size == limit) break
            }
        }
        return result
    }

    private fun moveToFoodDetail(food: FoodItem) {
        val fragment = FoodDetailFragment().apply {
            arguments = Bundle().apply {
                putString("foodName", food.name); putString("foodCode", food.code)
                putDouble("kcal", food.kcal); putDouble("protein", food.protein)
                putDouble("fat", food.fat); putDouble("carb", food.carb)
                putDouble("sugar", food.sugar); putDouble("fiber", food.fiber)
                putDouble("sodium", food.sodium); putDouble("saturatedFat", food.saturatedFat)
                putString("foodCategory", food.category); putString("foodSubCategory", food.subCategory)
                putInt("defaultType", 0)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun containsAllergy(food: FoodItem, allergies: List<String>): Boolean {
        val target = "${food.name} ${food.category} ${food.subCategory}"
        return allergies.any { allergy ->
            target.contains(allergy, ignoreCase = true)
        }
    }

    private fun buildPersonalizedMessage(
        allergies: List<String>,
        diseases: List<String>
    ): String {
        val messages = mutableListOf<String>()
        if (allergies.isNotEmpty()) {
            messages.add("알레르기 정보(${allergies.joinToString(", ")})를 고려해 일부 후보를 제외했습니다.")
        }
        if (diseases.any { it.contains("당뇨") }) {
            messages.add("질환 정보(당뇨)를 고려하여 당류가 낮은 후보를 우선 확인했습니다.")
        }
        if (diseases.any { it.contains("고혈압") }) {
            messages.add("질환 정보(고혈압)를 고려하여 나트륨이 낮은 후보를 우선 확인했습니다.")
        }
        if (diseases.any { it.contains("비만") }) {
            messages.add("질환 정보(비만)를 고려하여 칼로리가 낮은 후보를 우선 확인했습니다.")
        }
        return messages.joinToString("\n")
    }

    private fun getBrandGroupKey(name: String): String {
        val cleaned = cleanDisplayName(name)
            .replace(Regex("[^가-힣a-zA-Z0-9 ]"), "")
            .trim()

        if (cleaned.isBlank()) return ""

        val firstToken = cleaned.split(" ")
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?: return ""

        return if (firstToken.length >= 2) {
            firstToken.substring(0, 2)
        } else {
            firstToken
        }
    }

    private fun hideEmptyCards(cards: List<View>, count: Int) {
        cards.forEachIndexed {
            index, view -> view.visibility = if (index < count) View.VISIBLE else View.GONE
        }
    }

    private fun normalizeText(text: String): String {
        return text.replace(" ", "").trim()
    }

    private fun cleanDisplayName(name: String): String {
        return name
            .replace(Regex("^[\"'?]+"), "")
            .replace("\"", "")
            .trim()
    }
    private fun hasKeywordMatch(baseName: String, targetName: String): Boolean {
        val baseKeywords = cleanDisplayName(baseName)
            .split(" ", "(", ")", "-", "_")
            .filter { it.length >= 2 }

        val cleanedTarget = cleanDisplayName(targetName)

        return baseKeywords.any { keyword ->
            cleanedTarget.contains(keyword, ignoreCase = true)
        }
    }
}