package com.example.plzhealth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.plzhealth.data.AppDatabase
import com.example.plzhealth.data.FoodItem
import kotlinx.coroutines.launch
import java.util.Locale

class AddCustomFoodFragment : Fragment() {

    private val viewModel: MealViewModel by activityViewModels()

    private val db by lazy { AppDatabase.getDatabase(requireContext()) }

    private val servingOptions = listOf(
        "1인분 (100g)",
        "1/2인분 (50g)",
        "1/3인분 (33g)",
        "1/4인분 (25g)",
        "직접 입력"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_addcustomfood, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etFoodName = view.findViewById<EditText>(R.id.etFoodName)
        val spinnerServingAmount = view.findViewById<Spinner>(R.id.spinnerServingAmount)
        val etServingGram = view.findViewById<EditText>(R.id.etServingGram)
        val etKcal = view.findViewById<EditText>(R.id.etKcal)
        val etProtein = view.findViewById<EditText>(R.id.etProtein)
        val etSugar = view.findViewById<EditText>(R.id.etSugar)
        val etFiber = view.findViewById<EditText>(R.id.etFiber)
        val etSodium = view.findViewById<EditText>(R.id.etSodium)
        val etSaturatedFat = view.findViewById<EditText>(R.id.etSaturatedFat)

        val spinnerMajor = view.findViewById<Spinner>(R.id.spinnerMajor)
        val spinnerMiddle = view.findViewById<Spinner>(R.id.spinnerMiddle)
        val spinnerMinor = view.findViewById<Spinner>(R.id.spinnerMinor)

        val spinnerMealType = view.findViewById<Spinner>(R.id.spinnerMealType)
        val btnAddFood = view.findViewById<Button>(R.id.btnAddFood)

        spinnerServingAmount.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            servingOptions
        )

        etServingGram.setText("100")
        etServingGram.isEnabled = false

        spinnerServingAmount.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                itemView: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> {
                        etServingGram.setText("100")
                        etServingGram.isEnabled = false
                    }

                    1 -> {
                        etServingGram.setText("50")
                        etServingGram.isEnabled = false
                    }

                    2 -> {
                        etServingGram.setText("33")
                        etServingGram.isEnabled = false
                    }

                    3 -> {
                        etServingGram.setText("25")
                        etServingGram.isEnabled = false
                    }

                    4 -> {
                        etServingGram.setText("")
                        etServingGram.hint = "직접 입력(g)"
                        etServingGram.isEnabled = true
                        etServingGram.requestFocus()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val mealTypes = listOf("아침", "점심", "저녁")
        spinnerMealType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            mealTypes
        )

        val defaultMealType = arguments?.getString("mealType") ?: "아침"
        val defaultIndex = mealTypes.indexOf(defaultMealType)
        if (defaultIndex >= 0) {
            spinnerMealType.setSelection(defaultIndex)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val majorList = db.foodCategoryDao().getDistinctMajor()

            if (majorList.isNotEmpty()) {
                spinnerMajor.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    majorList
                )

                spinnerMajor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        itemView: View?,
                        position: Int,
                        id: Long
                    ) {
                        val selectedMajor = majorList[position]

                        viewLifecycleOwner.lifecycleScope.launch {
                            val middleList = db.foodCategoryDao().getDistinctMiddle(selectedMajor)

                            spinnerMiddle.adapter = ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_spinner_dropdown_item,
                                middleList
                            )

                            spinnerMiddle.onItemSelectedListener =
                                object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(
                                        parent: AdapterView<*>?,
                                        itemView: View?,
                                        pos: Int,
                                        id: Long
                                    ) {
                                        val selectedMiddle = middleList.getOrNull(pos) ?: "미분류"

                                        viewLifecycleOwner.lifecycleScope.launch {
                                            val minorList = db.foodCategoryDao()
                                                .getDistinctMinor(selectedMajor, selectedMiddle)

                                            spinnerMinor.adapter = ArrayAdapter(
                                                requireContext(),
                                                android.R.layout.simple_spinner_dropdown_item,
                                                minorList
                                            )
                                        }
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                                }
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }

        btnAddFood.setOnClickListener {
            val foodName = etFoodName.text.toString().trim()

            if (foodName.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "식품 이름을 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val servingGram = etServingGram.text
                .toString()
                .trim()
                .toDoubleOrNull() ?: 100.0

            if (servingGram <= 0.0) {
                Toast.makeText(
                    requireContext(),
                    "섭취량을 올바르게 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val selectedMajor = spinnerMajor.selectedItem?.toString() ?: "미분류"
            val selectedMiddle = spinnerMiddle.selectedItem?.toString() ?: "미분류"
            val selectedMinor = spinnerMinor.selectedItem?.toString() ?: "미분류"

            val servingText = formatServingGram(servingGram)

            val food = FoodItem(
                code = "custom_${System.currentTimeMillis()}",
                name = "$foodName (${servingText}g)",
                category = selectedMajor,
                subCategory = selectedMiddle,
                minorCategory = selectedMinor,
                kcal = etKcal.text.toString().toDoubleOrNull() ?: 0.0,
                protein = etProtein.text.toString().toDoubleOrNull() ?: 0.0,
                fat = 0.0,
                carb = 0.0,
                sugar = etSugar.text.toString().toDoubleOrNull() ?: 0.0,
                fiber = etFiber.text.toString().toDoubleOrNull() ?: 0.0,
                sodium = etSodium.text.toString().toDoubleOrNull() ?: 0.0,
                saturatedFat = etSaturatedFat.text.toString().toDoubleOrNull() ?: 0.0
            )

            val mealType = spinnerMealType.selectedItem.toString()

            viewModel.addMeal(food, mealType)

            Toast.makeText(
                requireContext(),
                "식품이 추가되었습니다.",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()
        }
    }

    private fun formatServingGram(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", value)
        }
    }
}