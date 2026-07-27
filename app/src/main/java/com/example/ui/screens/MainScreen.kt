package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import com.example.ui.components.OnboardingOverlay
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: VaultViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserEmail by viewModel.currentUserEmail.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()

    var showSessionMenu by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "SecureShare",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFF065F46),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AES-256",
                                    color = Color(0xFF34D399),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showOnboarding = true }) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Security Guide & Onboarding")
                        }

                        Box {
                            IconButton(onClick = { showSessionMenu = true }) {
                                Icon(Icons.Default.Person, contentDescription = "User Session")
                            }
                            DropdownMenu(
                                expanded = showSessionMenu,
                                onDismissRequest = { showSessionMenu = false }
                            ) {
                                Text(
                                    text = " Active Session",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("admin@enterprise.secureshare", fontWeight = FontWeight.Bold)
                                            Text("Administrator Role", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    },
                                    onClick = {
                                        viewModel.switchUserSession("admin@enterprise.secureshare", UserRole.ADMIN.displayName)
                                        showSessionMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("alice@enterprise.secureshare", fontWeight = FontWeight.Bold)
                                            Text("Security Manager Role", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    },
                                    onClick = {
                                        viewModel.switchUserSession("alice@enterprise.secureshare", UserRole.SECURITY_MANAGER.displayName)
                                        showSessionMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("bob@enterprise.secureshare", fontWeight = FontWeight.Bold)
                                            Text("Viewer Role", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    },
                                    onClick = {
                                        viewModel.switchUserSession("bob@enterprise.secureshare", UserRole.VIEWER.displayName)
                                        showSessionMenu = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!isExpanded) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.FolderSpecial, contentDescription = "Vault") },
                            label = { Text("Vault") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Link, contentDescription = "Links") },
                            label = { Text("Links") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.Assessment, contentDescription = "Audit Logs") },
                            label = { Text("Audit Logs") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "ACL & Keys") },
                            label = { Text("ACL & Keys") }
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpanded) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.FolderSpecial, contentDescription = "Vault") },
                            label = { Text("Vault") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Link, contentDescription = "Links") },
                            label = { Text("Links") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.Assessment, contentDescription = "Audit Logs") },
                            label = { Text("Audit Logs") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "ACL & Keys") },
                            label = { Text("ACL & Keys") }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1200.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (selectedTab) {
                        0 -> VaultScreen(viewModel = viewModel)
                        1 -> DownloadPortalScreen(viewModel = viewModel)
                        2 -> AuditLogScreen(viewModel = viewModel)
                        3 -> AccessControlScreen(viewModel = viewModel)
                    }
                }
            }
        }

        if (showOnboarding) {
            OnboardingOverlay(onDismiss = { showOnboarding = false })
        }
    }
}
