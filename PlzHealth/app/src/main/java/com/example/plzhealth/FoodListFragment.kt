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
// [추가] 베이스 데이터 모델과 리더 임포트
import com.example.plzhealth.data.FoodItem
import com.example.plzhealth.utils.CsvFoodReader

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

        etSearch.setText(query)

        val allFoods = CsvFoodReader.loadFoods(requireContext())
        showResult(allFoods, query, tvNoResult, rvFoodList)

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
        allFoods: List<FoodItem>,
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
                        putString("foodName", food.name)
                        putDouble("kcal", food.kcal)
                        putDouble("protein", food.protein)
                        putDouble("fat", food.fat)
                        putDouble("carb", food.carb)
                        putDouble("sugar", food.sugar)
                        putDouble("fiber", food.fiber)
                        putDouble("sodium", food.sodium)
                        putDouble("saturatedFat", food.saturatedFat)
                        putInt("defaultType", arguments?.getInt("defaultType") ?: 0)
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