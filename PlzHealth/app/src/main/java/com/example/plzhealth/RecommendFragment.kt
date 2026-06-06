package com.example.plzhealth

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.plzhealth.data.AppDatabase
import com.example.plzhealth.data.FoodItem
import com.example.plzhealth.data.RetrofitClient
import com.example.plzhealth.data.toFoodItem
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

        val food = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("selectedFood", FoodItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("selectedFood")
        }

        val baseCategory = food?.category ?: arguments?.getString("category") ?: ""
        val baseSubCategory = food?.subCategory ?: arguments?.getString("subCategory") ?: ""
        val baseName = food?.name ?: arguments?.getString("baseName") ?: ""

        val baseSodium = food?.sodium ?: arguments?.getDouble("sodium") ?: 0.0
        val baseSugar = food?.sugar ?: arguments?.getDouble("sugar") ?: 0.0
        val baseSaturatedFat = food?.saturatedFat ?: arguments?.getDouble("saturatedFat") ?: 0.0
        val baseProtein = food?.protein ?: arguments?.getDouble("protein") ?: 0.0
        val baseFiber = food?.fiber ?: arguments?.getDouble("fiber") ?: 0.0
        val baseKcal = food?.kcal ?: arguments?.getDouble("kcal") ?: 0.0

        val baseScore = HealthScore.calculateScore(
            sodium = baseSodium,
            sugar = baseSugar,
            saturatedFat = baseSaturatedFat,
            protein = baseProtein,
            fiber = baseFiber,
            kcal = baseKcal
        )

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

            try {
                val searchKeywords = listOf(
                    baseSubCategory,
                    baseCategory,
                    baseName.take(2),
                    "식품"
                ).filter { it.isNotBlank() }.distinct()

                var foods = emptyList<FoodItem>()

                for (keyword in searchKeywords) {
                    val response = RetrofitClient.service.getNutriInfo(
                        serviceKey = "4c0f8f4bc35efbe5d599f6c900f3475171464a453d2f1ad7ba568ffa5a15087b",
                        foodName = keyword,
                        numOfRows = 100
                    )

                    val apiItems = response.response.body?.items ?: emptyList()
                    foods = apiItems.map { it.toFoodItem() }

                    if (foods.isNotEmpty()) break
                }

                if (foods.isEmpty()) {
                    layoutGuideBox.visibility = View.GONE
                    showErrorOnFirstCard(cards[0], "데이터 없음", "추천할 식품이 없습니다.")
                    hideEmptyCards(cards, 1)
                    return@launch
                }

                val normalizedBaseCategory = normalizeText(baseCategory)
                val normalizedBaseSubCategory = normalizeText(baseSubCategory)
                val cleanedBaseName = cleanDisplayName(baseName)

                val scoredFoods = foods
                    .filter { item -> cleanDisplayName(item.name) != cleanedBaseName }
                    .filter { item -> !containsAllergy(item, allergyList) }
                    .map { item ->
                        val score = HealthScore.calculateScore(
                            sodium = item.sodium,
                            sugar = item.sugar,
                            saturatedFat = item.saturatedFat,
                            protein = item.protein,
                            fiber = item.fiber,
                            kcal = item.kcal
                        )
                        item to score
                    }

                val sameSubCategoryFoods = scoredFoods
                    .filter { (item, _) ->
                        normalizedBaseSubCategory.isNotBlank() &&
                                normalizeText(item.subCategory) == normalizedBaseSubCategory
                    }
                    .sortedWith(
                        recommendComparator(
                            baseScore = baseScore,
                            baseSodium = baseSodium,
                            baseSugar = baseSugar,
                            baseFat = baseSaturatedFat,
                            baseProtein = baseProtein,
                            baseFiber = baseFiber,
                            baseKcal = baseKcal
                        )
                    )

                val sameCategoryFoods = scoredFoods
                    .filter { (item, _) ->
                        normalizedBaseCategory.isNotBlank() &&
                                normalizeText(item.category) == normalizedBaseCategory
                    }
                    .sortedWith(
                        recommendComparator(
                            baseScore = baseScore,
                            baseSodium = baseSodium,
                            baseSugar = baseSugar,
                            baseFat = baseSaturatedFat,
                            baseProtein = baseProtein,
                            baseFiber = baseFiber,
                            baseKcal = baseKcal
                        )
                    )

                val betterNutritionFoods = scoredFoods
                    .filter { (item, score) ->
                        score >= baseScore - 20 &&
                                (
                                        item.sodium <= baseSodium ||
                                                item.sugar <= baseSugar ||
                                                item.saturatedFat <= baseSaturatedFat ||
                                                item.kcal <= baseKcal ||
                                                item.protein >= baseProtein ||
                                                item.fiber >= baseFiber
                                        )
                    }
                    .sortedWith(
                        recommendComparator(
                            baseScore = baseScore,
                            baseSodium = baseSodium,
                            baseSugar = baseSugar,
                            baseFat = baseSaturatedFat,
                            baseProtein = baseProtein,
                            baseFiber = baseFiber,
                            baseKcal = baseKcal
                        )
                    )

                val rawRecommended: List<Pair<FoodItem, Int>>
                val headerMessage: String

                when {
                    sameSubCategoryFoods.any { (_, score) -> score >= baseScore } -> {
                        rawRecommended = sameSubCategoryFoods.filter { (_, score) -> score >= baseScore }
                        headerMessage = "같은 소분류 안에서 현재 식품보다 건강점수가 같거나 높은 식품을 추천합니다."
                    }

                    sameSubCategoryFoods.isNotEmpty() -> {
                        rawRecommended = sameSubCategoryFoods
                        headerMessage = "같은 소분류 안에서 영양성분과 건강점수를 기준으로 참고 후보를 추천합니다."
                    }

                    sameCategoryFoods.any { (_, score) -> score >= baseScore } -> {
                        rawRecommended = sameCategoryFoods.filter { (_, score) -> score >= baseScore }
                        headerMessage = "같은 대분류 안에서 현재 식품보다 건강점수가 같거나 높은 식품을 추천합니다."
                    }

                    sameCategoryFoods.isNotEmpty() -> {
                        rawRecommended = sameCategoryFoods
                        headerMessage = "같은 대분류 안에서 영양성분과 건강점수를 기준으로 참고 후보를 추천합니다."
                    }

                    betterNutritionFoods.isNotEmpty() -> {
                        rawRecommended = betterNutritionFoods
                        headerMessage = "카테고리 비교 데이터가 부족하여, 주요 영양성분과 건강점수를 기준으로 참고 후보를 추천합니다."
                    }

                    else -> {
                        rawRecommended = emptyList()
                        headerMessage = "추천 가능한 식품 데이터가 부족합니다."
                    }
                }

                val recommended = selectFinalRecommendations(
                    candidates = rawRecommended,
                    baseScore = baseScore,
                    limit = 3
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
                tvGuideMessage.text =
                    if (personalizedMessage.isNotBlank()) {
                        "$headerMessage\n$personalizedMessage"
                    } else {
                        headerMessage
                    }

                for (i in cards.indices) {
                    val cardView = cards[i]
                    val item = recommended.getOrNull(i)

                    if (item != null) {
                        cardView.visibility = View.VISIBLE

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
            } catch (e: Exception) {
                android.util.Log.e("RecommendDebug", "추천 로드 중 에러 발생: ${e.message}", e)
                layoutGuideBox.visibility = View.GONE
                showErrorOnFirstCard(cards[0], "오류 발생", "데이터를 가져오지 못했습니다.\n(${e.localizedMessage})")
                hideEmptyCards(cards, 1)
            }
        }
    }

    private fun recommendComparator(
        baseScore: Int,
        baseSodium: Double,
        baseSugar: Double,
        baseFat: Double,
        baseProtein: Double,
        baseFiber: Double,
        baseKcal: Double
    ): Comparator<Pair<FoodItem, Int>> {
        return compareByDescending<Pair<FoodItem, Int>> { (_, score) ->
            score >= baseScore
        }.thenByDescending { (_, score) ->
            score
        }.thenByDescending { (item, _) ->
            nutritionImproveCount(
                item = item,
                baseSodium = baseSodium,
                baseSugar = baseSugar,
                baseFat = baseFat,
                baseProtein = baseProtein,
                baseFiber = baseFiber,
                baseKcal = baseKcal
            )
        }.thenBy { (item, _) ->
            item.sodium
        }.thenBy { (item, _) ->
            item.sugar
        }.thenBy { (item, _) ->
            item.saturatedFat
        }.thenBy { (_, score) ->
            abs(score - baseScore)
        }
    }

    private fun nutritionImproveCount(
        item: FoodItem,
        baseSodium: Double,
        baseSugar: Double,
        baseFat: Double,
        baseProtein: Double,
        baseFiber: Double,
        baseKcal: Double
    ): Int {
        var count = 0

        if (item.sodium < baseSodium) count++
        if (item.sugar < baseSugar) count++
        if (item.saturatedFat < baseFat) count++
        if (item.kcal < baseKcal) count++
        if (item.protein > baseProtein) count++
        if (item.fiber > baseFiber) count++

        return count
    }

    private fun bindToCard(
        cardView: View,
        item: Pair<FoodItem, Int>,
        baseScore: Int,
        baseSodium: Double,
        baseSugar: Double,
        baseFat: Double,
        baseProtein: Double,
        baseFiber: Double,
        baseKcal: Double
    ) {
        val food = item.first
        val score = item.second

        val tvScore = cardView.findViewById<TextView>(R.id.tvFoodScore)
        val tvName = cardView.findViewById<TextView>(R.id.tvFoodName)
        val tvReason = cardView.findViewById<TextView>(R.id.tvRecommendReason)
        val tvDiff = cardView.findViewById<TextView>(R.id.tvScoreDiff)

        tvScore.text = score.toString()
        tvName.text = cleanDisplayName(food.name)

        tvReason.text = getReason(
            rec = food,
            recScore = score,
            baseScore = baseScore,
            bSodium = baseSodium,
            bSugar = baseSugar,
            bFat = baseFat,
            bProtein = baseProtein,
            bFiber = baseFiber,
            bKcal = baseKcal
        )

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

    private fun getReason(
        rec: FoodItem,
        recScore: Int,
        baseScore: Int,
        bSodium: Double,
        bSugar: Double,
        bFat: Double,
        bProtein: Double,
        bFiber: Double,
        bKcal: Double
    ): String {
        val reasons = mutableListOf<String>()

        if (recScore > baseScore) reasons.add("건강점수가 높고")
        if (rec.sodium < bSodium) reasons.add("나트륨이 낮고")
        if (rec.sugar < bSugar) reasons.add("당류가 낮고")
        if (rec.saturatedFat < bFat) reasons.add("포화지방이 낮고")
        if (rec.kcal < bKcal) reasons.add("칼로리가 낮고")
        if (rec.protein > bProtein) reasons.add("단백질이 많고")
        if (rec.fiber > bFiber) reasons.add("식이섬유가 많고")

        if (reasons.isEmpty()) {
            return "카테고리와 전체 영양성분을 비교했을 때 참고할 수 있는 후보입니다."
        }

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
                .thenBy { abs(it.second - baseScore) }
        )

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
                putParcelable("selectedFood", food)
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
        cards.forEachIndexed { index, view ->
            view.visibility = if (index < count) View.VISIBLE else View.GONE
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
}