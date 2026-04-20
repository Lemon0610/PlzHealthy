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

class RecommendFragment : Fragment(R.layout.fragment_recommend) {

    private val db by lazy { AppDatabase.getDatabase(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)

        val layoutGuideBox = view.findViewById<View>(R.id.layoutGuideBox)
        val tvGuideMessage = view.findViewById<TextView>(R.id.tvGuideMessage)

        val cardRec1 = view.findViewById<View>(R.id.cardRec1)
        val cardRec2 = view.findViewById<View>(R.id.cardRec2)
        val cardRec3 = view.findViewById<View>(R.id.cardRec3)

        val tvFood1 = view.findViewById<TextView>(R.id.tvFood1)
        val tvReason1 = view.findViewById<TextView>(R.id.tvReason1)
        val tvFood2 = view.findViewById<TextView>(R.id.tvFood2)
        val tvReason2 = view.findViewById<TextView>(R.id.tvReason2)
        val tvFood3 = view.findViewById<TextView>(R.id.tvFood3)
        val tvReason3 = view.findViewById<TextView>(R.id.tvReason3)

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

        viewLifecycleOwner.lifecycleScope.launch {
            val myInfo = db.userDao().getMyInfo()

            val allergyList = myInfo?.allergies
                ?.split(", ")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val diseaseList = myInfo?.diseases
                ?.split(", ")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val personalizedMessage = buildPersonalizedMessage(allergyList, diseaseList)

            val foods = CsvFoodReader.loadFoods(requireContext())

            if (foods.isEmpty()) {
                layoutGuideBox.visibility = View.GONE
                tvFood1.text = "데이터 로드 실패"
                tvReason1.text = "CSV 파일을 읽어올 수 없습니다."
                clearText(tvFood2, tvReason2, tvFood3, tvReason3)
                disableCards(cardRec1, cardRec2, cardRec3)
                return@launch
            }

            val baseScore = personalizedBaseScore(
                sodium = baseSodium,
                sugar = baseSugar,
                saturatedFat = baseSaturatedFat,
                protein = baseProtein,
                fiber = baseFiber,
                kcal = baseKcal,
                diseases = diseaseList
            )

            val normalizedBaseCategory = normalizeText(baseCategory)
            val cleanedBaseName = cleanDisplayName(baseName)

            val scoredFoods = foods
                .filter { food -> cleanDisplayName(food.name) != cleanedBaseName }
                .filter { food -> !containsAllergy(food, allergyList) }
                .map { food -> food to personalizedScore(food, diseaseList) }

            val sameCategoryFoods = distinctByBrandGroup(
                scoredFoods
                    .filter { (food, _) ->
                        normalizeText(food.category) == normalizedBaseCategory
                    }
                    .sortedByDescending { it.second }
            )

            val higherInSameCategory = distinctByBrandGroup(
                sameCategoryFoods
                    .filter { it.second > baseScore }
                    .sortedByDescending { it.second }
            ).take(3)

            val similarLevelFoods = distinctByBrandGroup(
                scoredFoods
                    .filter { (_, score) -> score >= baseScore - 5 }
                    .sortedByDescending { it.second }
            ).take(6)

            val overallTopFoods = distinctByBrandGroup(
                scoredFoods.sortedByDescending { it.second }
            ).take(6)

            val similarNameFoods = distinctByBrandGroup(
                scoredFoods
                    .filter { (food, _) ->
                        hasKeywordMatch(baseName, food.name)
                    }
                    .sortedByDescending { it.second }
            ).take(6)

            val recommended: List<Pair<FoodItem, Int>>
            val headerMessage: String

            when {
                higherInSameCategory.isNotEmpty() -> {
                    recommended = higherInSameCategory
                    headerMessage = "현재 식품보다 건강점수가 높은 같은 카테고리 식품을 추천합니다."
                }

                sameCategoryFoods.isNotEmpty() -> {
                    recommended = sameCategoryFoods.take(3)
                    headerMessage = "현재 식품보다 더 높은 점수는 없어서, 같은 카테고리 내에서 비교적 건강한 식품을 추천합니다."
                }

                similarLevelFoods.isNotEmpty() -> {
                    recommended = pickDiverseRecommendations(similarLevelFoods, 3)
                    headerMessage = "같은 카테고리 비교 데이터가 부족하여, 현재 식품과 점수대가 비슷한 식품 중 상대적으로 건강한 식품을 추천합니다."
                }

                overallTopFoods.isNotEmpty() -> {
                    recommended = pickDiverseRecommendations(overallTopFoods, 3)
                    headerMessage = "같은 카테고리 비교 데이터가 부족하여, 전체 식품 중 상대적으로 건강한 식품을 추천합니다."
                }

                similarNameFoods.isNotEmpty() -> {
                    recommended = pickDiverseRecommendations(similarNameFoods, 3)
                    headerMessage = "같은 카테고리 비교 데이터가 부족하여, 이름이 유사한 식품 중 상대적으로 건강한 식품을 추천합니다."
                }

                else -> {
                    recommended = emptyList()
                    headerMessage = "추천 가능한 식품 데이터가 없습니다."
                }
            }

            if (recommended.isEmpty()) {
                layoutGuideBox.visibility = View.GONE
                tvFood1.text = "추천 식품 없음"
                tvReason1.text = "비교 가능한 식품 데이터가 없습니다."
                clearText(tvFood2, tvReason2, tvFood3, tvReason3)
                disableCards(cardRec1, cardRec2, cardRec3)
                return@launch
            }

            layoutGuideBox.visibility = View.VISIBLE
            tvGuideMessage.text = if (personalizedMessage.isNotBlank()) {
                "$headerMessage\n$personalizedMessage"
            } else {
                headerMessage
            }

            bindRecommendation(
                foodText = tvFood1,
                reasonText = tvReason1,
                item = recommended.getOrNull(0),
                baseScore = baseScore,
                baseSodium = baseSodium,
                baseSugar = baseSugar,
                baseFat = baseSaturatedFat,
                baseProtein = baseProtein,
                baseFiber = baseFiber
            )

            bindRecommendation(
                foodText = tvFood2,
                reasonText = tvReason2,
                item = recommended.getOrNull(1),
                baseScore = baseScore,
                baseSodium = baseSodium,
                baseSugar = baseSugar,
                baseFat = baseSaturatedFat,
                baseProtein = baseProtein,
                baseFiber = baseFiber
            )

            bindRecommendation(
                foodText = tvFood3,
                reasonText = tvReason3,
                item = recommended.getOrNull(2),
                baseScore = baseScore,
                baseSodium = baseSodium,
                baseSugar = baseSugar,
                baseFat = baseSaturatedFat,
                baseProtein = baseProtein,
                baseFiber = baseFiber
            )

            cardRec1.setOnClickListener {
                recommended.getOrNull(0)?.first?.let { food ->
                    moveToFoodDetail(food)
                }
            }

            cardRec2.setOnClickListener {
                recommended.getOrNull(1)?.first?.let { food ->
                    moveToFoodDetail(food)
                }
            }

            cardRec3.setOnClickListener {
                recommended.getOrNull(2)?.first?.let { food ->
                    moveToFoodDetail(food)
                }
            }

            cardRec1.isEnabled = recommended.getOrNull(0) != null
            cardRec2.isEnabled = recommended.getOrNull(1) != null
            cardRec3.isEnabled = recommended.getOrNull(2) != null
        }
    }

