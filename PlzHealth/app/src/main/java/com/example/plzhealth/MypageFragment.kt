package com.example.plzhealth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plzhealth.data.AppDatabase
import com.example.plzhealth.utils.DateUtils
import kotlinx.coroutines.launch

class MypageFragment : Fragment(R.layout.fragment_mypage) {

    private lateinit var tvMyName: TextView
    private lateinit var tvMyAgeGender: TextView
    private lateinit var tvMyAllergies: TextView
    private lateinit var tvMyDiseases: TextView
    private lateinit var rvMembers: RecyclerView
    private lateinit var cardMyInfo: View

    private val db by lazy { AppDatabase.getDatabase(requireContext()) }
    private lateinit var memberAdapter: MemberAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvMyName = view.findViewById(R.id.tvMyName)
        tvMyAgeGender = view.findViewById(R.id.tvMyAgeGender)
        tvMyAllergies = view.findViewById(R.id.tvMyAllergies)
        tvMyDiseases = view.findViewById(R.id.tvMyDiseases)
        rvMembers = view.findViewById(R.id.rvMembers)
        cardMyInfo = view.findViewById(R.id.cardMyInfo)

        setupRecyclerView()
        loadData()

        cardMyInfo.setOnClickListener {
            navigateToDetail(0, true)
        }

        view.findViewById<Button>(R.id.memberAdd).setOnClickListener {
            navigateToDetail(0, false)
        }
    }

    private fun setupRecyclerView() {
        memberAdapter = MemberAdapter(emptyList()) { user ->
            navigateToDetail(user.id, false)
        }

        rvMembers.apply {
            adapter = memberAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val myInfo = db.userDao().getMyInfo(true)

            if (myInfo != null) {
                tvMyName.text = myInfo.name
                tvMyAgeGender.text = "${DateUtils.calculateAge(myInfo.birthDate)}세 (${myInfo.gender})"
                tvMyAllergies.text = formatHealthInfo(myInfo.allergies)
                tvMyDiseases.text = formatHealthInfo(myInfo.diseases)
            } else {
                tvMyName.text = "미등록"
                tvMyAgeGender.text = "-"
                tvMyAllergies.text = "탭하여 정보를 등록하세요"
                tvMyDiseases.text = "탭하여 정보를 등록하세요"
            }

            val members = db.userDao().getMembers(false)
            memberAdapter.updateData(members)

            Log.d("Mypage", "멤버 수: ${members.size}")
        }
    }

    private fun formatHealthInfo(value: String): String {
        return if (value.isBlank()) "없음" else value
    }

    private fun navigateToDetail(userId: Int, isOwner: Boolean) {
        val fragment = UserDetailFragment().apply {
            arguments = Bundle().apply {
                putInt("USER_ID", userId)
                putBoolean("IS_OWNER", isOwner)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}