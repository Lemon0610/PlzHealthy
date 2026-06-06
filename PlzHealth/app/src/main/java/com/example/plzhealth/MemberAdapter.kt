package com.example.plzhealth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.plzhealth.data.entity.UserEntity
import com.example.plzhealth.utils.DateUtils

class MemberAdapter(
    private var members: List<UserEntity>,
    private val onItemClick: (UserEntity) -> Unit
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMemberName: TextView = itemView.findViewById(R.id.tvMemberName)
        private val tvMemberAgeGender: TextView = itemView.findViewById(R.id.tvMemberAgeGender)
        private val tvMemberAllergies: TextView = itemView.findViewById(R.id.tvMemberAllergies)

        fun bind(user: UserEntity) {
            tvMemberName.text = user.name
            val age = DateUtils.calculateAge(user.birthDate)
            tvMemberAgeGender.text = " ($age / ${user.gender})"
            tvMemberAllergies.text = "알러지: ${user.allergies.ifEmpty { "없음" }}"

            itemView.setOnClickListener { onItemClick(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_member, parent, false
        )
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(members[position])
    }

    override fun getItemCount(): Int = members.size

    fun updateData(newMembers: List<UserEntity>) {
        this.members = newMembers
        notifyDataSetChanged()
    }
}