package com.example.plzhealth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FoodAdapter(
    private val foodList: List<Food>,
    private val onItemClick: (Food) -> Unit
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
        holder.tvHealthScore.text = "82"
        holder.tvFoodName.text = food.name
        holder.tvFoodEnergy.text = "${food.energy}Kcal"
        holder.itemView.setOnClickListener {
            onItemClick(food)
        }
    }

    override fun getItemCount() = foodList.size
}