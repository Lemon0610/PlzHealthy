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

class SearchFragment : Fragment() {

    private val dummyFoods = listOf(
        Food("001", "연어구이", "수산물", "생선류", "100g", "208", "25", "10", "0", "0", "0", "20", "60", "0", "55", "3"),
        Food("002", "닭가슴살", "육류", "가금류", "100g", "165", "31", "4", "0", "0", "0", "15", "70", "0", "85", "1"),
        Food("003", "현미밥", "곡류", "밥류", "100g", "130", "3", "1", "28", "0", "2", "10", "5", "0", "0", "0"),
        Food("004", "브로콜리", "채소류", "십자화과", "100g", "34", "3", "0", "7", "2", "3", "47", "33", "89", "0", "0"),
        Food("005", "바나나", "과일류", "열대과일", "100g", "89", "1", "0", "23", "12", "3", "5", "1", "9", "0", "0"),
        Food("006", "고구마", "채소류", "뿌리채소", "100g", "86", "2", "0", "20", "4", "3", "30", "55", "3", "0", "0"),
        Food("007", "두부", "두류", "콩제품", "100g", "76", "8", "4", "2", "0", "1", "130", "10", "0", "0", "1"),
        Food("008", "계란", "난류", "달걀", "100g", "155", "13", "11", "1", "0", "0", "56", "124", "0", "373", "3"),
        Food("009", "아보카도", "과일류", "열대과일", "100g", "160", "2", "15", "9", "1", "7", "12", "7", "10", "0", "2"),
        Food("010", "오트밀", "곡류", "귀리", "100g", "389", "17", "7", "66", "1", "11", "54", "6", "0", "0", "1"),
    )

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

        // 전체 목록 보여주기
        rvAllFoods.layoutManager = LinearLayoutManager(requireContext())
        rvAllFoods.adapter = FoodAdapter(dummyFoods) { food ->
            goToDetail(food)
        }

        // 검색 버튼 눌렀을 때
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
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun goToDetail(food: Food) {
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