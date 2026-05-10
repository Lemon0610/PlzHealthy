package com.example.plzhealth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.plzhealth.data.FoodItem
import com.example.plzhealth.utils.HealthScore
import java.util.Locale

class FoodAdapter(
    private val foodList: List<FoodItem>,
    private val onItemClick: (FoodItem) -> Unit,
    private val onItemLongClick: ((FoodItem) -> Unit)? = null
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    class FoodViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHealthScore: TextView = view.findViewById(R.id.tvHealthScore)
        val tvFoodName: TextView = view.findViewById(R.id.tvFoodName)
        val tvFoodEnergy: TextView = view.findViewById(R.id.tvFoodEnergy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)

        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foodList[position]

        val score = HealthScore.calculateScore(
            food.sodium,
            food.sugar,
            food.saturatedFat,
            food.protein,
            food.fiber,
            food.kcal
        )

        holder.tvHealthScore.text = score.toString()
        holder.tvFoodName.text = food.name
        holder.tvFoodEnergy.text = String.format(Locale.getDefault(), "%.1fKcal", food.kcal)

        holder.itemView.setOnClickListener {
            onItemClick(food)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick?.invoke(food)
            true
        }
    }

    override fun getItemCount(): Int = foodList.size
}