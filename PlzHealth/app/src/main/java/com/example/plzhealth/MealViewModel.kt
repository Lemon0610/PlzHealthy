package com.example.plzhealth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.plzhealth.data.AppDatabase
import com.example.plzhealth.data.FoodItem
import com.example.plzhealth.data.entity.MealEntity
import com.example.plzhealth.utils.HealthScore
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SelectedMeal(
    val id: Int,
    val food: FoodItem,
    val mealType: String
)

data class DailyHealthScore(
    val date: String,
    val score: Int
)

class MealViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val mealDao = database.mealDao()

    private val todayDate: String =
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))

    private val _selectedMeals = MutableLiveData<MutableList<SelectedMeal>>(mutableListOf())
    val selectedMeals: LiveData<MutableList<SelectedMeal>> = _selectedMeals

    private val _dailyScores = MutableLiveData<List<DailyHealthScore>>(emptyList())
    val dailyScores: LiveData<List<DailyHealthScore>> = _dailyScores

    init {
        loadTodayMeals()
        loadDailyScores()
    }

    fun loadTodayMeals() {
        viewModelScope.launch {
            val entities = mealDao.getMealsByDate(todayDate)

            val list = entities.map { entity ->
                SelectedMeal(
                    id = entity.id,
                    food = FoodItem(
                        code = entity.code,
                        name = entity.foodName,
                        kcal = entity.kcal,
                        protein = entity.protein,
                        fat = entity.fat,
                        carb = entity.carb,
                        sugar = entity.sugar,
                        fiber = entity.fiber,
                        sodium = entity.sodium,
                        saturatedFat = entity.saturatedFat,
                        category = entity.category,
                        subCategory = entity.subCategory,
                        minorCategory = entity.minorCategory
                    ),
                    mealType = entity.mealType
                )
            }.toMutableList()

            _selectedMeals.postValue(list)
        }
    }

    fun loadDailyScores() {
        viewModelScope.launch {
            val entities = mealDao.getAllMeals()

            if (entities.isEmpty()) {
                _dailyScores.postValue(emptyList())
                return@launch
            }

            val result = entities
                .groupBy { it.date }
                .map { (date, mealList) ->
                    val averageScore = mealList.map { entity ->
                        HealthScore.calculateScore(
                            sodium = entity.sodium,
                            sugar = entity.sugar,
                            saturatedFat = entity.saturatedFat,
                            protein = entity.protein,
                            fiber = entity.fiber,
                            kcal = entity.kcal
                        )
                    }.average().toInt()

                    DailyHealthScore(
                        date = date,
                        score = averageScore
                    )
                }

            _dailyScores.postValue(result)
        }
    }

    fun addMeal(food: FoodItem, type: String) {
        viewModelScope.launch {
            val entity = MealEntity(
                date = todayDate,
                mealType = type,
                foodName = food.name,
                kcal = food.kcal,
                protein = food.protein,
                fat = food.fat,
                carb = food.carb,
                sugar = food.sugar,
                fiber = food.fiber,
                sodium = food.sodium,
                saturatedFat = food.saturatedFat,
                category = food.category,
                subCategory = food.subCategory,
                minorCategory = food.minorCategory,
                code = food.code
            )

            mealDao.insert(entity)
            loadTodayMeals()
            loadDailyScores()
        }
    }

    fun deleteMeal(mealId: Int) {
        viewModelScope.launch {
            mealDao.deleteMealById(mealId)
            loadTodayMeals()
            loadDailyScores()
        }
    }

    fun clearMeals() {
        viewModelScope.launch {
            mealDao.clearAll()
            _selectedMeals.postValue(mutableListOf())
            _dailyScores.postValue(emptyList())
        }
    }
}