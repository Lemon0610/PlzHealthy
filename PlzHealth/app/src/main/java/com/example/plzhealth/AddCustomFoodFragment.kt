package com.example.plzhealth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.plzhealth.data.FoodItem

class AddCustomFoodFragment : Fragment() {

    private val viewModel: MealViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_add_custom_food,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etFoodName = view.findViewById<EditText>(R.id.etFoodName)
        val etKcal = view.findViewById<EditText>(R.id.etKcal)
        val etProtein = view.findViewById<EditText>(R.id.etProtein)
        val etSugar = view.findViewById<EditText>(R.id.etSugar)
        val etFiber = view.findViewById<EditText>(R.id.etFiber)
        val etSodium = view.findViewById<EditText>(R.id.etSodium)
        val etSaturatedFat = view.findViewById<EditText>(R.id.etSaturatedFat)

        val spinnerMealType = view.findViewById<Spinner>(R.id.spinnerMealType)
        val btnAddFood = view.findViewById<Button>(R.id.btnAddFood)

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

            val food = FoodItem(
                code = "custom_${System.currentTimeMillis()}",
                name = foodName,
                category = "직접추가",
                subCategory = "직접추가",
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
}