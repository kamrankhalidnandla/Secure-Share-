package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserPermissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPermissionDao {
    @Query("SELECT * FROM user_permissions WHERE fileId = :fileId")
    fun getPermissionsForFile(fileId: String): Flow<List<UserPermissionEntity>>

    @Query("SELECT * FROM user_permissions WHERE fileId = :fileId AND userEmail = :userEmail")
    suspend fun getPermissionForUser(fileId: String, userEmail: String): UserPermissionEntity?

    @Query("SELECT * FROM user_permissions")
    fun getAllPermissions(): Flow<List<UserPermissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePermission(permission: UserPermissionEntity)

    @Query("DELETE FROM user_permissions WHERE id = :id")
    suspend fun deletePermission(id: Long)

    @Query("DELETE FROM user_permissions WHERE fileId = :fileId AND userEmail = :userEmail")
    suspend fun deletePermissionForUser(fileId: String, userEmail: String)
}
