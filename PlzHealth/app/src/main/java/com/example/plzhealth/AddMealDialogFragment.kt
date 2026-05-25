package com.example.plzhealth

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.plzhealth.data.FoodItem
import java.util.Locale

class AddMealDialogFragment : DialogFragment() {

    private val viewModel: MealViewModel by activityViewModels()
    private var food: FoodItem? = null

    private val servingOptions = listOf(
        "1인분 (100g)",
        "1/2인분 (50g)",
        "1/3인분 (33g)",
        "1/4인분 (25g)",
        "직접 입력"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        food = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("selectedFood", FoodItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("selectedFood")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_addmealdialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentFood = food ?: run {
            Toast.makeText(context, "식품 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        val tvName = view.findViewById<TextView>(R.id.tvPopupFoodName)
        val tvKcal = view.findViewById<TextView>(R.id.tvPopupFoodKcal)
        val spinnerServingAmount = view.findViewById<Spinner>(R.id.spinnerServingAmount)
        val etServingGram = view.findViewById<EditText>(R.id.etServingGram)
        val spinnerMealType = view.findViewById<Spinner>(R.id.spinnerMealType)
        val btnAdd = view.findViewById<Button>(R.id.btnConfirmAdd)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)

        tvName.text = currentFood.name
        tvKcal.text = String.format(Locale.getDefault(), "%.1f Kcal / 100g", currentFood.kcal)

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

        val defaultType = arguments?.getInt("defaultType") ?: 0
        spinnerMealType.setSelection(defaultType)

        btnClose.setOnClickListener {
            dismiss()
        }

        btnAdd.setOnClickListener {
            val selectedType = spinnerMealType.selectedItem.toString()

            val servingGram = etServingGram.text
                .toString()
                .trim()
                .toDoubleOrNull()

            if (servingGram == null || servingGram <= 0.0) {
                Toast.makeText(
                    requireContext(),
                    "섭취량을 올바르게 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val adjustedFood = adjustFoodByServingGram(
                food = currentFood,
                servingGram = servingGram
            )

            viewModel.addMeal(adjustedFood, selectedType)

            Toast.makeText(
                requireContext(),
                "${adjustedFood.name}이(가) ${selectedType} 식단에 추가되었습니다.",
                Toast.LENGTH_SHORT
            ).show()

            dismiss()
        }
    }

    private fun adjustFoodByServingGram(
        food: FoodItem,
        servingGram: Double
    ): FoodItem {
        val ratio = servingGram / 100.0
        val servingText = formatServingGram(servingGram)

        return food.copy(
            name = "${food.name} (${servingText}g)",
            kcal = food.kcal * ratio,
            protein = food.protein * ratio,
            fat = food.fat * ratio,
            carb = food.carb * ratio,
            sugar = food.sugar * ratio,
            fiber = food.fiber * ratio,
            sodium = food.sodium * ratio,
            saturatedFat = food.saturatedFat * ratio
        )
    }

    private fun formatServingGram(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", value)
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
