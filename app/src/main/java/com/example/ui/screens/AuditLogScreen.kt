package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLogEntity
import com.example.data.model.SeverityLevel
import com.example.ui.components.StatCard
import com.example.ui.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuditLogScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val auditLogs by viewModel.filteredAuditLogs.collectAsState()
    val allLogs by viewModel.allAuditLogs.collectAsState()
    val searchQuery by viewModel.auditSearchQuery.collectAsState()
    val severityFilter by viewModel.auditSeverityFilter.collectAsState()

    var showReportDialog by remember { mutableStateOf(false) }
    var selectedActionCategory by remember { mutableStateOf<String?>(null) }
    var showChartSection by remember { mutableStateOf(true) }

    val totalEvents = allLogs.size
    val alertCount = allLogs.count { it.severity == SeverityLevel.SECURITY_ALERT.name }
    val warningCount = allLogs.count { it.severity == SeverityLevel.WARNING.name }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val clipboardManager = LocalClipboardManager.current

    // Category filtered list
    val displayedLogs = remember(auditLogs, selectedActionCategory) {
        if (selectedActionCategory == null) auditLogs
        else auditLogs.filter { log ->
            when (selectedActionCategory) {
                "DECRYPT" -> log.actionType.contains("DECRYPT", ignoreCase = true)
                "ENCRYPT" -> log.actionType.contains("ENCRYPT", ignoreCase = true) || log.actionType.contains("UPLOAD", ignoreCase = true)
                "SHARE" -> log.actionType.contains("SHARE", ignoreCase = true) || log.actionType.contains("TOKEN", ignoreCase = true)
                "SECURITY" -> log.severity == SeverityLevel.SECURITY_ALERT.name || log.actionType.contains("REVOKE", ignoreCase = true)
                else -> true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Total Events",
                value = totalEvents.toString(),
                subtitle = "Audit Trail",
                icon = Icons.Default.Assessment,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Warnings",
                value = warningCount.toString(),
                subtitle = "System Alerts",
                icon = Icons.Default.Warning,
                accentColor = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Security Alerts",
                value = alertCount.toString(),
                subtitle = "Failed / Denied",
                icon = Icons.Default.Security,
                accentColor = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Visual Activity Dashboard Chart Toggle & Card
        if (allLogs.isNotEmpty() && showChartSection) {
            ActivityChartCard(
                logs = allLogs,
                onCategorySelect = { cat ->
                    selectedActionCategory = if (selectedActionCategory == cat) null else cat
                },
                activeCategory = selectedActionCategory
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Search Bar & Export Report
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.auditSearchQuery.value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search audit logs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { showReportDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Export")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Category & Severity Filter Chips (Horizontal Scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = severityFilter == null && selectedActionCategory == null,
                onClick = {
                    viewModel.auditSeverityFilter.value = null
                    selectedActionCategory = null
                },
                label = { Text("All ($totalEvents)") }
            )
            FilterChip(
                selected = selectedActionCategory == "ENCRYPT",
                onClick = {
                    selectedActionCategory = if (selectedActionCategory == "ENCRYPT") null else "ENCRYPT"
                },
                label = { Text("Uploads/Encryptions") }
            )
            FilterChip(
                selected = selectedActionCategory == "DECRYPT",
                onClick = {
                    selectedActionCategory = if (selectedActionCategory == "DECRYPT") null else "DECRYPT"
                },
                label = { Text("File Downloads") }
            )
            FilterChip(
                selected = selectedActionCategory == "SHARE",
                onClick = {
                    selectedActionCategory = if (selectedActionCategory == "SHARE") null else "SHARE"
                },
                label = { Text("Share Tokens") }
            )
            FilterChip(
                selected = severityFilter == SeverityLevel.WARNING,
                onClick = {
                    viewModel.auditSeverityFilter.value =
                        if (severityFilter == SeverityLevel.WARNING) null else SeverityLevel.WARNING
                },
                label = { Text("Warnings ($warningCount)") }
            )
            FilterChip(
                selected = severityFilter == SeverityLevel.SECURITY_ALERT,
                onClick = {
                    viewModel.auditSeverityFilter.value =
                        if (severityFilter == SeverityLevel.SECURITY_ALERT) null else SeverityLevel.SECURITY_ALERT
                },
                label = { Text("Alerts ($alertCount)") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logs Timeline List
        if (displayedLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No audit log entries match current filter.",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(displayedLogs, key = { it.id }) { log ->
                    AuditLogCard(log = log, dateFormat = dateFormat)
                }
            }
        }
    }

    if (showReportDialog) {
        val reportText = buildString {
            appendLine("==================================================")
            appendLine("      SECURESHARE ENTERPRISE AUDIT REPORT        ")
            appendLine("==================================================")
            appendLine("Generated At: ${dateFormat.format(Date())}")
            appendLine("Total Records: ${allLogs.size}")
            appendLine("--------------------------------------------------")
            allLogs.forEach { log ->
                appendLine("[${dateFormat.format(Date(log.timestamp))}] [${log.severity}] [${log.actionType}]")
                appendLine("Actor: ${log.actorEmail} (${log.actorRole})")
                appendLine("Details: ${log.details}")
                if (log.fileName != null) appendLine("File: ${log.fileName}")
                appendLine("--------------------------------------------------")
            }
        }

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Enterprise Security Audit Report") },
            text = {
                Column {
                    Text("Exportable security log text for compliance audit verification.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = reportText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(reportText))
                    showReportDialog = false
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ActivityChartCard(
    logs: List<AuditLogEntity>,
    onCategorySelect: (String) -> Unit,
    activeCategory: String?
) {
    val encryptCount = remember(logs) { logs.count { it.actionType.contains("ENCRYPT", true) || it.actionType.contains("UPLOAD", true) } }
    val decryptCount = remember(logs) { logs.count { it.actionType.contains("DECRYPT", true) } }
    val shareCount = remember(logs) { logs.count { it.actionType.contains("SHARE", true) || it.actionType.contains("TOKEN", true) } }
    val securityCount = remember(logs) { logs.count { it.severity == SeverityLevel.SECURITY_ALERT.name || it.actionType.contains("REVOKE", true) } }

    val total = (encryptCount + decryptCount + shareCount + securityCount).coerceAtLeast(1)

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.InsertChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "File Access & Event Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Interactive Analytics",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Bar Visualizer
            val primaryColor = MaterialTheme.colorScheme.primary
            val successColor = Color(0xFF10B981)
            val warningColor = Color(0xFFF59E0B)
            val errorColor = Color(0xFFEF4444)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                val width = size.width
                val height = size.height

                val encWidth = (encryptCount.toFloat() / total) * width
                val decWidth = (decryptCount.toFloat() / total) * width
                val shareWidth = (shareCount.toFloat() / total) * width
                val secWidth = (securityCount.toFloat() / total) * width

                var currentX = 0f

                if (encWidth > 0) {
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(currentX, 0f),
                        size = Size(encWidth, height),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    currentX += encWidth
                }
                if (decWidth > 0) {
                    drawRoundRect(
                        color = successColor,
                        topLeft = Offset(currentX, 0f),
                        size = Size(decWidth, height),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    currentX += decWidth
                }
                if (shareWidth > 0) {
                    drawRoundRect(
                        color = warningColor,
                        topLeft = Offset(currentX, 0f),
                        size = Size(shareWidth, height),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    currentX += shareWidth
                }
                if (secWidth > 0) {
                    drawRoundRect(
                        color = errorColor,
                        topLeft = Offset(currentX, 0f),
                        size = Size(secWidth, height),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend / Filter Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ChartLegendItem(
                    label = "Uploads ($encryptCount)",
                    color = primaryColor,
                    isSelected = activeCategory == "ENCRYPT",
                    onClick = { onCategorySelect("ENCRYPT") }
                )
                ChartLegendItem(
                    label = "Downloads ($decryptCount)",
                    color = successColor,
                    isSelected = activeCategory == "DECRYPT",
                    onClick = { onCategorySelect("DECRYPT") }
                )
                ChartLegendItem(
                    label = "Tokens ($shareCount)",
                    color = warningColor,
                    isSelected = activeCategory == "SHARE",
                    onClick = { onCategorySelect("SHARE") }
                )
                ChartLegendItem(
                    label = "Alerts ($securityCount)",
                    color = errorColor,
                    isSelected = activeCategory == "SECURITY",
                    onClick = { onCategorySelect("SECURITY") }
                )
            }
        }
    }
}

@Composable
fun ChartLegendItem(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AuditLogCard(
    log: AuditLogEntity,
    dateFormat: SimpleDateFormat
) {
    val (severityColor, severityBg) = when (log.severity) {
        SeverityLevel.SECURITY_ALERT.name -> Pair(Color(0xFFEF4444), Color(0xFF450A0A))
        SeverityLevel.WARNING.name -> Pair(Color(0xFFF59E0B), Color(0xFF78350F))
        else -> Pair(Color(0xFF0284C7), Color(0xFF1E3A8A))
    }

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
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = severityColor, shape = CircleShape)
                    .padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.actionType,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )
                    Text(
                        text = dateFormat.format(Date(log.timestamp)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Actor: ${log.actorEmail} (${log.actorRole})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = log.ipOrDevice,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

