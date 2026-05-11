package com.example.plzhealth.data.dao

import androidx.room.*
import com.example.plzhealth.data.entity.FoodCategoryEntity

@Dao
interface FoodCategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<FoodCategoryEntity>)

    @Query("SELECT DISTINCT major FROM food_category_table ORDER BY major")
    suspend fun getDistinctMajor(): List<String>

    @Query("SELECT DISTINCT middle FROM food_category_table WHERE major = :major ORDER BY middle")
    suspend fun getDistinctMiddle(major: String): List<String>

    @Query("SELECT COUNT(*) FROM food_category_table")
    suspend fun getCount(): Int
}