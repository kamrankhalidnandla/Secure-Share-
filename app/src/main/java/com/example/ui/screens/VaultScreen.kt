package com.example.ui.screens

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.data.model.EncryptedFileEntity
import com.example.data.model.ExpiryOption
import com.example.data.model.FileStatus
import com.example.ui.components.QrCodeView
import com.example.ui.components.SecurityBadge
import com.example.ui.components.StatCard
import com.example.ui.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val files by viewModel.allFiles.collectAsState()
    val activeDecryptedContent by viewModel.activeDecryptedContent.collectAsState()
    val activeDecryptedFileName by viewModel.activeDecryptedFileName.collectAsState()
    val userKeys by viewModel.allUserKeys.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    var showEncryptDialog by remember { mutableStateOf(false) }
    var fileForDetail by remember { mutableStateOf<EncryptedFileEntity?>(null) }
    var fileForShare by remember { mutableStateOf<EncryptedFileEntity?>(null) }
    var fileForPasswordPrompt by remember { mutableStateOf<EncryptedFileEntity?>(null) }

    val filteredFiles = remember(files, searchQuery, selectedFilter) {
        files.filter { file ->
            val matchesSearch = searchQuery.isEmpty() ||
                    file.fileName.contains(searchQuery, ignoreCase = true) ||
                    file.ownerEmail.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "ACTIVE" -> file.status == FileStatus.ACTIVE.name
                "EXPIRED" -> file.status == FileStatus.EXPIRED.name || file.status == FileStatus.REVOKED.name || file.status == FileStatus.SELF_DESTRUCTED.name
                "PASSWORD" -> file.isPasswordProtected
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    val activeCount = files.count { it.status == FileStatus.ACTIVE.name }
    val expiredCount = files.count { it.status != FileStatus.ACTIVE.name }
    val totalStorage = files.sumOf { it.fileSizeBytes }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showEncryptDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Encrypt File")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Encrypt File", fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Active Files",
                    value = activeCount.toString(),
                    subtitle = "E2EE Secured",
                    icon = Icons.Default.Lock,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Expired / Revoked",
                    value = expiredCount.toString(),
                    subtitle = "Self-Destructed",
                    icon = Icons.Default.LockClock,
                    accentColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Vault Size",
                    value = Formatter.formatShortFileSize(context, totalStorage),
                    subtitle = "AES-256 GCM",
                    icon = Icons.Default.Security,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search encrypted files by name or owner...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filters
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${files.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "ACTIVE",
                    onClick = { selectedFilter = "ACTIVE" },
                    label = { Text("Active ($activeCount)") }
                )
                FilterChip(
                    selected = selectedFilter == "EXPIRED",
                    onClick = { selectedFilter = "EXPIRED" },
                    label = { Text("Expired/Revoked ($expiredCount)") }
                )
                FilterChip(
                    selected = selectedFilter == "PASSWORD",
                    onClick = { selectedFilter = "PASSWORD" },
                    label = { Text("Password Protected") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // File List
            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No encrypted files found in vault",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Encrypt File' to add end-to-end encrypted items.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredFiles, key = { it.id }) { file ->
                        FileVaultCard(
                            file = file,
                            onDecryptClick = {
                                if (file.isPasswordProtected) {
                                    fileForPasswordPrompt = file
                                } else {
                                    viewModel.decryptFile(file)
                                }
                            },
                            onDetailClick = { fileForDetail = file },
                            onShareClick = { fileForShare = file },
                            onRevokeClick = { viewModel.revokeFile(file.id) },
                            onDeleteClick = { viewModel.deleteFile(file.id) }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    if (showEncryptDialog) {
        EncryptFileDialog(
            recipientKeys = userKeys.map { it.userEmail },
            onDismiss = { showEncryptDialog = false },
            onEncrypt = { fileName, content, mimeType, expiryMillis, maxDownloads, password, recipient ->
                viewModel.encryptAndUploadFile(
                    fileName = fileName,
                    contentString = content,
                    mimeType = mimeType,
                    expirationMillis = expiryMillis,
                    maxDownloads = maxDownloads,
                    passwordProtection = password,
                    recipientEmail = recipient
                )
                showEncryptDialog = false
            }
        )
    }

    fileForPasswordPrompt?.let { file ->
        PasswordPromptDialog(
            fileName = file.fileName,
            onDismiss = { fileForPasswordPrompt = null },
            onSubmit = { password ->
                viewModel.decryptFile(file, password)
                fileForPasswordPrompt = null
            }
        )
    }

    fileForDetail?.let { file ->
        FileDetailDialog(
            file = file,
            onDismiss = { fileForDetail = null }
        )
    }

    fileForShare?.let { file ->
        ShareLinkDialog(
            file = file,
            onDismiss = { fileForShare = null }
        )
    }

    activeDecryptedContent?.let { content ->
        DecryptedPreviewDialog(
            fileName = activeDecryptedFileName ?: "Decrypted Payload",
            content = content,
            onDismiss = { viewModel.clearDecryptedContent() }
        )
    }
}

@Composable
fun FileVaultCard(
    file: EncryptedFileEntity,
    onDecryptClick: () -> Unit,
    onDetailClick: () -> Unit,
    onShareClick: () -> Unit,
    onRevokeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (file.isPasswordProtected) Icons.Default.Password else Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = file.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${file.mimeType} • ${Formatter.formatShortFileSize(LocalContext.current, file.fileSizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                SecurityBadge(status = file.status)

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Cryptographic Metadata") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDetailClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Generate Secure Share Link") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onShareClick()
                            }
                        )
                        if (file.status == FileStatus.ACTIVE.name) {
                            DropdownMenuItem(
                                text = { Text("Revoke Access Link") },
                                leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onRevokeClick()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete from Vault") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Owner: ${file.ownerEmail}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "SHA-256: ${file.fileHashSha256.take(12)}...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Downloads: ${file.downloadCount}" + if (file.maxDownloads > 0) " / ${file.maxDownloads}" else "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (file.expirationTimestamp != null) {
                        val expires = dateFormat.format(Date(file.expirationTimestamp))
                        Text(
                            text = "Expires: $expires",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Decrypt / Download Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDetailClick,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Details")
                }

                Button(
                    onClick = onDecryptClick,
                    enabled = file.status == FileStatus.ACTIVE.name,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (file.isPasswordProtected) Icons.Default.Key else Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (file.isPasswordProtected) "Unlock & Decrypt" else "Decrypt & View")
                }
            }
        }
    }
}

@Composable
fun EncryptFileDialog(
    recipientKeys: List<String>,
    onDismiss: () -> Unit,
    onEncrypt: (fileName: String, content: String, mimeType: String, expiryMillis: Long?, maxDownloads: Int, password: String?, recipient: String?) -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    var selectedMime by remember { mutableStateOf("text/plain") }
    var selectedExpiry by remember { mutableStateOf(ExpiryOption.HOURS_24) }
    var maxDownloadsText by remember { mutableStateOf("5") }
    var isPasswordEnabled by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var selectedRecipient by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Encrypt New File (AES-256 GCM)") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name") },
                    placeholder = { Text("e.g., Confidential_Contract.pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    label = { Text("File Content Payload") },
                    placeholder = { Text("Enter text, JSON, key data, or confidential content...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )

                Text(
                    text = "Expiration Time-To-Live (TTL)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ExpiryOption.values().take(4).forEach { option ->
                        FilterChip(
                            selected = selectedExpiry == option,
                            onClick = { selectedExpiry = option },
                            label = { Text(option.label, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = maxDownloadsText,
                    onValueChange = { maxDownloadsText = it.filter { char -> char.isDigit() } },
                    label = { Text("Max Downloads Limit (-1 for unlimited)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Password, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Password Protection", fontWeight = FontWeight.SemiBold)
                    }
                    Switch(
                        checked = isPasswordEnabled,
                        onCheckedChange = { isPasswordEnabled = it }
                    )
                }

                if (isPasswordEnabled) {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Decryption Password (PBKDF2)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = selectedRecipient,
                    onValueChange = { selectedRecipient = it },
                    label = { Text("Recipient Email (RSA Key Exchange - Optional)") },
                    placeholder = { Text("e.g. bob@enterprise.secureshare") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileName.isNotBlank() && contentText.isNotBlank()) {
                        val maxDownloads = maxDownloadsText.toIntOrNull() ?: -1
                        val password = if (isPasswordEnabled && passwordInput.isNotBlank()) passwordInput else null
                        val recipient = if (selectedRecipient.isNotBlank()) selectedRecipient else null
                        onEncrypt(
                            fileName,
                            contentText,
                            selectedMime,
                            selectedExpiry.durationMillis,
                            maxDownloads,
                            password,
                            recipient
                        )
                    }
                },
                enabled = fileName.isNotBlank() && contentText.isNotBlank()
            ) {
                Text("Encrypt & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PasswordPromptDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onSubmit: (password: String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password Protected File") },
        text = {
            Column {
                Text("'$fileName' is protected with a password wrapper key. Please enter the password to derive the AES key and decrypt the file payload.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(password) },
                enabled = password.isNotBlank()
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FileDetailDialog(
    file: EncryptedFileEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cryptographic Audit Details") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailItem("File Name", file.fileName)
                DetailItem("File ID", file.id)
                DetailItem("Size", "${file.fileSizeBytes} bytes")
                DetailItem("MIME Type", file.mimeType)
                DetailItem("Algorithm", "AES-256 GCM / NoPadding")
                DetailItem("SHA-256 Hash", file.fileHashSha256)
                DetailItem("AES IV (Hex)", file.aesIvHex)
                DetailItem("Owner", file.ownerEmail)
                DetailItem("Password Protected", if (file.isPasswordProtected) "YES (PBKDF2 SHA-256)" else "NO")
                if (file.isPasswordProtected) {
                    DetailItem("Password Salt", file.passwordSaltHex ?: "N/A")
                }
                if (!file.recipientEmail.isNullOrBlank()) {
                    DetailItem("RSA Key Recipient", file.recipientEmail)
                    DetailItem("Wrapped AES Key", file.rsaWrappedAesKeyHex ?: "N/A")
                }
                DetailItem("Status", file.status)
                DetailItem("Share Link Token", file.shareToken)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ShareLinkDialog(
    file: EncryptedFileEntity,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val link = CryptoEngine.generateShareLink(file.id, file.shareToken)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Secure Share Link & Token") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Scan or share this secure link token. Access is governed by expiration and download limit controls.")

                QrCodeView(data = link)

                OutlinedTextField(
                    value = link,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Encrypted Share Link") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(link))
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Link")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DecryptedPreviewDialog(
    fileName: String,
    content: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Decrypted Content: $fileName") },
        text = {
            Column {
                Text(
                    text = "End-to-End Encryption verified via SHA-256 checksum.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = content,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    clipboardManager.setText(AnnotatedString(content))
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy")
                }
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
