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
import com.example.plzhealth.data.FoodItem
import androidx.lifecycle.lifecycleScope
import com.example.plzhealth.data.RetrofitClient
import com.example.plzhealth.data.toFoodItem
import kotlinx.coroutines.launch

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

        fetchFoodData(query, tvNoResult, rvFoodList)

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val newQuery = etSearch.text.toString().trim()
                if (newQuery.isNotEmpty()) {
                    fetchFoodData(newQuery, tvNoResult, rvFoodList)
                }
                true
            } else false
        }
    }

    private fun fetchFoodData(query: String, tvNoResult: TextView, rvFoodList: RecyclerView) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.service.getNutriInfo(
                    serviceKey = "4c0f8f4bc35efbe5d599f6c900f3475171464a453d2f1ad7ba568ffa5a15087b",
                    foodName = query
                )

                val apiItems = response.response.body.items ?: emptyList()
                val foodItems = apiItems.map { it.toFoodItem() } // FoodItem으로 변환

                if (foodItems.isEmpty()) {
                    tvNoResult.visibility = View.VISIBLE
                    rvFoodList.visibility = View.GONE
                } else {
                    tvNoResult.visibility = View.GONE
                    rvFoodList.visibility = View.VISIBLE
                    rvFoodList.layoutManager = LinearLayoutManager(requireContext())

                    rvFoodList.adapter = FoodAdapter(foodItems) { food ->
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
                                putInt("defaultType", arguments?.getInt("defaultType") ?: 0)
                            }
                        }
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }
            } catch (e: Exception) {
                tvNoResult.text = "데이터를 불러오지 못했습니다."
                tvNoResult.visibility = View.VISIBLE
            }
        }
    }
}