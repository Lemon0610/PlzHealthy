package com.example.plzhealth

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.plzhealth.data.FoodItem

class AddMealDialogFragment : DialogFragment() {

    private val viewModel: MealViewModel by activityViewModels()
    private var food: FoodItem? = null

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
    ): View? {
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
        val spinner = view.findViewById<Spinner>(R.id.spinnerMealType)
        val btnAdd = view.findViewById<Button>(R.id.btnConfirmAdd)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)

        tvName.text = currentFood.name
        tvKcal.text = "${currentFood.kcal} Kcal"

        val defaultType = arguments?.getInt("defaultType") ?: 0
        spinner.setSelection(defaultType)

        btnClose.setOnClickListener {
            dismiss()
        }

        btnAdd.setOnClickListener {
            val selectedType = spinner.selectedItem.toString()
            viewModel.addMeal(currentFood, selectedType)

            Toast.makeText(requireContext(), "${currentFood.name}이(가) ${selectedType} 식단에 추가되었습니다.", Toast.LENGTH_SHORT).show()

            dismiss()
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