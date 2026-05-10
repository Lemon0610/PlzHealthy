package com.example.plzhealth.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.plzhealth.data.entity.MealEntity

@Dao
interface MealDao {

    @Query("SELECT * FROM meal_table WHERE date = :date")
    suspend fun getMealsByDate(date: String): List<MealEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: MealEntity)

    @Query("DELETE FROM meal_table WHERE id = :mealId")
    suspend fun deleteMealById(mealId: Int)

    @Query("DELETE FROM meal_table")
    suspend fun clearAll()
}