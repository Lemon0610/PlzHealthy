package com.example.plzhealth

import android.os.Bundle
import android.view.View
import android.widget.*
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
    private lateinit var btnDelete: Button
    private lateinit var rbMale: RadioButton

    private val db by lazy { AppDatabase.getDatabase(requireContext()) }
    private var userId: Int = 0
    private var isOwnerMode: Boolean = false

    // 알레르기 CheckBox 목록 (XML id 순서와 일치)
    private val allergyIds = listOf(
        R.id.cbAllergyEgg, R.id.cbAllergyMilk, R.id.cbAllergyBuckwheat, R.id.cbAllergyPeanut,
        R.id.cbAllergySoy, R.id.cbAllergyWheat, R.id.cbAllergyMackerel, R.id.cbAllergyCrab,
        R.id.cbAllergyShrimp, R.id.cbAllergyPork, R.id.cbAllergyPeach, R.id.cbAllergyWalnut,
        R.id.cbAllergyChicken, R.id.cbAllergyBeef
    )

    // 질병 CheckBox 목록
    private val diseaseIds = listOf(
        R.id.cbHypertension, R.id.cbDiabetes, R.id.cbHyperlipidemia,
        R.id.cbFat, R.id.cbKidney, R.id.cbAllergy
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId      = arguments?.getInt("USER_ID", 0) ?: 0
        isOwnerMode = arguments?.getBoolean("IS_OWNER", false) ?: false

        etDetailName  = view.findViewById(R.id.etDetailName)
        etDetailBirth = view.findViewById(R.id.etDetailBirth)
        tvDisplayAge  = view.findViewById(R.id.tvDisplayAge)
        btnSave       = view.findViewById(R.id.btnSave)
        btnDelete     = view.findViewById(R.id.btnDelete)
        rbMale        = view.findViewById(R.id.rbMale)

        // 선택 시 칩 색상 토글
        setupChipToggle(view, allergyIds)
        setupChipToggle(view, diseaseIds)

        if (userId != 0 || isOwnerMode) {
            loadUserData(view)
            if (!isOwnerMode) btnDelete.visibility = View.VISIBLE
        }

        etDetailBirth.doAfterTextChanged { text ->
            if (text?.length == 8) {
                val raw = text.toString()
                val formatted = "${raw.substring(0,4)}-${raw.substring(4,6)}-${raw.substring(6,8)}"
                tvDisplayAge.text = "연산된 나이: ${DateUtils.calculateAge(formatted)} 세"
            }
        }

        btnSave.setOnClickListener { saveUserInfo(view) }
        btnDelete.setOnClickListener { deleteUserInfo() }
    }

    // 칩 선택/해제 시 배경색 토글
    private fun setupChipToggle(view: View, ids: List<Int>) {
        ids.forEach { id ->
            val cb = view.findViewById<CheckBox>(id) ?: return@forEach
            cb.setOnCheckedChangeListener { _, isChecked ->
                cb.setBackgroundResource(
                    if (isChecked) R.drawable.bg_chip_selected else R.drawable.bg_category_chip
                )
                cb.setTextColor(
                    if (isChecked) 0xFFFFFFFF.toInt() else 0xFF333333.toInt()
                )
            }
        }
    }

    private fun loadUserData(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = if (isOwnerMode) db.userDao().getMyInfo()
            else db.userDao().getUserById(userId)
            user?.let {
                userId = it.id
                etDetailName.setText(it.name)
                etDetailBirth.setText(it.birthDate)
                if (it.gender == "남성") rbMale.isChecked = true
                else view.findViewById<RadioButton>(R.id.rbFemale)?.isChecked = true
                setCheckedChips(view, allergyIds, it.allergies)
                setCheckedChips(view, diseaseIds, it.diseases)
            }
        }
    }

    private fun saveUserInfo(view: View) {
        val name  = etDetailName.text.toString()
        val birth = etDetailBirth.text.toString()

        if (name.isEmpty() || birth.length < 8) {
            Toast.makeText(requireContext(), "이름과 생년월일을 확인해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val user = UserEntity(
            id          = userId,
            name        = name,
            birthDate   = birth,
            gender      = if (rbMale.isChecked) "남성" else "여성",
            allergies   = getCheckedTexts(view, allergyIds),
            diseases    = getCheckedTexts(view, diseaseIds),
            memo        = "",
            isOwner     = isOwnerMode
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

    private fun setCheckedChips(view: View, ids: List<Int>, data: String) {
        val list = data.split(", ")
        ids.forEach { id ->
            val cb = view.findViewById<CheckBox>(id) ?: return@forEach
            if (list.contains(cb.text.toString())) {
                cb.isChecked = true
                cb.setBackgroundResource(R.drawable.bg_chip_selected)
                cb.setTextColor(0xFFFFFFFF.toInt())
            }
        }
    }

    private fun getCheckedTexts(view: View, ids: List<Int>): String {
        return ids.mapNotNull { id ->
            val cb = view.findViewById<CheckBox>(id)
            if (cb?.isChecked == true) cb.text.toString() else null
        }.joinToString(", ")
    }
}