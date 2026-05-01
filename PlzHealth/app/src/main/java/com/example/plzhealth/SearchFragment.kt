package com.example.plzhealth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plzhealth.data.FoodItem
import com.example.plzhealth.data.RetrofitClient
import com.example.plzhealth.data.toFoodItem
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var foodList: List<FoodItem> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val rvAllFoods = view.findViewById<RecyclerView>(R.id.rvAllFoods)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        rvAllFoods.layoutManager = LinearLayoutManager(requireContext())

        loadInitialData("닭가슴살", rvAllFoods, progressBar)

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

    private fun loadInitialData(keyword: String, recyclerView: RecyclerView, progressBar: ProgressBar?) {
        progressBar?.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.service.getNutriInfo(
                    serviceKey = "4c0f8f4bc35efbe5d599f6c900f3475171464a453d2f1ad7ba568ffa5a15087b",
                    foodName = keyword,
                    numOfRows = 20
                )

                val apiItems = response.response.body.items ?: emptyList()
                foodList = apiItems.map { it.toFoodItem() }

                recyclerView.adapter = FoodAdapter(foodList) { food ->
                    goToDetail(food)
                }
            } catch (e: Exception) {
            } finally {
                progressBar?.visibility = View.GONE
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
                putString("foodCategory", food.category)
                putString("foodSubCategory", food.subCategory)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}