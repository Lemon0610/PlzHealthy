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

    private enum class RecommendGroup {
        ICE_CREAM,
        CHOCOLATE,
        CANDY_JELLY,
        SNACK_COOKIE,
        CEREAL_BAR,
        RICE_CAKE,
        SAUCE_SYRUP,
        COFFEE,
        SODA,
        JUICE,
        TEA,
        DAIRY,
        RICE,
        NOODLE,
        BREAD,
        MEAT,
        SEAFOOD,
        VEGETABLE,
        UNKNOWN
    }

    private data class HealthInfo(
        val allergies: List<String>,
        val diseases: List<String>
    )

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
        val baseMinorCategory = food?.minorCategory ?: arguments?.getString("minorCategory") ?: ""
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

        showLoadingCards(cards, tvGuideMessage, layoutGuideBox)

        viewLifecycleOwner.lifecycleScope.launch {
            val healthInfo = getAllUserHealthInfo()
            val allergyList = healthInfo.allergies
            val diseaseList = healthInfo.diseases

            try {
                val baseGroup = inferRecommendGroup(
                    name = baseName,
                    category = baseCategory,
                    subCategory = baseSubCategory,
                    minorCategory = baseMinorCategory
                )

                val searchKeywords = buildRecommendationSearchKeywords(
                    baseName = baseName,
                    baseCategory = baseCategory,
                    baseSubCategory = baseSubCategory,
                    baseGroup = baseGroup
                )

                val foods = mutableListOf<FoodItem>()

                for (keyword in searchKeywords) {
                    try {
                        val response = RetrofitClient.service.getNutriInfo(
                            serviceKey = "4c0f8f4bc35efbe5d599f6c900f3475171464a453d2f1ad7ba568ffa5a15087b",
                            foodName = keyword,
                            numOfRows = 100
                        )

                        val apiItems = response.response.body?.items ?: emptyList()
                        foods.addAll(apiItems.map { it.toFoodItem() })
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "RecommendDebug",
                            "검색어 [$keyword] 추천 후보 로드 실패: ${e.message}",
                            e
                        )
                    }
                }

                val distinctFoods = foods
                    .distinctBy { simplifyFoodName(it.name) }

                if (distinctFoods.isEmpty()) {
                    layoutGuideBox.visibility = View.VISIBLE
                    tvGuideMessage.text = "현재 식품과 비교 가능한 추천 후보가 부족합니다."
                    showErrorOnFirstCard(cards[0], "추천 식품 없음", "비교 가능한 식품 데이터가 부족합니다.")
                    hideEmptyCards(cards, 1)
                    return@launch
                }

                val normalizedBaseCategory = normalizeText(baseCategory)
                val normalizedBaseSubCategory = normalizeText(baseSubCategory)

                val scoredCandidates = distinctFoods
                    .filter { item -> !isSameFoodName(baseName, item.name) }
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

                val allScoredFoods = scoredCandidates
                    .filter { (item, _) -> !containsAllergy(item, allergyList) }

                if (allScoredFoods.isEmpty()) {
                    layoutGuideBox.visibility = View.VISIBLE
                    tvGuideMessage.text = "추천 가능한 식품 데이터가 부족합니다."
                    showErrorOnFirstCard(cards[0], "추천 식품 없음", "비교 가능한 식품 데이터가 부족합니다.")
                    hideEmptyCards(cards, 1)
                    return@launch
                }

                val comparator = recommendComparator(
                    baseGroup = baseGroup,
                    baseScore = baseScore,
                    baseSodium = baseSodium,
                    baseSugar = baseSugar,
                    baseFat = baseSaturatedFat,
                    baseProtein = baseProtein,
                    baseFiber = baseFiber,
                    baseKcal = baseKcal,
                    diseaseList = diseaseList
                )



                val sameSubCategoryFoods = allScoredFoods
                    .filter { (item, _) ->
                        normalizedBaseSubCategory.isNotBlank() &&
                                normalizeText(item.subCategory) == normalizedBaseSubCategory
                    }
                    .sortedWith(comparator)

                val sameCategoryFoods = allScoredFoods
                    .filter { (item, _) ->
                        normalizedBaseCategory.isNotBlank() &&
                                normalizeText(item.category) == normalizedBaseCategory
                    }
                    .sortedWith(comparator)

                val sameGroupFoods = allScoredFoods
                    .filter { (item, _) ->
                        inferRecommendGroup(
                            name = item.name,
                            category = item.category,
                            subCategory = item.subCategory,
                            minorCategory = item.minorCategory
                        ) == baseGroup
                    }
                    .sortedWith(comparator)

                val relatedGroupFoods = allScoredFoods
                    .filter { (item, _) ->
                        val itemGroup = inferRecommendGroup(
                            name = item.name,
                            category = item.category,
                            subCategory = item.subCategory,
                            minorCategory = item.minorCategory
                        )
                        allowedCandidateGroups(baseGroup).contains(itemGroup)
                    }
                    .sortedWith(comparator)

                val nutritionImprovedFoods = allScoredFoods
                    .filter { (item, score) ->
                        score >= baseScore - 20 &&
                                nutritionImproveCount(
                                    item = item,
                                    baseSodium = baseSodium,
                                    baseSugar = baseSugar,
                                    baseFat = baseSaturatedFat,
                                    baseProtein = baseProtein,
                                    baseFiber = baseFiber,
                                    baseKcal = baseKcal
                                ) >= 2
                    }
                    .sortedWith(comparator)

                val fallbackFoods = allScoredFoods.sortedWith(comparator)

                val rawRecommended: List<Pair<FoodItem, Int>>
                val headerMessage: String

                when {
                    sameSubCategoryFoods.any { (_, score) -> score >= baseScore } -> {
                        rawRecommended = sameSubCategoryFoods.filter { (_, score) -> score >= baseScore }
                        headerMessage = "현재 식품과 같은 세부 식품군 안에서 건강점수와 영양성분을 기준으로 추천합니다."
                    }

                    sameSubCategoryFoods.isNotEmpty() -> {
                        rawRecommended = sameSubCategoryFoods
                        headerMessage = "현재 식품과 같은 세부 식품군 안에서 영양성분을 비교해 참고 후보를 추천합니다."
                    }

                    sameGroupFoods.any { (_, score) -> score >= baseScore } -> {
                        rawRecommended = sameGroupFoods.filter { (_, score) -> score >= baseScore }
                        headerMessage = "유사한 식품군 안에서 현재 식품보다 건강점수가 같거나 높은 식품을 추천합니다."
                    }

                    sameGroupFoods.isNotEmpty() -> {
                        rawRecommended = sameGroupFoods
                        headerMessage = "유사한 식품군 안에서 건강점수와 영양성분을 기준으로 추천합니다."
                    }

                    sameCategoryFoods.isNotEmpty() -> {
                        rawRecommended = sameCategoryFoods
                        headerMessage = "같은 식품군 후보 중 영양성분과 건강점수를 기준으로 참고 후보를 추천합니다."
                    }

                    relatedGroupFoods.isNotEmpty() -> {
                        rawRecommended = relatedGroupFoods
                        headerMessage = "관련 식품군 안에서 건강점수와 주요 영양성분을 기준으로 추천합니다."
                    }

                    nutritionImprovedFoods.isNotEmpty() -> {
                        rawRecommended = nutritionImprovedFoods
                        headerMessage = "유사 후보가 부족하여, 주요 영양성분이 개선된 후보를 추천합니다."
                    }

                    fallbackFoods.isNotEmpty() -> {
                        rawRecommended = fallbackFoods
                        headerMessage = "추천 후보 중 건강점수와 주요 영양성분을 기준으로 참고 식품을 추천합니다."
                    }

                    else -> {
                        rawRecommended = emptyList()
                        headerMessage = "추천 가능한 식품 데이터가 부족합니다."
                    }
                }

                val strictBackupCandidates =
                    when {
                        sameGroupFoods.isNotEmpty() -> sameGroupFoods
                        relatedGroupFoods.isNotEmpty() -> relatedGroupFoods
                        sameCategoryFoods.isNotEmpty() -> sameCategoryFoods
                        else -> fallbackFoods
                    }

                val recommended = selectFinalRecommendations(
                    candidates = rawRecommended,
                    backupCandidates = strictBackupCandidates,
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

    private suspend fun getAllUserHealthInfo(): HealthInfo {
        val myInfo = db.userDao().getMyInfo(true)
        val members = db.userDao().getMembers(false)

        val allUsers = listOfNotNull(myInfo) + members

        val allergies = allUsers
            .flatMap { splitHealthText(it.allergies) }
            .distinct()

        val diseases = allUsers
            .flatMap { splitHealthText(it.diseases) }
            .distinct()

        return HealthInfo(
            allergies = allergies,
            diseases = diseases
        )
    }

    private fun splitHealthText(text: String): List<String> {
        return text.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun recommendComparator(
        baseGroup: RecommendGroup,
        baseScore: Int,
        baseSodium: Double,
        baseSugar: Double,
        baseFat: Double,
        baseProtein: Double,
        baseFiber: Double,
        baseKcal: Double,
        diseaseList: List<String>
    ): Comparator<Pair<FoodItem, Int>> {
        return compareByDescending<Pair<FoodItem, Int>> { (item, _) ->
            inferRecommendGroup(
                name = item.name,
                category = item.category,
                subCategory = item.subCategory,
                minorCategory = item.minorCategory
            ) == baseGroup
        }.thenByDescending { (_, score) ->
            score >= baseScore
        }.thenByDescending { (item, _) ->
            diseasePriorityScore(item, diseaseList)
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
        }.thenByDescending { (_, score) ->
            score
        }.thenBy { (item, _) ->
            item.sugar
        }.thenBy { (item, _) ->
            item.sodium
        }.thenBy { (item, _) ->
            item.saturatedFat
        }.thenBy { (item, _) ->
            item.kcal
        }.thenBy { (_, score) ->
            abs(score - baseScore)
        }
    }

    private fun diseasePriorityScore(
        item: FoodItem,
        diseases: List<String>
    ): Int {
        var score = 0

        if (diseases.any { it.contains("당뇨") } && item.sugar <= 10.0) score += 3
        if (diseases.any { it.contains("고혈압") } && item.sodium <= 300.0) score += 3
        if (diseases.any { it.contains("비만") } && item.kcal <= 250.0) score += 3

        return score
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

    private fun showLoadingCards(
        cards: List<View>,
        tvGuideMessage: TextView,
        layoutGuideBox: View
    ) {
        layoutGuideBox.visibility = View.VISIBLE
        tvGuideMessage.text = "추천 후보를 분석 중입니다...\n식품군, 알레르기, 질환 정보를 함께 확인하고 있어요."

        showErrorOnFirstCard(cards[0], "분석 중", "추천 후보를 불러오고 있습니다.")
        hideEmptyCards(cards, 1)
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
        if (rec.sugar < bSugar) reasons.add("당류가 낮고")
        if (rec.sodium < bSodium) reasons.add("나트륨이 낮고")
        if (rec.saturatedFat < bFat) reasons.add("포화지방이 낮고")
        if (rec.kcal < bKcal) reasons.add("칼로리가 낮고")
        if (rec.protein > bProtein) reasons.add("단백질이 많고")
        if (rec.fiber > bFiber) reasons.add("식이섬유가 많고")

        if (reasons.isEmpty()) {
            return "건강점수와 전체 영양성분을 비교했을 때 참고할 수 있는 후보입니다."
        }

        val reasonText = reasons.take(3).joinToString(" ")

        return "현재 식품보다 $reasonText 더 적절한 선택입니다."
    }

    private fun selectFinalRecommendations(
        candidates: List<Pair<FoodItem, Int>>,
        backupCandidates: List<Pair<FoodItem, Int>>,
        baseScore: Int,
        limit: Int
    ): List<Pair<FoodItem, Int>> {
        val result = mutableListOf<Pair<FoodItem, Int>>()
        val seenSimpleNames = mutableSetOf<String>()

        val combined = (candidates + backupCandidates)
            .distinctBy { simplifyFoodName(it.first.name) }

        fun addCandidate(item: Pair<FoodItem, Int>) {
            if (result.size >= limit) return

            val simpleKey = simplifyFoodName(item.first.name)
            if (simpleKey.isBlank()) return
            if (seenSimpleNames.contains(simpleKey)) return

            seenSimpleNames.add(simpleKey)
            result.add(item)
        }

        // 1순위: 건강점수 가장 좋은 후보
        combined
            .sortedWith(
                compareByDescending<Pair<FoodItem, Int>> { it.second }
                    .thenBy { it.first.sugar }
                    .thenBy { it.first.sodium }
            )
            .firstOrNull()
            ?.let { addCandidate(it) }

        // 2순위: 당류가 낮은 후보
        combined
            .filter { !result.contains(it) }
            .sortedWith(
                compareBy<Pair<FoodItem, Int>> { it.first.sugar }
                    .thenByDescending { it.second }
                    .thenBy { it.first.kcal }
            )
            .firstOrNull()
            ?.let { addCandidate(it) }

        // 3순위: 나트륨 또는 칼로리가 낮은 후보
        combined
            .filter { !result.contains(it) }
            .sortedWith(
                compareBy<Pair<FoodItem, Int>> { it.first.sodium }
                    .thenBy { it.first.kcal }
                    .thenByDescending { it.second }
            )
            .firstOrNull()
            ?.let { addCandidate(it) }

        // 그래도 3개가 안 차면 남은 후보로 채우기
        combined
            .filter { !result.contains(it) }
            .sortedWith(
                compareByDescending<Pair<FoodItem, Int>> { it.second >= baseScore }
                    .thenByDescending { it.second }
                    .thenBy { abs(it.second - baseScore) }
            )
            .forEach {
                addCandidate(it)
                if (result.size >= limit) return@forEach
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
        val target = normalizeText(
            "${food.name} ${food.category} ${food.subCategory} ${food.minorCategory}"
        )

        return allergies.any { allergy ->

            val matched = getAllergyKeywords(allergy).any { keyword ->
                target.contains(normalizeText(keyword))
            }

            matched
        }
    }

    private fun getAllergyKeywords(allergy: String): List<String> {
        return when {
            allergy.contains("유제품") || allergy.contains("우유") ->
                listOf("유제품", "우유", "요거트", "요구르트", "치즈", "버터", "생크림", "크림", "라떼")

            allergy.contains("해산물") || allergy.contains("수산") ->
                listOf("해산물", "수산", "생선", "참치", "고등어", "연어", "오징어", "어묵", "새우", "게", "조개")

            allergy.contains("갑각류") || allergy.contains("새우") || allergy.contains("게") ->
                listOf("갑각류", "새우", "게", "크랩", "랍스터")

            allergy.contains("계란") || allergy.contains("달걀") ->
                listOf("계란", "달걀", "난백", "난황", "에그", "마요네즈")

            allergy.contains("땅콩") ->
                listOf("땅콩", "피넛", "견과")

            allergy.contains("대두") || allergy.contains("콩") ->
                listOf("대두", "콩", "두유", "두부", "된장", "간장")

            allergy.contains("밀") ->
                listOf("밀", "밀가루", "빵", "면", "라면", "파스타", "쿠키", "비스킷")

            allergy.contains("돼지고기") ->
                listOf("돼지고기", "돈육", "햄", "소시지", "베이컨")

            allergy.contains("복숭아") ->
                listOf("복숭아", "피치")

            else ->
                listOf(allergy)
        }
    }

    private fun buildPersonalizedMessage(
        allergies: List<String>,
        diseases: List<String>
    ): String {
        val messages = mutableListOf<String>()

        if (allergies.isNotEmpty()) {
            messages.add("내 정보와 구성원의 알레르기 정보(${allergies.joinToString(", ")})를 고려해 일부 후보를 제외했습니다.")
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

    private fun buildRecommendationSearchKeywords(
        baseName: String,
        baseCategory: String,
        baseSubCategory: String,
        baseGroup: RecommendGroup
    ): List<String> {
        val cleanedName = cleanDisplayName(baseName)

        val categoryKeywords = when (baseGroup) {
            RecommendGroup.ICE_CREAM ->
                listOf("아이스크림", "빙과", "샤베트", "요거트아이스", "저당아이스")

            RecommendGroup.CHOCOLATE ->
                listOf("초콜릿", "초코", "카카오", "다크초콜릿", "초코바")

            RecommendGroup.CANDY_JELLY ->
                listOf("사탕", "젤리", "캔디", "카라멜", "양갱")

            RecommendGroup.SNACK_COOKIE ->
                listOf("과자", "스낵", "쿠키", "비스킷", "크래커", "웨하스")

            RecommendGroup.CEREAL_BAR ->
                listOf("시리얼", "그래놀라", "오트", "시리얼바", "에너지바")

            RecommendGroup.RICE_CAKE ->
                listOf("떡", "찹쌀떡", "인절미", "가래떡", "설기", "약과", "한과")

            RecommendGroup.SAUCE_SYRUP ->
                listOf("시럽", "카라멜시럽", "메이플시럽", "꿀", "잼", "소스", "드레싱")

            RecommendGroup.COFFEE ->
                listOf("커피", "아메리카노", "블랙커피", "원두커피", "믹스커피", "카페라떼")

            RecommendGroup.SODA ->
                listOf("탄산음료", "제로", "콜라", "사이다", "탄산수")

            RecommendGroup.JUICE ->
                listOf("주스", "과채주스", "오렌지주스", "사과주스", "과일음료")

            RecommendGroup.TEA ->
                listOf("차", "녹차", "홍차", "보리차", "허브티")

            RecommendGroup.DAIRY ->
                listOf("우유", "요거트", "요구르트", "치즈", "두유")

            RecommendGroup.NOODLE ->
                listOf("국수", "우동", "냉면", "쌀국수", "파스타", "라면")

            RecommendGroup.RICE ->
                listOf("비빔밥", "볶음밥", "주먹밥", "김밥", "덮밥", "죽")

            RecommendGroup.BREAD ->
                listOf("식빵", "베이글", "샌드위치", "모닝빵", "호밀빵", "토스트")

            RecommendGroup.MEAT ->
                listOf("닭가슴살", "소고기", "돼지고기", "햄", "소시지")

            RecommendGroup.SEAFOOD ->
                listOf("생선", "참치", "고등어", "어묵", "연어", "오징어")

            RecommendGroup.VEGETABLE ->
                listOf("샐러드", "채소", "야채", "두부", "버섯")

            RecommendGroup.UNKNOWN ->
                listOf(cleanedName.take(2), baseSubCategory, baseCategory)
        }

        val backupKeywords = listOf(
            "김밥", "비빔밥", "볶음밥", "샐러드", "닭가슴살",
            "두부", "버섯", "아메리카노", "차", "두유",
            "요거트", "국수", "샌드위치"
        )

        return (categoryKeywords + backupKeywords)
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun inferRecommendGroup(
        name: String,
        category: String,
        subCategory: String,
        minorCategory: String
    ): RecommendGroup {
        val text = normalizeText("$name $category $subCategory $minorCategory")

        return when {
            containsAny(text, listOf("아이스크림", "빙과", "샤베트", "월드콘", "메로나", "비비빅", "돼지바")) ->
                RecommendGroup.ICE_CREAM

            containsAny(text, listOf("초콜릿", "초코", "코코아", "카카오", "초코바", "초코칩")) ->
                RecommendGroup.CHOCOLATE

            containsAny(text, listOf("시럽", "카라멜시럽", "메이플시럽", "초코시럽", "꿀", "잼", "소스", "드레싱", "케찹", "마요네즈", "양념", "시즈닝")) ->
                RecommendGroup.SAUCE_SYRUP

            containsAny(text, listOf("사탕", "젤리", "캔디", "카라멜", "양갱")) ->
                RecommendGroup.CANDY_JELLY

            containsAny(text, listOf("시리얼", "그래놀라", "오트", "에너지바", "시리얼바")) ->
                RecommendGroup.CEREAL_BAR

            containsAny(text, listOf("떡", "찹쌀", "인절미", "가래떡", "설기", "약과", "한과", "오란다")) ->
                RecommendGroup.RICE_CAKE

            containsAny(text, listOf("과자", "스낵", "쿠키", "비스킷", "크래커", "웨하스", "칩", "새우깡", "포카칩", "홈런볼")) ->
                RecommendGroup.SNACK_COOKIE

            containsAny(text, listOf("커피", "아메리카노", "블랙커피", "믹스커피", "카페라떼", "라떼")) ->
                RecommendGroup.COFFEE

            containsAny(text, listOf("탄산", "콜라", "사이다", "탄산수", "제로음료")) ->
                RecommendGroup.SODA

            containsAny(text, listOf("주스", "과채", "과일음료", "오렌지", "사과주스")) ->
                RecommendGroup.JUICE

            containsAny(text, listOf("녹차", "홍차", "보리차", "허브티", "티백")) ->
                RecommendGroup.TEA

            containsAny(text, listOf("우유", "요거트", "요구르트", "치즈", "두유", "유제품", "유가공")) ->
                RecommendGroup.DAIRY

            containsAny(text, listOf("라면", "국수", "우동", "냉면", "쌀국수", "파스타", "면")) ->
                RecommendGroup.NOODLE

            containsAny(text, listOf("밥", "김밥", "볶음밥", "비빔밥", "덮밥", "주먹밥", "죽")) ->
                RecommendGroup.RICE

            containsAny(text, listOf("빵", "식빵", "베이글", "샌드위치", "토스트", "모닝빵", "호밀빵")) ->
                RecommendGroup.BREAD

            containsAny(text, listOf("고기", "육", "닭", "소고기", "돼지고기", "햄", "소시지", "닭가슴살")) ->
                RecommendGroup.MEAT

            containsAny(text, listOf("해산물", "수산", "생선", "참치", "고등어", "연어", "오징어", "어묵", "새우", "게", "조개", "굴비")) ->
                RecommendGroup.SEAFOOD

            containsAny(text, listOf("채소", "야채", "샐러드", "버섯")) ->
                RecommendGroup.VEGETABLE

            else ->
                RecommendGroup.UNKNOWN
        }
    }

    private fun allowedCandidateGroups(baseGroup: RecommendGroup): Set<RecommendGroup> {
        return when (baseGroup) {
            RecommendGroup.ICE_CREAM ->
                setOf(RecommendGroup.ICE_CREAM, RecommendGroup.DAIRY)

            RecommendGroup.CHOCOLATE ->
                setOf(RecommendGroup.CHOCOLATE, RecommendGroup.CEREAL_BAR, RecommendGroup.SNACK_COOKIE)

            RecommendGroup.CANDY_JELLY ->
                setOf(RecommendGroup.CANDY_JELLY, RecommendGroup.RICE_CAKE, RecommendGroup.SNACK_COOKIE)

            RecommendGroup.SNACK_COOKIE ->
                setOf(RecommendGroup.SNACK_COOKIE, RecommendGroup.CHOCOLATE, RecommendGroup.CEREAL_BAR, RecommendGroup.RICE_CAKE)

            RecommendGroup.CEREAL_BAR ->
                setOf(RecommendGroup.CEREAL_BAR, RecommendGroup.SNACK_COOKIE)

            RecommendGroup.RICE_CAKE ->
                setOf(RecommendGroup.RICE_CAKE, RecommendGroup.SNACK_COOKIE, RecommendGroup.CANDY_JELLY)

            RecommendGroup.SAUCE_SYRUP ->
                setOf(RecommendGroup.SAUCE_SYRUP, RecommendGroup.CANDY_JELLY)

            RecommendGroup.COFFEE ->
                setOf(RecommendGroup.COFFEE, RecommendGroup.TEA)

            RecommendGroup.SODA ->
                setOf(RecommendGroup.SODA, RecommendGroup.TEA)

            RecommendGroup.JUICE ->
                setOf(RecommendGroup.JUICE, RecommendGroup.TEA)

            RecommendGroup.TEA ->
                setOf(RecommendGroup.TEA, RecommendGroup.COFFEE)

            RecommendGroup.DAIRY ->
                setOf(RecommendGroup.DAIRY)

            RecommendGroup.NOODLE ->
                setOf(RecommendGroup.NOODLE, RecommendGroup.RICE)

            RecommendGroup.RICE ->
                setOf(RecommendGroup.RICE, RecommendGroup.NOODLE)

            RecommendGroup.BREAD ->
                setOf(RecommendGroup.BREAD, RecommendGroup.CEREAL_BAR)

            RecommendGroup.MEAT ->
                setOf(RecommendGroup.MEAT, RecommendGroup.VEGETABLE)

            RecommendGroup.SEAFOOD ->
                setOf(RecommendGroup.SEAFOOD, RecommendGroup.MEAT)

            RecommendGroup.VEGETABLE ->
                setOf(RecommendGroup.VEGETABLE)

            RecommendGroup.UNKNOWN ->
                setOf(RecommendGroup.UNKNOWN)
        }
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { keyword ->
            text.contains(normalizeText(keyword))
        }
    }

    private fun simplifyFoodName(name: String): String {
        return cleanDisplayName(name)
            .replace(Regex("\\([0-9.]+g\\)"), "")
            .replace(Regex("[^가-힣a-zA-Z0-9]"), "")
            .take(8)
    }

    private fun isSameFoodName(baseName: String, targetName: String): Boolean {
        val base = simplifyFoodName(baseName)
        val target = simplifyFoodName(targetName)

        if (base.isBlank() || target.isBlank()) return false

        return base == target || target.contains(base) || base.contains(target)
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