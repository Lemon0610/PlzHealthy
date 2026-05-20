package com.example.plzhealth.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_table")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val birthDate: String,
    val gender: String,
    val allergies: String,
    val diseases: String,
    val memo: String,
    val isOwner: Boolean
)