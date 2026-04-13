package com.example.plzhealth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plzhealth.data.FoodItem
import com.example.plzhealth.utils.CsvFoodReader

class SearchFragment : Fragment() {

    private var allFoods: List<FoodItem> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        allFoods = CsvFoodReader.loadFoods(requireContext())

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val rvAllFoods = view.findViewById<RecyclerView>(R.id.rvAllFoods)

        rvAllFoods.layoutManager = LinearLayoutManager(requireContext())
        rvAllFoods.adapter = FoodAdapter(allFoods) { food ->
            goToDetail(food)
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    goToFoodList(query)
                }
                true
            } else {
                false
            }
        }
    }

    private fun goToFoodList(query: String) {
        val fragment = FoodListFragment().apply {
            arguments = Bundle().apply {
                putString("query", query)
                putInt("defaultType", arguments?.getInt("defaultType") ?: 0)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun goToDetail(food: FoodItem) {
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
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}