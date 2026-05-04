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
    val totalCount: Int
)

data class ApiFoodItem(
    @SerializedName("foodNm") val foodNm: String?,
    @SerializedName("foodCtgryNm") val category: String?,
    @SerializedName("foodSubCtgryNm") val subCategory: String?,
    @SerializedName("enerqyQy") val kcal: String?,
    @SerializedName("protQy") val protein: String?,
    @SerializedName("fatQy") val fat: String?,
    @SerializedName("carboQy") val carb: String?,
    @SerializedName("sugarQy") val sugar: String?,
    @SerializedName("ntrfsFiberQy") val fiber: String?,
    @SerializedName("natriumQy") val sodium: String?,
    @SerializedName("sfaQy") val saturatedFat: String?
)

fun ApiFoodItem.toFoodItem(): FoodItem {
    return FoodItem(
        code = this.foodNm ?: "",
        name = this.foodNm ?: "알 수 없는 식품",
        category = this.category ?: "기타",
        subCategory = this.subCategory ?: "기타",
        kcal = this.kcal?.toDoubleOrNull() ?: 0.0,
        protein = this.protein?.toDoubleOrNull() ?: 0.0,
        fat = this.fat?.toDoubleOrNull() ?: 0.0,
        carb = this.carb?.toDoubleOrNull() ?: 0.0,
        sugar = this.sugar?.toDoubleOrNull() ?: 0.0,
        fiber = this.fiber?.toDoubleOrNull() ?: 0.0,
        sodium = this.sodium?.toDoubleOrNull() ?: 0.0,
        saturatedFat = this.saturatedFat?.toDoubleOrNull() ?: 0.0
    )
}