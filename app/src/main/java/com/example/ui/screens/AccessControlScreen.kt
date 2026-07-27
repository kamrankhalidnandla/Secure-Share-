package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserKeyEntity
import com.example.data.model.UserPermissionEntity
import com.example.data.model.UserRole
import com.example.ui.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessControlScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val userKeys by viewModel.allUserKeys.collectAsState()
    val permissions by viewModel.allPermissions.collectAsState()
    val files by viewModel.allFiles.collectAsState()

    var showGenerateKeyDialog by remember { mutableStateOf(false) }
    var showAddPermissionDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("RSA Key Vault (${userKeys.size})") },
                icon = { Icon(Icons.Default.Key, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Granular ACL (${permissions.size})") },
                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            // RSA Key Vault View
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enterprise RSA-2048 Key Pairs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(onClick = { showGenerateKeyDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate KeyPair")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(userKeys, key = { it.userEmail }) { userKey ->
                    UserKeyCard(
                        userKey = userKey,
                        onCopyPublicKey = {
                            clipboardManager.setText(AnnotatedString(userKey.publicKeyPem))
                        }
                    )
                }
            }
        } else {
            // Granular ACL View
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Granular Access Control List (ACL)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(onClick = { showAddPermissionDialog = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Grant ACL Access")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(permissions, key = { it.id }) { permission ->
                    val file = files.find { it.id == permission.fileId }
                    AclCard(
                        permission = permission,
                        fileName = file?.fileName ?: permission.fileId,
                        onToggleRead = {
                            viewModel.updatePermission(permission.copy(canRead = !permission.canRead))
                        },
                        onToggleDownload = {
                            viewModel.updatePermission(permission.copy(canDownload = !permission.canDownload))
                        },
                        onToggleShare = {
                            viewModel.updatePermission(permission.copy(canShare = !permission.canShare))
                        },
                        onToggleRevoke = {
                            viewModel.updatePermission(permission.copy(canRevoke = !permission.canRevoke))
                        },
                        onDelete = {
                            viewModel.deletePermission(permission.id)
                        }
                    )
                }
            }
        }
    }

    if (showGenerateKeyDialog) {
        var emailInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showGenerateKeyDialog = false },
            title = { Text("Generate RSA Key Pair") },
            text = {
                Column {
                    Text("Create a new 2048-bit RSA Key Pair for secure key exchange.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("User Email") },
                        placeholder = { Text("e.g. charlie@enterprise.secureshare") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailInput.isNotBlank()) {
                            viewModel.generateNewKeyForUser(emailInput.trim())
                            showGenerateKeyDialog = false
                        }
                    },
                    enabled = emailInput.isNotBlank()
                ) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddPermissionDialog) {
        var targetUserEmail by remember { mutableStateOf("") }
        var selectedFileId by remember { mutableStateOf(files.firstOrNull()?.id ?: "") }
        var selectedRole by remember { mutableStateOf(UserRole.VIEWER) }

        AlertDialog(
            onDismissRequest = { showAddPermissionDialog = false },
            title = { Text("Grant Granular Access Permission") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = targetUserEmail,
                        onValueChange = { targetUserEmail = it },
                        label = { Text("Target User Email") },
                        placeholder = { Text("e.g. user@enterprise.secureshare") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Target File", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    files.forEach { file ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = selectedFileId == file.id,
                                onCheckedChange = { if (it) selectedFileId = file.id }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = file.fileName, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetUserEmail.isNotBlank() && selectedFileId.isNotBlank()) {
                            val newPermission = UserPermissionEntity(
                                fileId = selectedFileId,
                                userEmail = targetUserEmail.trim(),
                                userRole = selectedRole.name,
                                canRead = true,
                                canDownload = true,
                                canShare = false,
                                canRevoke = false,
                                canDelete = false
                            )
                            viewModel.updatePermission(newPermission)
                            showAddPermissionDialog = false
                        }
                    },
                    enabled = targetUserEmail.isNotBlank() && selectedFileId.isNotBlank()
                ) {
                    Text("Grant")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UserKeyCard(
    userKey: UserKeyEntity,
    onCopyPublicKey: () -> Unit
) {
    var showPem by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = userKey.userEmail,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = userKey.keyAlgorithm,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row {
                    IconButton(onClick = onCopyPublicKey) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Public Key")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SHA-256 Fingerprint: ${userKey.fingerprintSha256}",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (showPem) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = userKey.publicKeyPem,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }
            }

            TextButton(
                onClick = { showPem = !showPem },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (showPem) "Hide Public Key PEM" else "Show Public Key PEM", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun AclCard(
    permission: UserPermissionEntity,
    fileName: String,
    onToggleRead: () -> Unit,
    onToggleDownload: () -> Unit,
    onToggleShare: () -> Unit,
    onToggleRevoke: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = permission.userEmail,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "File: $fileName",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Role: ${permission.userRole}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Permission",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Checkbox Capabilities Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PermissionCheckbox("Read", permission.canRead, onToggleRead)
                PermissionCheckbox("Download", permission.canDownload, onToggleDownload)
                PermissionCheckbox("Share", permission.canShare, onToggleShare)
                PermissionCheckbox("Revoke", permission.canRevoke, onToggleRevoke)
            }
        }
    }
}

@Composable
private fun PermissionCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp)
    }
}
