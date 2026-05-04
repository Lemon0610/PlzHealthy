package com.example.plzhealth

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
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

    private val majorCategories = listOf(
        "전체",
        "과자류·빵류 또는 떡류",
        "즉석식품류",
        "음료류",
        "식육가공품 및 포장육",
        "조미식품",
        "농산가공식품류",
        "수산가공식품류",
        "절임류 또는 조림류",
        "면류",
        "유가공품류",
        "코코아가공품류 또는 초콜릿류",
        "식용유지류",
        "빙과류",
        "장류",
        "두부류 또는 묵류",
        "잼류",
        "당류",
        "알가공품류",
        "주류",
        "기타식품류"
    )

    private var selectedChip: TextView? = null

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

        // 칩 세팅
        setupCategoryChips(view, rvAllFoods, progressBar)

        loadInitialData("닭가슴살", rvAllFoods, progressBar)

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) goToFoodList(query)
                true
            } else false
        }
    }

    private fun setupCategoryChips(view: View, rvAllFoods: RecyclerView, progressBar: ProgressBar?) {
        val llChips = view.findViewById<LinearLayout>(R.id.llCategoryChips)

        majorCategories.forEach { category ->
            val chip = TextView(requireContext()).apply {
                text = category
                textSize = 13f
                setPadding(14.dpToPx(), 8.dpToPx(), 14.dpToPx(), 8.dpToPx())
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 8.dpToPx(), 0) }
                layoutParams = params
                isClickable = true
                isFocusable = true

                // 전체 칩은 처음부터 선택 상태
                if (category == "전체") {
                    setChipSelected(this)
                    selectedChip = this
                } else {
                    setChipUnselected(this)
                }

                setOnClickListener {
                    selectedChip?.let { setChipUnselected(it) }
                    setChipSelected(this)
                    selectedChip = this

                    if (category == "전체") {
                        loadInitialData("닭가슴살", rvAllFoods, progressBar)
                    } else {
                        goToFoodList(category)
                    }
                }
            }
            llChips.addView(chip)
        }
    }

    private fun setChipSelected(chip: TextView) {
        chip.setBackgroundResource(R.drawable.bg_chip_selected)
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        chip.setTypeface(null, Typeface.BOLD)
    }

    private fun setChipUnselected(chip: TextView) {
        chip.setBackgroundResource(R.drawable.bg_chip_unselected)
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.GoodBlack))
        chip.setTypeface(null, Typeface.NORMAL)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

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
                recyclerView.adapter = FoodAdapter(foodList) { food -> goToDetail(food) }
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