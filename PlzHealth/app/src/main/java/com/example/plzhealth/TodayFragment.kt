package com.example.plzhealth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plzhealth.utils.HealthScore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
            val breakfastList = allSelectedMeals.filter { it.mealType == "아침" }.map { it.food }
            val lunchList = allSelectedMeals.filter { it.mealType == "점심" }.map { it.food }
            val dinnerList = allSelectedMeals.filter { it.mealType == "저녁" }.map { it.food }

            rvBreakfast.adapter = FoodAdapter(breakfastList) { }
            rvLunch.adapter = FoodAdapter(lunchList) { }
            rvDinner.adapter = FoodAdapter(dinnerList) { }

            updateSummaryUI(view, allSelectedMeals)
        }

        view.findViewById<TextView>(R.id.btnAddBreakfast).setOnClickListener { goToSearch(0) }
        view.findViewById<TextView>(R.id.btnAddLunch).setOnClickListener { goToSearch(1) }
        view.findViewById<TextView>(R.id.btnAddDinner).setOnClickListener { goToSearch(2) }
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
                sodium = it.food.sodium,
                sugar = it.food.sugar,
                saturatedFat = it.food.saturatedFat,
                protein = it.food.protein,
                fiber = it.food.fiber,
                kcal = it.food.kcal
            )
        }

        val avgScore = totalScore / count

        tvTotalKcal.text = String.format("%.1f", totalKcal)
        tvFoodCount.text = count.toString()
        tvAverageScore.text = avgScore.toString()
    }

    private fun goToSearch(defaultType: Int) {
        val fragment = SearchFragment().apply {
            arguments = Bundle().apply {
                putInt("defaultType", defaultType)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}