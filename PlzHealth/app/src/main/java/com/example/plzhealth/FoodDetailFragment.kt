package com.example.plzhealth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.plzhealth.data.FoodItem

class FoodDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_fooddetail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentArgs = arguments

        val foodName = fragmentArgs?.getString("foodName") ?: ""
        val foodCode = fragmentArgs?.getString("foodCode") ?: ""
        val kcal = fragmentArgs?.getDouble("kcal") ?: 0.0
        val protein = fragmentArgs?.getDouble("protein") ?: 0.0
        val fat = fragmentArgs?.getDouble("fat") ?: 0.0
        val carb = fragmentArgs?.getDouble("carb") ?: 0.0
        val sugar = fragmentArgs?.getDouble("sugar") ?: 0.0
        val fiber = fragmentArgs?.getDouble("fiber") ?: 0.0
        val sodium = fragmentArgs?.getDouble("sodium") ?: 0.0
        val saturatedFat = fragmentArgs?.getDouble("saturatedFat") ?: 0.0
        val category = fragmentArgs?.getString("foodCategory") ?: ""
        val subCategory = fragmentArgs?.getString("foodSubCategory") ?: ""
        val defaultMealType = fragmentArgs?.getInt("defaultType") ?: 0

        // 2. UI 데이터 반영
        view.findViewById<TextView>(R.id.tvFoodName).text = foodName
        view.findViewById<TextView>(R.id.tvEnergy).text = kcal.toString()
        view.findViewById<TextView>(R.id.tvProtein).text = protein.toString()
        view.findViewById<TextView>(R.id.tvFat).text = "${fat} g"
        view.findViewById<TextView>(R.id.tvCarbo).text = "${carb} g"
        view.findViewById<TextView>(R.id.tvSugar).text = "${sugar} g"
        view.findViewById<TextView>(R.id.tvFiber).text = "${fiber} g"
        view.findViewById<TextView>(R.id.tvSodium).text = "${sodium} mg"

        view.findViewById<ProgressBar>(R.id.pbSodium).progress = (sodium / 10).toInt().coerceIn(0, 100)
        view.findViewById<ProgressBar>(R.id.pbFat).progress = (fat * 2).toInt().coerceIn(0, 100)
        view.findViewById<ProgressBar>(R.id.pbSugar).progress = (sugar * 5).toInt().coerceIn(0, 100)

        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<Button>(R.id.btnRecommend).setOnClickListener {
            val fragment = RecommendFragment().apply {
                arguments = Bundle().apply {
                    putString("category", category)
                    putString("baseName", foodName)
                    putDouble("sodium", sodium)
                    putDouble("sugar", sugar)
                    putDouble("saturatedFat", saturatedFat)
                    putDouble("protein", protein)
                    putDouble("fiber", fiber)
                    putDouble("kcal", kcal)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<Button>(R.id.btnAddMeal).setOnClickListener {
            val currentFood = FoodItem(
                code = foodCode,
                name = foodName,
                kcal = kcal,
                protein = protein,
                fat = fat,
                carb = carb,
                sugar = sugar,
                fiber = fiber,
                sodium = sodium,
                saturatedFat = saturatedFat,
                category = category,
                subCategory = subCategory
            )

            val dialog = AddMealDialogFragment(currentFood)

            val dialogArgs = Bundle()
            dialogArgs.putInt("defaultType", defaultMealType)
            dialog.arguments = dialogArgs

            dialog.show(parentFragmentManager, "AddMealDialog")
        }
    }
}