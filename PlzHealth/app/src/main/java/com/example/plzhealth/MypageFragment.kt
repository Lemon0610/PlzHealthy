package com.example.plzhealth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.plzhealth.data.AppDatabase
import com.example.plzhealth.utils.DateUtils
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.TextView
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import android.util.Log

class MypageFragment : Fragment(R.layout.fragment_mypage) {
    private lateinit var tvMyName: TextView
    private lateinit var tvMyAgeGender: TextView
    private lateinit var tvMyAllergies: TextView
    private lateinit var rvMembers: RecyclerView
    private lateinit var cardMyInfo: View

    private val db by lazy { AppDatabase.getDatabase(requireContext()) }
    private lateinit var memberAdapter: MemberAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvMyName = view.findViewById(R.id.tvMyName)
        tvMyAgeGender = view.findViewById(R.id.tvMyAgeGender)
        tvMyAllergies = view.findViewById(R.id.tvMyAllergies)
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
                // 새 레이아웃: 라벨 열 고정이므로 값만 표시
                tvMyName.text = myInfo.name
                tvMyAgeGender.text = "${DateUtils.calculateAge(myInfo.birthDate)}세 (${myInfo.gender})"
                tvMyAllergies.text = if (myInfo.allergies.isNullOrBlank()) "없음" else myInfo.allergies
            } else {
                tvMyName.text = "미등록"
                tvMyAgeGender.text = "-"
                tvMyAllergies.text = "탭하여 정보를 등록하세요"
            }

            val members = db.userDao().getMembers(false)
            memberAdapter.updateData(members)
            Log.d("Mypage", "멤버 수: ${members.size}")
        }
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