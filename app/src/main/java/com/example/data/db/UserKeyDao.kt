package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UserKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserKeyDao {
    @Query("SELECT * FROM user_keys")
    fun getAllUserKeys(): Flow<List<UserKeyEntity>>

    @Query("SELECT * FROM user_keys WHERE userEmail = :email")
    suspend fun getKeyForUser(email: String): UserKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserKey(userKey: UserKeyEntity)

    @Query("DELETE FROM user_keys WHERE userEmail = :email")
    suspend fun deleteKeyForUser(email: String)
}
