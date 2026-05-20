package com.example.plzhealth.utils

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

object DateUtils {

    fun calculateAge(birthDate: String): Int {
        val today = LocalDate.now()

        val formatters = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
        )

        for (formatter in formatters) {
            try {
                val birth = LocalDate.parse(birthDate, formatter)
                return Period.between(birth, today).years
            } catch (_: Exception) {
            }
        }

        return 0
    }

    fun getAgeGroup(age: Int): String {
        return when {
            age <= 11 -> "아동(6-11)"
            age in 12..18 -> "청소년(12-18)"
            age in 19..64 -> "성인"
            else -> "노인(65+)"
        }
    }

    fun getAgeGroupFromBirthDate(birthDate: String): String {
        val age = calculateAge(birthDate)
        return getAgeGroup(age)
    }
}