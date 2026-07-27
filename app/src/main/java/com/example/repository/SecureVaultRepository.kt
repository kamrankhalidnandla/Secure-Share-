package com.example.repository

import com.example.crypto.CryptoEngine
import com.example.data.db.AppDatabase
import com.example.data.model.ActionType
import com.example.data.model.AuditLogEntity
import com.example.data.model.EncryptedFileEntity
import com.example.data.model.FileStatus
import com.example.data.model.SeverityLevel
import com.example.data.model.UserKeyEntity
import com.example.data.model.UserPermissionEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class SecureVaultRepository(private val db: AppDatabase) {

    val allFiles: Flow<List<EncryptedFileEntity>> = db.fileVaultDao().getAllFiles()
    val allAuditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().getAllAuditLogs()
    val allUserKeys: Flow<List<UserKeyEntity>> = db.userKeyDao().getAllUserKeys()
    val allPermissions: Flow<List<UserPermissionEntity>> = db.userPermissionDao().getAllPermissions()

    fun getPermissionsForFile(fileId: String): Flow<List<UserPermissionEntity>> =
        db.userPermissionDao().getPermissionsForFile(fileId)

    suspend fun getFileById(fileId: String): EncryptedFileEntity? =
        db.fileVaultDao().getFileById(fileId)

    suspend fun getFileByShareToken(shareToken: String): EncryptedFileEntity? =
        db.fileVaultDao().getFileByShareToken(shareToken)

    suspend fun logAudit(
        fileId: String? = null,
        fileName: String? = null,
        actionType: ActionType,
        severity: SeverityLevel = SeverityLevel.INFO,
        actorEmail: String,
        actorRole: String,
        details: String
    ) {
        val audit = AuditLogEntity(
            fileId = fileId,
            fileName = fileName,
            actionType = actionType.name,
            severity = severity.name,
            actorEmail = actorEmail,
            actorRole = actorRole,
            details = details
        )
        db.auditLogDao().insertAuditLog(audit)
    }

    suspend fun encryptAndSaveFile(
        fileName: String,
        rawContentBytes: ByteArray,
        mimeType: String,
        ownerEmail: String,
        ownerRole: String,
        expirationDurationMillis: Long?,
        maxDownloads: Int,
        passwordProtection: String?, // optional password
        recipientEmail: String? // optional recipient for RSA key exchange
    ): EncryptedFileEntity = withContext(Dispatchers.IO) {
        val fileId = UUID.randomUUID().toString()
        val fileHash = CryptoEngine.sha256(rawContentBytes)
        val rawAesKeyBytes = CryptoEngine.generateAesKey()

        val gcmResult = CryptoEngine.encryptAesGcm(rawContentBytes, rawAesKeyBytes)

        val expirationTimestamp = expirationDurationMillis?.let { System.currentTimeMillis() + it }

        var isPasswordProtected = false
        var passwordSaltHex: String? = null
        var passwordKeyWrapIvHex: String? = null
        var passwordWrappedAesKeyHex: String? = null

        if (!passwordProtection.isNullOrBlank()) {
            isPasswordProtected = true
            val wrapResult = CryptoEngine.wrapAesKeyWithPassword(rawAesKeyBytes, passwordProtection)
            passwordSaltHex = wrapResult.saltHex
            passwordKeyWrapIvHex = wrapResult.ivHex
            passwordWrappedAesKeyHex = wrapResult.wrappedKeyHex
        }

        var rsaWrappedAesKeyHex: String? = null
        if (!recipientEmail.isNullOrBlank()) {
            val recipientKey = db.userKeyDao().getKeyForUser(recipientEmail)
            if (recipientKey != null) {
                rsaWrappedAesKeyHex = CryptoEngine.wrapAesKeyWithRsaPublic(rawAesKeyBytes, recipientKey.publicKeyPem)
            }
        }

        val shareToken = UUID.randomUUID().toString().replace("-", "").take(16)

        val encryptedFile = EncryptedFileEntity(
            id = fileId,
            fileName = fileName,
            fileSizeBytes = rawContentBytes.size.toLong(),
            mimeType = mimeType,
            encryptedContentHex = gcmResult.ciphertextHex,
            fileHashSha256 = fileHash,
            rawAesKeyHex = CryptoEngine.bytesToHex(rawAesKeyBytes),
            aesIvHex = gcmResult.ivHex,
            createdAt = System.currentTimeMillis(),
            expirationTimestamp = expirationTimestamp,
            maxDownloads = maxDownloads,
            downloadCount = 0,
            isPasswordProtected = isPasswordProtected,
            passwordSaltHex = passwordSaltHex,
            passwordKeyWrapIvHex = passwordKeyWrapIvHex,
            passwordWrappedAesKeyHex = passwordWrappedAesKeyHex,
            rsaWrappedAesKeyHex = rsaWrappedAesKeyHex,
            recipientEmail = recipientEmail,
            ownerEmail = ownerEmail,
            status = FileStatus.ACTIVE.name,
            shareToken = shareToken
        )

        db.fileVaultDao().insertFile(encryptedFile)

        // Grant owner permissions
        db.userPermissionDao().insertOrUpdatePermission(
            UserPermissionEntity(
                fileId = fileId,
                userEmail = ownerEmail,
                userRole = UserRole.OWNER.name,
                canRead = true,
                canDownload = true,
                canShare = true,
                canRevoke = true,
                canDelete = true
            )
        )

        // Grant recipient permissions if set
        if (!recipientEmail.isNullOrBlank()) {
            db.userPermissionDao().insertOrUpdatePermission(
                UserPermissionEntity(
                    fileId = fileId,
                    userEmail = recipientEmail,
                    userRole = UserRole.VIEWER.name,
                    canRead = true,
                    canDownload = true,
                    canShare = false,
                    canRevoke = false,
                    canDelete = false
                )
            )
        }

        logAudit(
            fileId = fileId,
            fileName = fileName,
            actionType = ActionType.FILE_ENCRYPTED,
            severity = SeverityLevel.INFO,
            actorEmail = ownerEmail,
            actorRole = ownerRole,
            details = "Encrypted $fileName (${rawContentBytes.size} bytes) with AES-256 GCM. " +
                    if (isPasswordProtected) "Password protection enabled. " else "" +
                    if (expirationTimestamp != null) "Expiration set. " else "" +
                    if (recipientEmail != null) "RSA key wrapped for $recipientEmail." else ""
        )

        encryptedFile
    }

    suspend fun decryptFileContent(
        file: EncryptedFileEntity,
        passwordInput: String? = null,
        actorEmail: String = "admin@enterprise.secureshare",
        actorRole: String = "Administrator"
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            // Verify if active
            if (file.status == FileStatus.REVOKED.name || file.status == FileStatus.SELF_DESTRUCTED.name) {
                logAudit(
                    fileId = file.id,
                    fileName = file.fileName,
                    actionType = ActionType.FAILED_PASSWORD_ATTEMPT,
                    severity = SeverityLevel.SECURITY_ALERT,
                    actorEmail = actorEmail,
                    actorRole = actorRole,
                    details = "Attempted to decrypt non-active file (Status: ${file.status})"
                )
                return@withContext Result.failure(IllegalStateException("File is revoked or self-destructed."))
            }

            // Check expiration time
            if (file.expirationTimestamp != null && System.currentTimeMillis() > file.expirationTimestamp) {
                db.fileVaultDao().updateFileStatus(file.id, FileStatus.EXPIRED.name)
                logAudit(
                    fileId = file.id,
                    fileName = file.fileName,
                    actionType = ActionType.AUTO_EXPIRED,
                    severity = SeverityLevel.WARNING,
                    actorEmail = "SYSTEM",
                    actorRole = "SYSTEM",
                    details = "File expired due to time-to-live expiration timer."
                )
                return@withContext Result.failure(IllegalStateException("File has expired and can no longer be downloaded."))
            }

            // Check max download count
            if (file.maxDownloads > 0 && file.downloadCount >= file.maxDownloads) {
                db.fileVaultDao().updateFileStatus(file.id, FileStatus.SELF_DESTRUCTED.name)
                logAudit(
                    fileId = file.id,
                    fileName = file.fileName,
                    actionType = ActionType.DESTRUCT_TRIGGERED,
                    severity = SeverityLevel.SECURITY_ALERT,
                    actorEmail = "SYSTEM",
                    actorRole = "SYSTEM",
                    details = "Maximum download limit of ${file.maxDownloads} reached. Self-destruct triggered."
                )
                return@withContext Result.failure(IllegalStateException("Maximum download limit reached ($file.maxDownloads). File self-destructed."))
            }

            // Resolve raw AES key bytes
            val aesKeyBytes: ByteArray = if (file.isPasswordProtected) {
                if (passwordInput.isNullOrBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Password required to decrypt this file."))
                }
                try {
                    CryptoEngine.unwrapAesKeyWithPassword(
                        wrappedKeyHex = file.passwordWrappedAesKeyHex!!,
                        password = passwordInput,
                        saltHex = file.passwordSaltHex!!,
                        ivHex = file.passwordKeyWrapIvHex!!
                    )
                } catch (e: Exception) {
                    logAudit(
                        fileId = file.id,
                        fileName = file.fileName,
                        actionType = ActionType.FAILED_PASSWORD_ATTEMPT,
                        severity = SeverityLevel.SECURITY_ALERT,
                        actorEmail = actorEmail,
                        actorRole = actorRole,
                        details = "Incorrect password provided for encrypted file."
                    )
                    return@withContext Result.failure(IllegalArgumentException("Incorrect decryption password."))
                }
            } else if (!file.rsaWrappedAesKeyHex.isNullOrBlank() && file.recipientEmail == actorEmail) {
                val recipientKey = db.userKeyDao().getKeyForUser(actorEmail)
                    ?: return@withContext Result.failure(IllegalStateException("No RSA private key found for user $actorEmail"))
                CryptoEngine.unwrapAesKeyWithRsaPrivate(file.rsaWrappedAesKeyHex, recipientKey.privateKeyPemEncrypted)
            } else {
                CryptoEngine.hexToBytes(file.rawAesKeyHex)
            }

            // Decrypt AES-GCM
            val plainBytes = CryptoEngine.decryptAesGcm(
                ciphertextHex = file.encryptedContentHex,
                keyBytes = aesKeyBytes,
                ivHex = file.aesIvHex
            )

            // Verify SHA-256 integrity
            val actualHash = CryptoEngine.sha256(plainBytes)
            if (actualHash != file.fileHashSha256) {
                logAudit(
                    fileId = file.id,
                    fileName = file.fileName,
                    actionType = ActionType.FAILED_PASSWORD_ATTEMPT,
                    severity = SeverityLevel.SECURITY_ALERT,
                    actorEmail = actorEmail,
                    actorRole = actorRole,
                    details = "Integrity check failed: Expected SHA-256 ${file.fileHashSha256}, got $actualHash"
                )
                return@withContext Result.failure(SecurityException("Data integrity checksum failure. File may have been tampered with."))
            }

            // Increment download count
            val newDownloadCount = file.downloadCount + 1
            db.fileVaultDao().incrementDownloadCount(file.id)

            // Check if self-destruct condition reached after this download
            if (file.maxDownloads > 0 && newDownloadCount >= file.maxDownloads) {
                db.fileVaultDao().updateFileStatus(file.id, FileStatus.SELF_DESTRUCTED.name)
            }

            logAudit(
                fileId = file.id,
                fileName = file.fileName,
                actionType = ActionType.FILE_DECRYPTED,
                severity = SeverityLevel.INFO,
                actorEmail = actorEmail,
                actorRole = actorRole,
                details = "Successfully decrypted file $file.fileName (${plainBytes.size} bytes). Download #$newDownloadCount."
            )

            if (file.isPasswordProtected) {
                logAudit(
                    fileId = file.id,
                    fileName = file.fileName,
                    actionType = ActionType.PASSWORD_VERIFIED,
                    severity = SeverityLevel.INFO,
                    actorEmail = actorEmail,
                    actorRole = actorRole,
                    details = "Password verification successful."
                )
            }

            Result.success(plainBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeFile(
        fileId: String,
        actorEmail: String,
        actorRole: String
    ) = withContext(Dispatchers.IO) {
        val file = db.fileVaultDao().getFileById(fileId)
        if (file != null) {
            db.fileVaultDao().updateFileStatus(fileId, FileStatus.REVOKED.name)
            logAudit(
                fileId = fileId,
                fileName = file.fileName,
                actionType = ActionType.FILE_REVOKED,
                severity = SeverityLevel.WARNING,
                actorEmail = actorEmail,
                actorRole = actorRole,
                details = "Encrypted access link and file download revoked by $actorEmail."
            )
        }
    }

    suspend fun deleteFile(
        fileId: String,
        actorEmail: String,
        actorRole: String
    ) = withContext(Dispatchers.IO) {
        val file = db.fileVaultDao().getFileById(fileId)
        if (file != null) {
            db.fileVaultDao().deleteFileById(fileId)
            logAudit(
                fileId = fileId,
                fileName = file.fileName,
                actionType = ActionType.FILE_REVOKED,
                severity = SeverityLevel.WARNING,
                actorEmail = actorEmail,
                actorRole = actorRole,
                details = "File permanently purged from encrypted vault storage."
            )
        }
    }

    suspend fun generateUserRsaKey(email: String, actorRole: String): UserKeyEntity = withContext(Dispatchers.IO) {
        val pem = CryptoEngine.generateRsaKeyPair()
        val userKey = UserKeyEntity(
            userEmail = email,
            publicKeyPem = pem.publicKeyPem,
            privateKeyPemEncrypted = pem.privateKeyPem,
            keyAlgorithm = "RSA-2048",
            fingerprintSha256 = pem.fingerprintSha256,
            createdTimestamp = System.currentTimeMillis()
        )
        db.userKeyDao().insertUserKey(userKey)
        logAudit(
            actionType = ActionType.RSA_KEY_GENERATED,
            severity = SeverityLevel.INFO,
            actorEmail = email,
            actorRole = actorRole,
            details = "Generated 2048-bit RSA key pair. Fingerprint SHA-256: ${pem.fingerprintSha256}"
        )
        userKey
    }

    suspend fun updatePermission(
        permission: UserPermissionEntity,
        actorEmail: String,
        actorRole: String
    ) = withContext(Dispatchers.IO) {
        db.userPermissionDao().insertOrUpdatePermission(permission)
        logAudit(
            fileId = permission.fileId,
            actionType = ActionType.PERMISSION_CHANGED,
            severity = SeverityLevel.INFO,
            actorEmail = actorEmail,
            actorRole = actorRole,
            details = "Updated permissions for ${permission.userEmail}: Role=${permission.userRole}, CanRead=${permission.canRead}, CanDownload=${permission.canDownload}, CanShare=${permission.canShare}"
        )
    }

    suspend fun deletePermission(permissionId: Long, actorEmail: String, actorRole: String) = withContext(Dispatchers.IO) {
        db.userPermissionDao().deletePermission(permissionId)
        logAudit(
            actionType = ActionType.PERMISSION_CHANGED,
            severity = SeverityLevel.INFO,
            actorEmail = actorEmail,
            actorRole = actorRole,
            details = "Revoked permission entry #$permissionId."
        )
    }

    suspend fun seedInitialEnterpriseDataIfNeeded() = withContext(Dispatchers.IO) {
        val existingFiles = db.fileVaultDao().getAllFiles().first()
        if (existingFiles.isNotEmpty()) return@withContext

        val adminEmail = "admin@enterprise.secureshare"
        val aliceEmail = "alice@enterprise.secureshare"
        val bobEmail = "bob@enterprise.secureshare"

        // Generate keys for initial users
        generateUserRsaKey(adminEmail, "Administrator")
        generateUserRsaKey(aliceEmail, "Security Manager")
        generateUserRsaKey(bobEmail, "Viewer")

        // Seed File 1: Financial Audit Report (Password protected, 24h expiration)
        val doc1Content = """
            CONFIDENTIAL FINANCIAL AUDIT REPORT Q3
            ------------------------------------------------
            Gross Revenue: $14,250,000 USD
            Operating Margin: 34.2%
            Enterprise Cryptographic Audit Status: PASSED
            AES-256 Encryption Compliance: Verified 100%
            Security Standard: ISO/IEC 27001 & FIPS 140-3
        """.trimIndent().toByteArray(Charsets.UTF_8)

        encryptAndSaveFile(
            fileName = "Q3_Financial_Audit_Report.pdf",
            rawContentBytes = doc1Content,
            mimeType = "application/pdf",
            ownerEmail = adminEmail,
            ownerRole = "Administrator",
            expirationDurationMillis = 24 * 60 * 60 * 1000L,
            maxDownloads = 5,
            passwordProtection = "Vault2026!",
            recipientEmail = aliceEmail
        )

        // Seed File 2: Executive Acquisition Agreement (RSA Key Exchange Encrypted for Bob)
        val doc2Content = """
            EXECUTIVE ACQUISITION AGREEMENT (STRICTLY CLASSIFIED)
            -----------------------------------------------------
            Target Entity: CyberCore Dynamics Inc.
            Agreed Valuation: $85,000,000 USD
            Signatories: Chief Information Security Officer & Managing Board
            Encryption: Wrapped AES-256 Key via Bob's RSA-2048 Public Key
        """.trimIndent().toByteArray(Charsets.UTF_8)

        encryptAndSaveFile(
            fileName = "Acquisition_M_and_A_Agreement.docx",
            rawContentBytes = doc2Content,
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ownerEmail = adminEmail,
            ownerRole = "Administrator",
            expirationDurationMillis = 7 * 24 * 60 * 60 * 1000L,
            maxDownloads = 3,
            passwordProtection = null,
            recipientEmail = bobEmail
        )

        // Seed File 3: Customer Database Export (Expired/Self-Destructed Example)
        val doc3Content = """
            EXPIRED DATA DUMP (HISTORICAL RECOVERY TEST)
            Customer Accounts: 50,000
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val expiredFile = encryptAndSaveFile(
            fileName = "Historical_Customer_Export.csv",
            rawContentBytes = doc3Content,
            mimeType = "text/csv",
            ownerEmail = aliceEmail,
            ownerRole = "Security Manager",
            expirationDurationMillis = 1000L, // Expired immediately
            maxDownloads = 1,
            passwordProtection = null,
            recipientEmail = null
        )

        // Mark doc3 expired
        db.fileVaultDao().updateFileStatus(expiredFile.id, FileStatus.EXPIRED.name)
        logAudit(
            fileId = expiredFile.id,
            fileName = expiredFile.fileName,
            actionType = ActionType.AUTO_EXPIRED,
            severity = SeverityLevel.WARNING,
            actorEmail = "SYSTEM",
            actorRole = "SYSTEM",
            details = "Historical_Customer_Export.csv expired automatically via TTL timer."
        )
    }
}
