package com.example.plzhealth.utils

import java.util.Calendar

object DateUtils {
    fun calculateAge(birthDate: String?): String {
        if (birthDate.isNullOrBlank()) return "미입력"

        val cleanDate = birthDate.replace(Regex("[^0-9]"), "")
        if (cleanDate.length < 8) return "형식 오류"

        try {
            val year = cleanDate.substring(0, 4).toInt()
            val month = cleanDate.substring(4, 6).toInt()
            val day = cleanDate.substring(6, 8).toInt()

            val today = Calendar.getInstance()
            val currentYear = today.get(Calendar.YEAR)
            val currentMonth = today.get(Calendar.MONTH) + 1
            val currentDay = today.get(Calendar.DAY_OF_MONTH)
            val age = currentYear - year + 1

            return "${age}세"
        } catch (e: Exception) {
            return "계산 불가"
        }
    }
}