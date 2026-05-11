package com.example.plzhealth

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.plzhealth.data.AppDatabase
import com.example.plzhealth.data.entity.UserEntity
import com.example.plzhealth.utils.DateUtils
import kotlinx.coroutines.launch

class UserDetailFragment : Fragment(R.layout.fragment_user_detail) {
    private lateinit var etDetailName: EditText
    private lateinit var etDetailBirth: EditText
    private lateinit var tvDisplayAge: TextView
    private lateinit var btnSave: Button
    private lateinit var gridAllergies: GridLayout
    private lateinit var gridDiseases: GridLayout
    private lateinit var rbMale: RadioButton
    private lateinit var rbFemale: RadioButton
    private lateinit var rgGender: RadioGroup
    private lateinit var etDetailMemo: EditText
    private lateinit var btnDelete: Button

    private val db by lazy { AppDatabase.getDatabase(requireContext()) }
    private var userId: Int = 0
    private var isOwnerMode: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = arguments?.getInt("USER_ID", 0) ?: 0
        isOwnerMode = arguments?.getBoolean("IS_OWNER", false) ?: false

        etDetailName = view.findViewById(R.id.etDetailName)
        etDetailBirth = view.findViewById(R.id.etDetailBirth)
        tvDisplayAge = view.findViewById(R.id.tvDisplayAge)
        btnSave = view.findViewById(R.id.btnSave)
        gridAllergies = view.findViewById(R.id.gridAllergies)
        gridDiseases = view.findViewById(R.id.gridDiseases)
        rbMale = view.findViewById(R.id.rbMale)
        rbFemale = view.findViewById(R.id.rbFemale)
        rgGender = view.findViewById(R.id.rgGender)
        etDetailMemo = view.findViewById(R.id.etDetailMemo)
        btnDelete = view.findViewById(R.id.btnDelete)

        // 초기 남성 선택 상태
        rbMale.setBackgroundResource(R.drawable.bg_chip_selected)
        rbMale.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        rgGender.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbMale -> {
                    rbMale.setBackgroundResource(R.drawable.bg_chip_selected)
                    rbMale.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    rbFemale.setBackgroundResource(R.drawable.bg_chip_unselected)
                    rbFemale.setTextColor(ContextCompat.getColor(requireContext(), R.color.GoodBlack))
                }
                R.id.rbFemale -> {
                    rbFemale.setBackgroundResource(R.drawable.bg_chip_selected)
                    rbFemale.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    rbMale.setBackgroundResource(R.drawable.bg_chip_unselected)
                    rbMale.setTextColor(ContextCompat.getColor(requireContext(), R.color.GoodBlack))
                }
            }
        }

        // 칩 체크박스 클릭 리스너
        setupChipCheckboxes(gridAllergies)
        setupChipCheckboxes(gridDiseases)

        if (userId != 0 || isOwnerMode) {
            loadUserData()
            btnDelete.visibility = View.VISIBLE
            if (isOwnerMode) btnDelete.visibility = View.GONE
        } else {
            btnDelete.visibility = View.GONE
        }

        etDetailBirth.doAfterTextChanged { text ->
            if (text?.length == 8) {
                val rawDate = text.toString()
                val formattedDate = "${rawDate.substring(0,4)}-${rawDate.substring(4,6)}-${rawDate.substring(6,8)}"
                tvDisplayAge.text = "연산된 나이: ${DateUtils.calculateAge(formattedDate)}"
            }
        }

        btnSave.setOnClickListener { saveUserInfo() }
        btnDelete.setOnClickListener { deleteUserInfo() }
    }

    private fun setupChipCheckboxes(grid: GridLayout) {
        for (i in 0 until grid.childCount) {
            val child = grid.getChildAt(i)
            if (child is CheckBox) {
                child.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        child.setBackgroundResource(R.drawable.bg_chip_selected)
                        child.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    } else {
                        child.setBackgroundResource(R.drawable.bg_chip_unselected)
                        child.setTextColor(ContextCompat.getColor(requireContext(), R.color.GoodBlack))
                    }
                }
            }
        }
    }

    private fun loadUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = if (isOwnerMode) db.userDao().getMyInfo()
            else db.userDao().getUserById(userId)

            user?.let {
                userId = it.id
                etDetailName.setText(it.name)
                etDetailBirth.setText(it.birthDate)
                etDetailMemo.setText(it.memo)

                // 성별 칩 상태 업데이트
                if (it.gender == "남성") {
                    rbMale.isChecked = true
                    rbMale.setBackgroundResource(R.drawable.bg_chip_selected)
                    rbMale.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    rbFemale.setBackgroundResource(R.drawable.bg_chip_unselected)
                    rbFemale.setTextColor(ContextCompat.getColor(requireContext(), R.color.GoodBlack))
                } else {
                    rbFemale.isChecked = true
                    rbFemale.setBackgroundResource(R.drawable.bg_chip_selected)
                    rbFemale.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    rbMale.setBackgroundResource(R.drawable.bg_chip_unselected)
                    rbMale.setTextColor(ContextCompat.getColor(requireContext(), R.color.GoodBlack))
                }

                setupCheckboxes(it.allergies, gridAllergies)
                setupCheckboxes(it.diseases, gridDiseases)
            }
        }
    }

    private fun saveUserInfo() {
        val name = etDetailName.text.toString()
        val birth = etDetailBirth.text.toString()

        if (name.isEmpty() || birth.length < 8) {
            Toast.makeText(requireContext(), "이름과 생년월일을 확인해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val user = UserEntity(
            id = userId,
            name = name,
            birthDate = birth,
            gender = if (rbMale.isChecked) "남성" else "여성",
            allergies = getCheckedTexts(gridAllergies),
            diseases = getCheckedTexts(gridDiseases),
            memo = etDetailMemo.text.toString(),
            isOwner = isOwnerMode
        )

        lifecycleScope.launch {
            db.userDao().insert(user)
            parentFragmentManager.popBackStack()
        }
    }

    private fun deleteUserInfo() {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = db.userDao().getUserById(userId)
            user?.let { db.userDao().delete(it) }
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupCheckboxes(data: String, grid: GridLayout) {
        val list = data.split(", ")
        for (i in 0 until grid.childCount) {
            val child = grid.getChildAt(i)
            if (child is CheckBox && list.contains(child.text.toString())) {
                child.isChecked = true
                child.setBackgroundResource(R.drawable.bg_chip_selected)
                child.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }
        }
    }

    private fun getCheckedTexts(grid: GridLayout): String {
        val selected = mutableListOf<String>()
        for (i in 0 until grid.childCount) {
            val child = grid.getChildAt(i)
            if (child is CheckBox && child.isChecked) {
                selected.add(child.text.toString())
            }
        }
        return selected.joinToString(", ")
    }
}