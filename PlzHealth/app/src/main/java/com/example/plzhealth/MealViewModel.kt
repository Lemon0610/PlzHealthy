package com.example.plzhealth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.plzhealth.data.AppDatabase
import com.example.plzhealth.data.entity.MealEntity
import com.example.plzhealth.data.FoodItem
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SelectedMeal(
    val id: Int,
    val food: FoodItem,
    val mealType: String,
    val date: String
)

class MealViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val mealDao = database.mealDao()

    private val todayDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))

    private val _selectedMeals = MutableLiveData<MutableList<SelectedMeal>>(mutableListOf())
    val selectedMeals: LiveData<MutableList<SelectedMeal>> = _selectedMeals

    private val _allMealsForGraph = MutableLiveData<List<SelectedMeal>>(emptyList())
    val allMealsForGraph: LiveData<List<SelectedMeal>> = _allMealsForGraph

    init {
        loadTodayMeals()
        loadAllMeals()
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
                    mealType = entity.mealType,
                    date = entity.date
                )
            }.toMutableList()
            _selectedMeals.postValue(list)
        }
    }

    fun loadAllMeals() {
        viewModelScope.launch {
            val entities = mealDao.getAllMeals()
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
                    mealType = entity.mealType,
                    date = entity.date
                )
            }
            _allMealsForGraph.postValue(list)
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
            loadAllMeals()
        }
    }

    fun deleteMeal(mealId: Int) {
        viewModelScope.launch {
            mealDao.deleteMealById(mealId)
            loadTodayMeals()
            loadAllMeals()
        }
    }

    fun clearMeals() {
        viewModelScope.launch {
            mealDao.clearAll()
            _selectedMeals.postValue(mutableListOf())
            _allMealsForGraph.postValue(emptyList())
        }
    }
}