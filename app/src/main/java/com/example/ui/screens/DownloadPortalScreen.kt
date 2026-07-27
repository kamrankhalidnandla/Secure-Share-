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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.data.model.FileStatus
import com.example.ui.components.SecurityBadge
import com.example.ui.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DownloadPortalScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    var linkInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    val files by viewModel.allFiles.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Consumer Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Secure Download & Link Consumer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Paste a SecureShare URL token to decrypt and download the encrypted file payload.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = linkInput,
                    onValueChange = { linkInput = it },
                    label = { Text("Paste Share Link or Token") },
                    placeholder = { Text("https://secureshare.enterprise.vault/v1/download/...?token=...") },
                    leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.getText()?.let { linkInput = it.text }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Paste")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Decryption Password (If Protected)") },
                    leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (linkInput.isNotBlank()) {
                            viewModel.consumeShareToken(
                                tokenInput = linkInput,
                                passwordInput = if (passwordInput.isNotBlank()) passwordInput else null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = linkInput.isNotBlank()
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Decrypt & Download File")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Active Vault Share Links (${files.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(files, key = { it.id }) { file ->
                val fullLink = CryptoEngine.generateShareLink(file.id, file.shareToken)

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = file.fileName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                SecurityBadge(status = file.status)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Token: ${file.shareToken}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Text(
                                text = "Downloads: ${file.downloadCount}" + if (file.maxDownloads > 0) " / ${file.maxDownloads}" else "",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (file.expirationTimestamp != null) {
                                Text(
                                    text = "Expires: ${dateFormat.format(Date(file.expirationTimestamp))}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(fullLink))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link")
                            }

                            if (file.status == FileStatus.ACTIVE.name) {
                                IconButton(onClick = {
                                    viewModel.revokeFile(file.id)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "Revoke Link",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
