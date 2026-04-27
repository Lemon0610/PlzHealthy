package com.example.plzhealth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.plzhealth.data.FoodItem

class AddMealDialogFragment(private val food: FoodItem) : DialogFragment() {

    private val viewModel: MealViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_addmealdialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName = view.findViewById<TextView>(R.id.tvPopupFoodName)
        val tvKcal = view.findViewById<TextView>(R.id.tvPopupFoodKcal)
        val spinner = view.findViewById<Spinner>(R.id.spinnerMealType)
        val btnAdd = view.findViewById<Button>(R.id.btnConfirmAdd)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)

        tvName.text = food.name
        tvKcal.text = "${food.kcal} Kcal"

        val defaultType = arguments?.getInt("defaultType") ?: 0
        spinner.setSelection(defaultType)

        btnClose.setOnClickListener {
            dismiss()
        }

        btnAdd.setOnClickListener {
            val selectedType = spinner.selectedItem.toString()

            viewModel.addMeal(food, selectedType)

            Toast.makeText(requireContext(), "${food.name}이(가) ${selectedType} 식단에 추가되었습니다.", Toast.LENGTH_SHORT).show()

            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}