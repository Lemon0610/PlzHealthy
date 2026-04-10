package com.example.plzhealth.utils

object HealthScore {

    fun calculateScore(
        sodium: Double,
        sugar: Double,
        saturatedFat: Double,
        protein: Double,
        fiber: Double,
        kcal: Double
    ): Int {

        var score = 100

        if (sodium >= 500) {
            score -= 15
        } else if (sodium >= 300) {
            score -= 5
        }

        if (sugar >= 20) {
            score -= 15
        } else if (sugar >= 10) {
            score -= 5
        }

        if (saturatedFat >= 5) {
            score -= 10
        } else if (saturatedFat >= 3) {
            score -= 5
        }

        if (kcal >= 800) {
            score -= 10
        } else if (kcal >= 500) {
            score -= 5
        }

        if (protein >= 10) {
            score += 10
        } else if (protein >= 5) {
            score += 5
        }

        if (fiber >= 5) {
            score += 10
        } else if (fiber >= 3) {
            score += 5
        }

        if (score < 0) score = 0
        if (score > 100) score = 100

        return score
    }
}