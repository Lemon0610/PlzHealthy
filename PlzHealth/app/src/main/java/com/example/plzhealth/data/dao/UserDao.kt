package com.example.plzhealth.data.dao

import androidx.room.*
import com.example.plzhealth.data.entity.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM user_table WHERE isOwner = :isOwner LIMIT 1")
    suspend fun getMyInfo(isOwner: Boolean = true): UserEntity?

    @Query("SELECT * FROM user_table WHERE isOwner = :isOwner")
    suspend fun getMembers(isOwner: Boolean = false): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM user_table WHERE id = :id")
    suspend fun getUserById(id: Int): UserEntity?

    @Delete
    suspend fun delete(user: UserEntity)
}