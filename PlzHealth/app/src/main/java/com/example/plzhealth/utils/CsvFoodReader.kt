package com.example.plzhealth.utils

import android.content.Context
import com.example.plzhealth.data.FoodItem

object CsvFoodReader {

    fun loadFoods(context: Context): List<FoodItem> {
        val foodList = mutableListOf<FoodItem>()

        val inputStream = context.assets.open("food_data.csv")
        val reader = inputStream.bufferedReader(Charsets.UTF_8)

        val lines = reader.readLines()
        if (lines.isEmpty()) return foodList

        for (i in 1 until lines.size) {
            val row = lines[i].split(",")

            if (row.size < 16) continue

            val item = FoodItem(
                code = row[0].trim(),
                name = row[1].trim(),
                category = row[2].trim(),
                subCategory = row[3].trim(),
                kcal = row[5].toDoubleOrNull() ?: 0.0,
                protein = row[6].toDoubleOrNull() ?: 0.0,
                fat = row[7].toDoubleOrNull() ?: 0.0,
                carb = row[8].toDoubleOrNull() ?: 0.0,
                sugar = row[9].toDoubleOrNull() ?: 0.0,
                fiber = row[10].toDoubleOrNull() ?: 0.0,
                sodium = row[12].toDoubleOrNull() ?: 0.0,
                saturatedFat = row[15].toDoubleOrNull() ?: 0.0
            )

            foodList.add(item)
        }

        return foodList
    }
}