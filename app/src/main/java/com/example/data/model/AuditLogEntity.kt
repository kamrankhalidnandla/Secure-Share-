package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val fileId: String? = null,
    val fileName: String? = null,
    val actionType: String,
    val severity: String = SeverityLevel.INFO.name,
    val actorEmail: String,
    val actorRole: String,
    val details: String,
    val ipOrDevice: String = "Android Device (192.168.1.104)"
)