    private fun personalizedBaseScore(
        sodium: Double,
        sugar: Double,
        saturatedFat: Double,
        protein: Double,
        fiber: Double,
        kcal: Double,
        diseases: List<String>
    ): Int {
        var score = HealthScore.calculateScore(
            sodium = sodium,
            sugar = sugar,
            saturatedFat = saturatedFat,
            protein = protein,
            fiber = fiber,
            kcal = kcal
        )

        if (diseases.any { it.contains("당뇨") }) {
            if (sugar >= 10) score -= 15
            if (sugar >= 20) score -= 10
        }

        if (diseases.any { it.contains("고혈압") }) {
            if (sodium >= 500) score -= 15
            if (sodium >= 1000) score -= 10
        }

        if (diseases.any { it.contains("비만") }) {
            if (kcal >= 300) score -= 10
            if (kcal >= 500) score -= 10
        }

        return score.coerceIn(0, 100)
    }

    private fun personalizedScore(food: FoodItem, diseases: List<String>): Int {
        var score = HealthScore.calculateScore(
            sodium = food.sodium,
            sugar = food.sugar,
            saturatedFat = food.saturatedFat,
            protein = food.protein,
            fiber = food.fiber,
            kcal = food.kcal
        )

        if (diseases.any { it.contains("당뇨") }) {
            if (food.sugar >= 10) score -= 15
            if (food.sugar >= 20) score -= 10
        }

        if (diseases.any { it.contains("고혈압") }) {
            if (food.sodium >= 500) score -= 15
            if (food.sodium >= 1000) score -= 10
        }

        if (diseases.any { it.contains("비만") }) {
            if (food.kcal >= 300) score -= 10
            if (food.kcal >= 500) score -= 10
        }

        return score.coerceIn(0, 100)
    }

    private fun bindRecommendation(
        foodText: TextView,
        reasonText: TextView,
        item: Pair<FoodItem, Int>?,
        baseScore: Int,
        baseSodium: Double,
        baseSugar: Double,
        baseFat: Double,
        baseProtein: Double,
        baseFiber: Double
    ) {
        if (item == null) {
            foodText.text = ""
            reasonText.text = ""
            return
        }

        val food = item.first
        val score = item.second

        foodText.text = "${cleanDisplayName(food.name)} (${score}점)"
        reasonText.text = getReason(
            rec = food,
            recScore = score,
            baseScore = baseScore,
            bSodium = baseSodium,
            bSugar = baseSugar,
            bFat = baseFat,
            bProtein = baseProtein,
            bFiber = baseFiber
        )
    }

