package com.example.plzhealth.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_category_table")
data class FoodCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val major: String,
    val middle: String,
    val minor: String
)