package com.example.plzhealth.data

import com.google.gson.annotations.SerializedName

data class NutriResponse(
    val response: NutriResponseBody
)

data class NutriResponseBody(
    val body: NutriBody
)

data class NutriBody(
    val items: List<ApiFoodItem>?,
    val totalCount: String?
)

data class ApiFoodItem(
    @SerializedName("foodNm") val foodNm: String?,         // 식품명
    @SerializedName("foodLv3Nm") val category: String?,    // 카테고리 (대분류)
    @SerializedName("foodLv4Nm") val subCategory: String?, // 서브 카테고리 (중분류)
    @SerializedName("enerc") val kcal: String?,            // 에너지
    @SerializedName("prot") val protein: String?,          // 단백질
    @SerializedName("fatce") val fat: String?,             // 지방
    @SerializedName("chocdf") val carb: String?,           // 탄수화물
    @SerializedName("sugar") val sugar: String?,           // 당류
    @SerializedName("fibtg") val fiber: String?,           // 식이섬유
    @SerializedName("nat") val sodium: String?,            // 나트륨
    @SerializedName("fasat") val saturatedFat: String?     // 포화지방
)

fun ApiFoodItem.toFoodItem(): FoodItem {
    fun String?.toCleanDouble(): Double {
        if (this.isNullOrBlank()) return 0.0
        return this.replace(",", "").toDoubleOrNull() ?: 0.0
    }
    return FoodItem(
        code = this.foodNm ?: "",
        name = this.foodNm ?: "알 수 없는 식품",
        category = this.category ?: "기타",
        subCategory = this.subCategory ?: "기타",
        kcal = this.kcal.toCleanDouble(),
        protein = this.protein.toCleanDouble(),
        fat = this.fat.toCleanDouble(),
        carb = this.carb.toCleanDouble(),
        sugar = this.sugar.toCleanDouble(),
        fiber = this.fiber.toCleanDouble(),
        sodium = this.sodium.toCleanDouble(),
        saturatedFat = this.saturatedFat.toCleanDouble()
    )
}