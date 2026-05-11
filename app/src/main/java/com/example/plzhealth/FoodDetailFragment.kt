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

        val foodName = arguments?.getString("foodName") ?: ""
        val foodEnergy = arguments?.getString("foodEnergy") ?: "0"
        val foodProtein = arguments?.getString("foodProtein") ?: "0"
        val foodFat = arguments?.getString("foodFat") ?: "0"
        val foodCarbo = arguments?.getString("foodCarbo") ?: "0"
        val foodSugar = arguments?.getString("foodSugar") ?: "0"
        val foodFiber = arguments?.getString("foodFiber") ?: "0"
        val foodSodium = arguments?.getString("foodSodium") ?: "0"
        val foodCholesterol = arguments?.getString("foodCholesterol") ?: "0"

        // 텍스트 연결
        view.findViewById<TextView>(R.id.tvFoodName).text = foodName
        view.findViewById<TextView>(R.id.tvEnergy).text = foodEnergy
        view.findViewById<TextView>(R.id.tvProtein).text = foodProtein
        view.findViewById<TextView>(R.id.tvFat).text = "${foodFat} mg"
        view.findViewById<TextView>(R.id.tvCarbo).text = "${foodCarbo} g"
        view.findViewById<TextView>(R.id.tvSugar).text = "${foodSugar} mg"
        view.findViewById<TextView>(R.id.tvFiber).text = "${foodFiber} g"
        view.findViewById<TextView>(R.id.tvSodium).text = "${foodSodium} mg"
        view.findViewById<TextView>(R.id.tvCholesterol).text = "${foodCholesterol} mg"

        // 프로그레스바 연결
        val sodiumVal = foodSodium.toFloatOrNull() ?: 0f
        val fatVal = foodFat.toFloatOrNull() ?: 0f
        val sugarVal = foodSugar.toFloatOrNull() ?: 0f

        view.findViewById<ProgressBar>(R.id.pbSodium).progress = (sodiumVal / 20f).toInt().coerceIn(0, 100)
        view.findViewById<ProgressBar>(R.id.pbFat).progress = (fatVal * 2f).toInt().coerceIn(0, 100)
        view.findViewById<ProgressBar>(R.id.pbSugar).progress = (sugarVal * 5f).toInt().coerceIn(0, 100)

        // 뒤로가기
        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 버튼 (기능 없음)
        view.findViewById<Button>(R.id.btnAddMeal).setOnClickListener { }
        view.findViewById<Button>(R.id.btnRecommend).setOnClickListener { }
    }
}