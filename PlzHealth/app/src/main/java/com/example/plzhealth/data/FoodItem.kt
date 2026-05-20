package com.example.plzhealth.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FoodItem(
    val code: String,
    val name: String,
    val category: String,
    val subCategory: String,
    val minorCategory: String,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carb: Double,
    val sugar: Double,
    val fiber: Double,
    val sodium: Double,
    val saturatedFat: Double
) : Parcelable