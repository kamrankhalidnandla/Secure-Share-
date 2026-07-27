package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_keys")
data class UserKeyEntity(
    @PrimaryKey val userEmail: String,
    val publicKeyPem: String,
    val privateKeyPemEncrypted: String,
    val keyAlgorithm: String = "RSA-2048",
    val fingerprintSha256: String,
    val createdTimestamp: Long = System.currentTimeMillis()
)
