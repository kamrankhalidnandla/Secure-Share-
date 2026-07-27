package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.ActionType
import com.example.data.model.AuditLogEntity
import com.example.data.model.EncryptedFileEntity
import com.example.data.model.SeverityLevel
import com.example.data.model.UserKeyEntity
import com.example.data.model.UserPermissionEntity
import com.example.data.model.UserRole
import com.example.repository.SecureVaultRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = SecureVaultRepository(db)

    // Current Session User
    val currentUserEmail = MutableStateFlow("admin@enterprise.secureshare")
    val currentUserRole = MutableStateFlow(UserRole.ADMIN.displayName)

    // UI Message Events
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Data Streams from Repository
    val allFiles: StateFlow<List<EncryptedFileEntity>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUserKeys: StateFlow<List<UserKeyEntity>> = repository.allUserKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPermissions: StateFlow<List<UserPermissionEntity>> = repository.allPermissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audit Search & Filter
    val auditSearchQuery = MutableStateFlow("")
    val auditSeverityFilter = MutableStateFlow<SeverityLevel?>(null)

    val filteredAuditLogs: StateFlow<List<AuditLogEntity>> = combine(
        allAuditLogs,
        auditSearchQuery,
        auditSeverityFilter
    ) { logs, query, severity ->
        logs.filter { log ->
            val matchesQuery = query.isEmpty() ||
                    log.fileName?.contains(query, ignoreCase = true) == true ||
                    log.actorEmail.contains(query, ignoreCase = true) ||
                    log.actionType.contains(query, ignoreCase = true) ||
                    log.details.contains(query, ignoreCase = true)

            val matchesSeverity = severity == null || log.severity == severity.name
            matchesQuery && matchesSeverity
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Decryption preview modal state
    val activeDecryptedContent = MutableStateFlow<String?>(null)
    val activeDecryptedFileName = MutableStateFlow<String?>(null)
    val isDecrypting = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repository.seedInitialEnterpriseDataIfNeeded()
        }
    }

    fun switchUserSession(email: String, role: String) {
        currentUserEmail.value = email
        currentUserRole.value = role
        viewModelScope.launch {
            _userMessage.emit("Switched session to $email ($role)")
        }
    }

    fun encryptAndUploadFile(
        fileName: String,
        contentString: String,
        mimeType: String,
        expirationMillis: Long?,
        maxDownloads: Int,
        passwordProtection: String?,
        recipientEmail: String?
    ) {
        viewModelScope.launch {
            try {
                val bytes = contentString.toByteArray(Charsets.UTF_8)
                val file = repository.encryptAndSaveFile(
                    fileName = fileName,
                    rawContentBytes = bytes,
                    mimeType = mimeType,
                    ownerEmail = currentUserEmail.value,
                    ownerRole = currentUserRole.value,
                    expirationDurationMillis = expirationMillis,
                    maxDownloads = maxDownloads,
                    passwordProtection = passwordProtection,
                    recipientEmail = recipientEmail
                )
                _userMessage.emit("Encrypted and secured '${file.fileName}' with AES-256 GCM!")
            } catch (e: Exception) {
                _userMessage.emit("Encryption failed: ${e.localizedMessage}")
            }
        }
    }

    fun decryptFile(file: EncryptedFileEntity, passwordInput: String? = null) {
        viewModelScope.launch {
            isDecrypting.value = true
            val result = repository.decryptFileContent(
                file = file,
                passwordInput = passwordInput,
                actorEmail = currentUserEmail.value,
                actorRole = currentUserRole.value
            )
            isDecrypting.value = false

            result.fold(
                onSuccess = { bytes ->
                    activeDecryptedFileName.value = file.fileName
                    activeDecryptedContent.value = String(bytes, Charsets.UTF_8)
                    _userMessage.emit("Successfully decrypted '${file.fileName}'!")
                },
                onFailure = { error ->
                    _userMessage.emit("Decryption error: ${error.localizedMessage}")
                }
            )
        }
    }

    fun clearDecryptedContent() {
        activeDecryptedContent.value = null
        activeDecryptedFileName.value = null
    }

    fun revokeFile(fileId: String) {
        viewModelScope.launch {
            repository.revokeFile(
                fileId = fileId,
                actorEmail = currentUserEmail.value,
                actorRole = currentUserRole.value
            )
            _userMessage.emit("Access revoked for file ID $fileId")
        }
    }

    fun deleteFile(fileId: String) {
        viewModelScope.launch {
            repository.deleteFile(
                fileId = fileId,
                actorEmail = currentUserEmail.value,
                actorRole = currentUserRole.value
            )
            _userMessage.emit("File permanently deleted from vault")
        }
    }

    fun generateNewKeyForUser(email: String) {
        viewModelScope.launch {
            try {
                val key = repository.generateUserRsaKey(email, currentUserRole.value)
                _userMessage.emit("Generated RSA-2048 KeyPair for $email")
            } catch (e: Exception) {
                _userMessage.emit("Key generation error: ${e.localizedMessage}")
            }
        }
    }

    fun updatePermission(permission: UserPermissionEntity) {
        viewModelScope.launch {
            repository.updatePermission(
                permission = permission,
                actorEmail = currentUserEmail.value,
                actorRole = currentUserRole.value
            )
            _userMessage.emit("Permissions updated for ${permission.userEmail}")
        }
    }

    fun deletePermission(id: Long) {
        viewModelScope.launch {
            repository.deletePermission(
                permissionId = id,
                actorEmail = currentUserEmail.value,
                actorRole = currentUserRole.value
            )
            _userMessage.emit("Permission entry deleted.")
        }
    }

    fun consumeShareToken(tokenInput: String, passwordInput: String? = null) {
        viewModelScope.launch {
            val cleanToken = tokenInput.trim().substringAfter("token=").trim()
            val file = repository.getFileByShareToken(cleanToken)
            if (file == null) {
                _userMessage.emit("Invalid or non-existent share link token.")
                repository.logAudit(
                    actionType = ActionType.FAILED_PASSWORD_ATTEMPT,
                    severity = SeverityLevel.WARNING,
                    actorEmail = currentUserEmail.value,
                    actorRole = currentUserRole.value,
                    details = "Invalid token consumption attempt: '$cleanToken'"
                )
                return@launch
            }

            decryptFile(file, passwordInput)
        }
    }
}
