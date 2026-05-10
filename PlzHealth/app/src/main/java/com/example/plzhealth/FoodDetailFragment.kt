package com.example.plzhealth

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.plzhealth.data.FoodItem
import com.example.plzhealth.utils.HealthScore
import java.util.Locale

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

        val food = getFoodItemFromArguments()
        val defaultMealType = arguments?.getInt("defaultType") ?: 0

        if (food == null) {
            Toast.makeText(requireContext(), "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        val calculatedScore = HealthScore.calculateScore(
            sodium = food.sodium,
            sugar = food.sugar,
            saturatedFat = food.saturatedFat,
            protein = food.protein,
            fiber = food.fiber,
            kcal = food.kcal
        )

        view.findViewById<TextView>(R.id.tvHealthScore).text = calculatedScore.toString()
        view.findViewById<TextView>(R.id.tvHealthScoreCircle).text = calculatedScore.toString()
        view.findViewById<TextView>(R.id.tvFoodName).text = food.name

        view.findViewById<TextView>(R.id.tvEnergy).text =
            String.format(Locale.getDefault(), "%.1f Kcal", food.kcal)

        view.findViewById<TextView>(R.id.tvProtein).text =
            String.format(Locale.getDefault(), "%.1f g", food.protein)

        view.findViewById<TextView>(R.id.tvFat).text =
            String.format(Locale.getDefault(), "%.1f g", food.fat)

        view.findViewById<TextView>(R.id.tvCarbo).text =
            String.format(Locale.getDefault(), "%.1f g", food.carb)

        view.findViewById<TextView>(R.id.tvSugar).text =
            String.format(Locale.getDefault(), "%.1f g", food.sugar)

        view.findViewById<TextView>(R.id.tvFiber).text =
            String.format(Locale.getDefault(), "%.1f g", food.fiber)

        view.findViewById<TextView>(R.id.tvSodium).text =
            String.format(Locale.getDefault(), "%.1f mg", food.sodium)

        view.findViewById<ProgressBar>(R.id.pbSodium).progress =
            (food.sodium / 10).toInt().coerceIn(0, 100)

        view.findViewById<ProgressBar>(R.id.pbFat).progress =
            (food.fat * 2).toInt().coerceIn(0, 100)

        view.findViewById<ProgressBar>(R.id.pbSugar).progress =
            (food.sugar * 5).toInt().coerceIn(0, 100)

        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<Button>(R.id.btnRecommend).setOnClickListener {
            val fragment = RecommendFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("selectedFood", food)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<Button>(R.id.btnAddMeal).setOnClickListener {
            val dialog = AddMealDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("selectedFood", food)
                    putInt("defaultType", defaultMealType)
                }
            }

            dialog.show(parentFragmentManager, "AddMealDialog")
        }
    }

    private fun getFoodItemFromArguments(): FoodItem? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("selectedFood", FoodItem::class.java)
                ?: arguments?.getParcelable("food", FoodItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("selectedFood")
                ?: arguments?.getParcelable("food")
        }
    }
}