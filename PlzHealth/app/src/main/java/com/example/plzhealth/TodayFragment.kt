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
        // fragment_today 레이아웃 인플레이트
        return inflater.inflate(R.layout.fragment_today, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 날짜 표시
        val tvTodayDate = view.findViewById<TextView>(R.id.tvTodayDate)
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))
        tvTodayDate.text = today

        // 2. 리사이클러뷰 초기화
        val rvBreakfast = view.findViewById<RecyclerView>(R.id.rvBreakfast)
        val rvLunch = view.findViewById<RecyclerView>(R.id.rvLunch)
        val rvDinner = view.findViewById<RecyclerView>(R.id.rvDinner)

        rvBreakfast.layoutManager = LinearLayoutManager(requireContext())
        rvLunch.layoutManager = LinearLayoutManager(requireContext())
        rvDinner.layoutManager = LinearLayoutManager(requireContext())

        // 3. ViewModel 관찰 (식단 데이터 변경 시 자동 갱신)
        viewModel.selectedMeals.observe(viewLifecycleOwner) { allSelectedMeals ->
            // 타입별 필터링
            val breakfastList = allSelectedMeals.filter { it.mealType == "아침" }.map { it.food }
            val lunchList = allSelectedMeals.filter { it.mealType == "점심" }.map { it.food }
            val dinnerList = allSelectedMeals.filter { it.mealType == "저녁" }.map { it.food }

            // 어댑터 연결
            rvBreakfast.adapter = FoodAdapter(breakfastList) { /* 클릭 시 상세 이동 로직 추가 가능 */ }
            rvLunch.adapter = FoodAdapter(lunchList) { /* 클릭 시 상세 이동 로직 추가 가능 */ }
            rvDinner.adapter = FoodAdapter(dinnerList) { /* 클릭 시 상세 이동 로직 추가 가능 */ }

            // 상단 요약 UI 업데이트
            updateSummaryUI(view, allSelectedMeals)
        }

        // 4. 추가 버튼 클릭 리스너 (수정된 XML ID 적용 및 Type 전달)
        // 아침=0, 점심=1, 저녁=2
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

        // 건강 점수 평균 계산
        val totalScore = meals.sumOf {
            HealthScore.calculateScore(
                it.food.sodium, it.food.sugar, it.food.saturatedFat,
                it.food.protein, it.food.fiber, it.food.kcal
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