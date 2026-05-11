package com.example.plzhealth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plzhealth.data.AppDatabase
import com.example.plzhealth.data.FoodItem
import com.example.plzhealth.data.RetrofitClient
import com.example.plzhealth.data.entity.FoodCategoryEntity
import com.example.plzhealth.data.toFoodItem
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.ImageView

class SearchFragment : Fragment() {

    private enum class Step { MAJOR, MIDDLE, RESULT }
    private var currentStep = Step.MAJOR
    private var selectedMajor = ""
    private var selectedMiddle = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val tvBack = view.findViewById<TextView>(R.id.tvBackCategory)
        val rvGrid = view.findViewById<RecyclerView>(R.id.rvCategoryGrid)
        val rvResult = view.findViewById<RecyclerView>(R.id.rvFoodResult)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val ivSearch = view.findViewById<ImageView>(R.id.ivSearch)

        ivSearch.setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) goToFoodList(query)
        }

        rvGrid.layoutManager = GridLayoutManager(requireContext(), 3)
        rvResult.layoutManager = LinearLayoutManager(requireContext())

        tvBack.setOnClickListener {
            when (currentStep) {
                Step.MIDDLE -> showMajorStep(view)
                Step.RESULT -> showMiddleStep(view, selectedMajor)
                else -> {}
            }
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) goToFoodList(query)
                true
            } else false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            initCategoriesIfNeeded()
            progressBar.visibility = View.GONE
            showMajorStep(view)
        }
    }

    private suspend fun initCategoriesIfNeeded() {
        val dao = AppDatabase.getDatabase(requireContext()).foodCategoryDao()
        if (dao.getCount() > 0) return

        try {
            val response = RetrofitClient.service.getNutriInfo(
                serviceKey = "4c0f8f4bc35efbe5d599f6c900f3475171464a453d2f1ad7ba568ffa5a15087b",
                foodName = null,
                numOfRows = 500
            )
            val items = response.response.body.items ?: return

            // null이나 빈 값 필터링 추가
            val categories = items
                .filter { !it.category.isNullOrBlank() && !it.subCategory.isNullOrBlank() }
                .map { FoodCategoryEntity(major = it.category!!, middle = it.subCategory!!) }
                .distinctBy { "${it.major}|${it.middle}" }

            if (categories.isEmpty()) {
                Log.e("SearchFragment", "카테고리 없음 - API 응답 확인 필요")
                return
            }

            dao.insertAll(categories)
            Log.d("SearchFragment", "카테고리 저장 완료: ${categories.size}개")
        } catch (e: Exception) {
            Log.e("SearchFragment", "카테고리 초기화 실패: ${e.message}")
        }
    }

    private fun showMajorStep(view: View) {
        currentStep = Step.MAJOR
        view.findViewById<TextView>(R.id.tvCategoryStep).text = "대분류 선택"
        view.findViewById<TextView>(R.id.tvBackCategory).visibility = View.GONE
        view.findViewById<RecyclerView>(R.id.rvFoodResult).visibility = View.GONE
        val rvGrid = view.findViewById<RecyclerView>(R.id.rvCategoryGrid)
        rvGrid.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(requireContext()).foodCategoryDao()
            val majors = dao.getDistinctMajor()
            rvGrid.adapter = CategoryGridAdapter(majors) { selected ->
                selectedMajor = selected
                showMiddleStep(view, selected)
            }
        }
    }

    private fun showMiddleStep(view: View, major: String) {
        currentStep = Step.MIDDLE
        view.findViewById<TextView>(R.id.tvCategoryStep).text = "$major > 중분류 선택"
        view.findViewById<TextView>(R.id.tvBackCategory).visibility = View.VISIBLE
        val rvGrid = view.findViewById<RecyclerView>(R.id.rvCategoryGrid)
        rvGrid.visibility = View.VISIBLE
        view.findViewById<RecyclerView>(R.id.rvFoodResult).visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(requireContext()).foodCategoryDao()
            val middles = dao.getDistinctMiddle(major)
            rvGrid.adapter = CategoryGridAdapter(middles) { selected ->
                selectedMiddle = selected
                showResultStep(view, selected)
            }
        }
    }

    private fun showResultStep(view: View, query: String) {
        currentStep = Step.RESULT
        view.findViewById<TextView>(R.id.tvCategoryStep).text = "$selectedMajor > $query"
        view.findViewById<TextView>(R.id.tvBackCategory).visibility = View.VISIBLE
        view.findViewById<RecyclerView>(R.id.rvCategoryGrid).visibility = View.GONE
        val rvResult = view.findViewById<RecyclerView>(R.id.rvFoodResult)
        rvResult.visibility = View.VISIBLE
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        viewLifecycleOwner.lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                val response = RetrofitClient.service.getNutriInfo(
                    serviceKey = "4c0f8f4bc35efbe5d599f6c900f3475171464a453d2f1ad7ba568ffa5a15087b",
                    foodName = query
                )
                val foods = response.response.body.items?.map { it.toFoodItem() } ?: emptyList()
                rvResult.adapter = FoodAdapter(foods) { food -> goToDetail(food) }
            } catch (e: Exception) {
                Log.e("SearchFragment", "식품 조회 실패: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun goToFoodList(query: String) {
        val fragment = FoodListFragment().apply {
            arguments = Bundle().apply { putString("query", query) }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun goToDetail(food: FoodItem) {
        val fragment = FoodDetailFragment().apply {
            arguments = Bundle().apply { putParcelable("selectedFood", food) }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}