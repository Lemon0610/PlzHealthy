package com.example.plzhealth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FoodListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_foodlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val query = arguments?.getString("query") ?: ""

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val tvNoResult = view.findViewById<TextView>(R.id.tvNoResult)
        val rvFoodList = view.findViewById<RecyclerView>(R.id.rvFoodList)

        // 검색어 표시
        etSearch.setText(query)

        // 데이터 로드 및 검색
        val allFoods = FoodDataLoader.loadFoods(requireContext())
        showResult(allFoods, query, tvNoResult, rvFoodList)

        // 검색창에서 재검색
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val newQuery = etSearch.text.toString().trim()
                if (newQuery.isNotEmpty()) {
                    showResult(allFoods, newQuery, tvNoResult, rvFoodList)
                }
                true
            } else {
                false
            }
        }
    }

    private fun showResult(
        allFoods: List<Food>,
        query: String,
        tvNoResult: TextView,
        rvFoodList: RecyclerView
    ) {
        val result = allFoods.filter { it.name.contains(query, ignoreCase = true) }

        if (result.isEmpty()) {
            tvNoResult.visibility = View.VISIBLE
            rvFoodList.visibility = View.GONE
        } else {
            tvNoResult.visibility = View.GONE
            rvFoodList.visibility = View.VISIBLE
            rvFoodList.layoutManager = LinearLayoutManager(requireContext())
            rvFoodList.adapter = FoodAdapter(result) { food ->
                val fragment = FoodDetailFragment().apply {
                    arguments = Bundle().apply {
                        putString("foodCode", food.code)
                        putString("foodName", food.name)
                        putString("foodCategory", food.category)
                        putString("foodEnergy", food.energy)
                        putString("foodProtein", food.protein)
                        putString("foodFat", food.fat)
                        putString("foodCarbo", food.carbohydrate)
                        putString("foodSugar", food.sugar)
                        putString("foodFiber", food.fiber)
                        putString("foodCalcium", food.calcium)
                        putString("foodSodium", food.sodium)
                        putString("foodVitaminC", food.vitaminC)
                        putString("foodCholesterol", food.cholesterol)
                        putString("foodSaturatedFat", food.saturatedFat)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}