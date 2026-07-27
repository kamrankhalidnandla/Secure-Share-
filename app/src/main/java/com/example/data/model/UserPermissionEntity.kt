package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_permissions")
data class UserPermissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileId: String,
    val userEmail: String,
    val userRole: String = UserRole.VIEWER.name,
    val canRead: Boolean = true,
    val canDownload: Boolean = true,
    val canShare: Boolean = false,
    val canRevoke: Boolean = false,
    val canDelete: Boolean = false,
    val grantedAt: Long = System.currentTimeMillis()
)
