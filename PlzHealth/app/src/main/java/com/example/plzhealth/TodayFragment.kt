package com.example.plzhealth

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plzhealth.data.FoodItem
import com.example.plzhealth.utils.HealthScore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodayFragment : Fragment() {

    private val viewModel: MealViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_today, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTodayDate = view.findViewById<TextView>(R.id.tvTodayDate)
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))
        tvTodayDate.text = today

        val rvBreakfast = view.findViewById<RecyclerView>(R.id.rvBreakfast)
        val rvLunch = view.findViewById<RecyclerView>(R.id.rvLunch)
        val rvDinner = view.findViewById<RecyclerView>(R.id.rvDinner)

        rvBreakfast.layoutManager = LinearLayoutManager(requireContext())
        rvLunch.layoutManager = LinearLayoutManager(requireContext())
        rvDinner.layoutManager = LinearLayoutManager(requireContext())

        viewModel.selectedMeals.observe(viewLifecycleOwner) { allSelectedMeals ->

            val breakfastMeals = allSelectedMeals
                .filter { it.mealType == "아침" }
                .asReversed()

            val lunchMeals = allSelectedMeals
                .filter { it.mealType == "점심" }
                .asReversed()

            val dinnerMeals = allSelectedMeals
                .filter { it.mealType == "저녁" }
                .asReversed()

            rvBreakfast.adapter = FoodAdapter(
                foodList = breakfastMeals.map { it.food },
                onItemClick = { food ->
                    goToFoodDetail(food)
                },
                onItemLongClick = { food ->
                    val meal = breakfastMeals.firstOrNull { it.food.code == food.code }
                    if (meal != null) showDeleteDialog(meal)
                }
            )

            rvLunch.adapter = FoodAdapter(
                foodList = lunchMeals.map { it.food },
                onItemClick = { food ->
                    goToFoodDetail(food)
                },
                onItemLongClick = { food ->
                    val meal = lunchMeals.firstOrNull { it.food.code == food.code }
                    if (meal != null) showDeleteDialog(meal)
                }
            )

            rvDinner.adapter = FoodAdapter(
                foodList = dinnerMeals.map { it.food },
                onItemClick = { food ->
                    goToFoodDetail(food)
                },
                onItemLongClick = { food ->
                    val meal = dinnerMeals.firstOrNull { it.food.code == food.code }
                    if (meal != null) showDeleteDialog(meal)
                }
            )

            updateSummaryUI(view, allSelectedMeals)
        }

        view.findViewById<TextView>(R.id.btnAddBreakfast)
            .setOnClickListener { goToAddCustomFood("아침") }

        view.findViewById<TextView>(R.id.btnAddLunch)
            .setOnClickListener { goToAddCustomFood("점심") }

        view.findViewById<TextView>(R.id.btnAddDinner)
            .setOnClickListener { goToAddCustomFood("저녁") }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTodayMeals()
    }

    private fun updateSummaryUI(view: View, meals: List<SelectedMeal>) {
        val tvTotalKcal = view.findViewById<TextView>(R.id.tvTotalKcal)
        val tvFoodCount = view.findViewById<TextView>(R.id.tvFoodCount)
        val tvAverageScore = view.findViewById<TextView>(R.id.tvAverageScore)

        if (meals.isEmpty()) {
            tvTotalKcal.text = "0.0"
            tvFoodCount.text = "0"
            tvAverageScore.text = "0"
            return
        }

        val totalKcal = meals.sumOf { it.food.kcal }
        val count = meals.size

        val totalScore = meals.sumOf {
            HealthScore.calculateScore(
                it.food.sodium,
                it.food.sugar,
                it.food.saturatedFat,
                it.food.protein,
                it.food.fiber,
                it.food.kcal
            )
        }

        val avgScore = totalScore / count

        tvTotalKcal.text = String.format(Locale.getDefault(), "%.1f", totalKcal)
        tvFoodCount.text = count.toString()
        tvAverageScore.text = avgScore.toString()
    }

    private fun showDeleteDialog(meal: SelectedMeal) {
        AlertDialog.Builder(requireContext())
            .setTitle("식단 삭제")
            .setMessage("${meal.food.name}을(를) 오늘의 식단에서 삭제할까요?")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                viewModel.deleteMeal(meal.id)
            }
            .show()
    }

    private fun goToAddCustomFood(mealType: String) {
        val fragment = AddCustomFoodFragment().apply {
            arguments = Bundle().apply {
                putString("mealType", mealType)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun goToFoodDetail(food: FoodItem) {
        val fragment = FoodDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable("selectedFood", food)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}