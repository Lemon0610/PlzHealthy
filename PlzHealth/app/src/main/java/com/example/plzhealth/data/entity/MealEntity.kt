package com.example.plzhealth.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_table")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val mealType: String,
    val foodName: String,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carb: Double,
    val sugar: Double,
    val fiber: Double,
    val sodium: Double,
    val saturatedFat: Double,
    val category: String,
    val subCategory: String,
    val code: String
)