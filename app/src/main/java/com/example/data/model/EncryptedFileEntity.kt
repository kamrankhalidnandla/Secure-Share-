package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encrypted_files")
data class EncryptedFileEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String,
    val encryptedContentHex: String, // Hex string of encrypted content or file payload
    val fileHashSha256: String,
    val rawAesKeyHex: String, // Encrypted/stored symmetric key in hex
    val aesIvHex: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expirationTimestamp: Long? = null,
    val maxDownloads: Int = -1, // -1 means unlimited
    val downloadCount: Int = 0,
    val isPasswordProtected: Boolean = false,
    val passwordSaltHex: String? = null,
    val passwordKeyWrapIvHex: String? = null,
    val passwordWrappedAesKeyHex: String? = null,
    val rsaWrappedAesKeyHex: String? = null, // Key wrapped via RSA recipient public key
    val recipientEmail: String? = null,
    val ownerEmail: String,
    val status: String = FileStatus.ACTIVE.name,
    val shareToken: String
)