    private fun getReason(
        rec: FoodItem,
        recScore: Int,
        baseScore: Int,
        bSodium: Double,
        bSugar: Double,
        bFat: Double,
        bProtein: Double,
        bFiber: Double
    ): String {
        return when {
            recScore > baseScore && rec.sodium < bSodium ->
                "기준 식품보다 건강점수가 높고 나트륨이 낮습니다."
            recScore > baseScore && rec.sugar < bSugar ->
                "기준 식품보다 건강점수가 높고 당류가 낮습니다."
            recScore > baseScore && rec.saturatedFat < bFat ->
                "기준 식품보다 건강점수가 높고 포화지방이 낮습니다."
            recScore > baseScore && rec.fiber > bFiber ->
                "기준 식품보다 건강점수가 높고 식이섬유가 더 많습니다."
            recScore > baseScore && rec.protein > bProtein ->
                "기준 식품보다 건강점수가 높고 단백질이 더 많습니다."
            rec.sodium < bSodium ->
                "기준 식품보다 나트륨이 낮아 비교적 건강한 선택입니다."
            rec.sugar < bSugar ->
                "기준 식품보다 당류가 낮아 비교적 적절한 선택입니다."
            rec.saturatedFat < bFat ->
                "기준 식품보다 포화지방이 낮아 대체 후보로 적절합니다."
            rec.fiber > bFiber ->
                "기준 식품보다 식이섬유가 많아 상대적으로 건강한 편입니다."
            rec.protein > bProtein ->
                "기준 식품보다 단백질이 많아 영양 균형 면에서 더 좋습니다."
            else ->
                "전체 영양 성분을 비교했을 때 상대적으로 더 적절한 식품입니다."
        }
    }

    private fun moveToFoodDetail(food: FoodItem) {
        val fragment = FoodDetailFragment().apply {
            arguments = Bundle().apply {
                putString("foodName", food.name)
                putString("foodCode", food.code)
                putDouble("kcal", food.kcal)
                putDouble("protein", food.protein)
                putDouble("fat", food.fat)
                putDouble("carb", food.carb)
                putDouble("sugar", food.sugar)
                putDouble("fiber", food.fiber)
                putDouble("sodium", food.sodium)
                putDouble("saturatedFat", food.saturatedFat)
                putString("foodCategory", food.category)
                putString("foodSubCategory", food.subCategory)
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
            messages.add("질환 정보(당뇨)를 반영해 당류 기준을 더 엄격하게 적용했습니다.")
        }

        if (diseases.any { it.contains("고혈압") }) {
            messages.add("질환 정보(고혈압)를 반영해 나트륨 기준을 더 엄격하게 적용했습니다.")
        }

        if (diseases.any { it.contains("비만") }) {
            messages.add("질환 정보(비만)를 반영해 칼로리 기준을 더 엄격하게 적용했습니다.")
        }

        return messages.joinToString("\n")
    }

    private fun distinctByBrandGroup(items: List<Pair<FoodItem, Int>>): List<Pair<FoodItem, Int>> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Pair<FoodItem, Int>>()

        for (item in items) {
            val brandKey = getBrandGroupKey(item.first.name)
            if (brandKey.isNotBlank() && !seen.contains(brandKey)) {
                seen.add(brandKey)
                result.add(item)
            }
        }

        return result
    }

    private fun pickDiverseRecommendations(
        items: List<Pair<FoodItem, Int>>,
        limit: Int
    ): List<Pair<FoodItem, Int>> {
        val result = mutableListOf<Pair<FoodItem, Int>>()
        val seenBrandGroups = mutableSetOf<String>()

        for (item in items) {
            val key = getBrandGroupKey(item.first.name)
            if (!seenBrandGroups.contains(key)) {
                seenBrandGroups.add(key)
                result.add(item)
            }
            if (result.size == limit) break
        }

        return if (result.isNotEmpty()) result else items.take(limit)
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

        return if (firstToken.isNotEmpty()) {
            firstToken.substring(0, 1)
        } else {
            ""
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

    private fun clearText(
        tvFood2: TextView,
        tvReason2: TextView,
        tvFood3: TextView,
        tvReason3: TextView
    ) {
        tvFood2.text = ""
        tvReason2.text = ""
        tvFood3.text = ""
        tvReason3.text = ""
    }

    private fun disableCards(
        cardRec1: View,
        cardRec2: View,
        cardRec3: View
    ) {
        cardRec1.isEnabled = false
        cardRec2.isEnabled = false
        cardRec3.isEnabled = false
    }
}