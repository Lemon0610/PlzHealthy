package com.example.plzhealth.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.plzhealth.data.dao.FoodCategoryDao
import com.example.plzhealth.data.dao.MealDao
import com.example.plzhealth.data.dao.UserDao
import com.example.plzhealth.data.entity.FoodCategoryEntity
import com.example.plzhealth.data.entity.MealEntity
import com.example.plzhealth.data.entity.UserEntity

@Database(
    entities = [UserEntity::class, MealEntity::class, FoodCategoryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun mealDao(): MealDao
    abstract fun foodCategoryDao(): FoodCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "plz_health_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}