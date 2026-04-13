package com.example.plzhealth.data.dao

import androidx.room.*
import com.example.plzhealth.data.entity.MealEntity

@Dao
interface MealDao {
    @Query("SELECT * FROM meal_table WHERE date = :date")
    suspend fun getMealsByDate(date: String): List<MealEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: MealEntity)

    @Query("DELETE FROM meal_table")
    suspend fun clearAll()
}