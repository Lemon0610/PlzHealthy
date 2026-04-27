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

        // 1. 감점 요소
        // 나트륨
        if (sodium >= 1500) score -= 25
        else if (sodium >= 800) score -= 15
        else if (sodium >= 400) score -= 8

        // 당류
        if (sugar >= 20) score -= 20
        else if (sugar >= 10) score -= 10
        else if (sugar >= 5) score -= 5

        // 포화지방
        if (saturatedFat >= 8) score -= 15
        else if (saturatedFat >= 4) score -= 8
        else if (saturatedFat >= 2) score -= 4

        // 칼로리
        if (kcal >= 500) score -= 15
        else if (kcal >= 300) score -= 8

        // 2. 가점 요소
        // 단백질 가점
        score += when {
            protein >= 15 -> 4
            protein >= 8 -> 2
            protein >= 4 -> 1
            else -> 0
        }

// 식이섬유 가점
        score += when {
            fiber >= 6 -> 6
            fiber >= 3 -> 3
            fiber >= 1.5 -> 1
            else -> 0
        }

        // 3. 데이터 신뢰도 보정
        var zeroCount = 0
        if (sodium == 0.0) zeroCount++
        if (sugar == 0.0) zeroCount++
        if (saturatedFat == 0.0) zeroCount++
        if (protein == 0.0) zeroCount++
        if (fiber == 0.0) zeroCount++
        if (kcal == 0.0) zeroCount++

        if (kcal == 0.0) score -= 15

        if (zeroCount >= 5) score -= 25
        else if (zeroCount >= 3) score -= 15

        return score.coerceIn(0, 100)
    }
}