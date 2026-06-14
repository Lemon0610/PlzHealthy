package com.example.plzhealth

import android.content.Context
import android.util.Log

object FoodDataLoader {
    fun loadFoods(context: Context): List<Food> {
        val foods = mutableListOf<Food>()
        try {
            val inputStream = context.assets.open("food_data.csv")
            val reader = inputStream.bufferedReader(Charsets.UTF_8)
            val headerLine = reader.readLine()

            val delimiter = if (headerLine.contains("\t")) "\t" else ","
            Log.d("FoodLoader", "구분자: '$delimiter', 헤더: $headerLine")

            reader.forEachLine { line ->
                val cols = line.split(delimiter)
                Log.d("FoodLoader", "컬럼 수: ${cols.size}, 첫번째: ${cols[0]}")
                if (cols.size >= 16) {
                    foods.add(
                        Food(
                            code = cols[0].trim(),
                            name = cols[1].trim(),
                            category = cols[2].trim(),
                            subCategory = cols[3].trim(),
                            amount = cols[4].trim(),
                            energy = cols[5].trim(),
                            protein = cols[6].trim(),
                            fat = cols[7].trim(),
                            carbohydrate = cols[8].trim(),
                            sugar = cols[9].trim(),
                            fiber = cols[10].trim(),
                            calcium = cols[11].trim(),
                            sodium = cols[12].trim(),
                            vitaminC = cols[13].trim(),
                            cholesterol = cols[14].trim(),
                            saturatedFat = cols[15].trim()
                        )
                    )
                }
            }
            Log.d("FoodLoader", "로드된 식품 수: ${foods.size}")
        } catch (e: Exception) {
            Log.e("FoodLoader", "에러: ${e.message}")
        }
        return foods
    }
}