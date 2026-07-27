package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val highlights: List<String>
)

@Composable
fun OnboardingOverlay(
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }

    val steps = listOf(
        OnboardingStep(
            title = "Zero-Trust E2EE Transfers",
            subtitle = "AES-256-GCM Hardware Encryption",
            description = "Files are encrypted locally on client devices using random 256-bit symmetric keys. Payloads never touch servers unencrypted.",
            icon = Icons.Default.EnhancedEncryption,
            accentColor = Color(0xFF0284C7),
            highlights = listOf(
                "Client-side AES-256-GCM authenticated encryption",
                "Password key derivation via PBKDF2 (100,000 rounds)",
                "RSA-2048 public key key-wrapping for granular recipient delivery"
            )
        ),
        OnboardingStep(
            title = "Link Expiration & Self-Destruct",
            subtitle = "Automatic TTL & Limit Enforcement",
            description = "Protect files against perpetual exposure. Configure tight expiration windows and maximum download counts.",
            icon = Icons.Default.Timer,
            accentColor = Color(0xFFF59E0B),
            highlights = listOf(
                "Time-To-Live options: 15m, 1h, 24h, or 30 days",
                "Max download count self-destruct triggers",
                "Instant one-click manual revokation of active share tokens"
            )
        ),
        OnboardingStep(
            title = "Enterprise ACL & Key Vault",
            subtitle = "Granular Access Control & Audits",
            description = "Manage recipient permissions (Read, Download, Revoke) and verify cryptographic fingerprints in real-time.",
            icon = Icons.Default.AdminPanelSettings,
            accentColor = Color(0xFF10B981),
            highlights = listOf(
                "Role-based ACL permissions per file payload",
                "Automated tamper-evident cryptographic audit logs",
                "Exportable compliance security reports for enterprise verification"
            )
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(if (true) 0.95f else 1f)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .background(
                                    color = steps[currentStep].accentColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = steps[currentStep].accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ENTERPRISE SECURITY GUIDE (${currentStep + 1}/${steps.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = steps[currentStep].accentColor
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Step Icon Header
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    color = steps[currentStep].accentColor.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = steps[currentStep].icon,
                                contentDescription = null,
                                tint = steps[currentStep].accentColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Animated Step Content
                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "OnboardingContent"
                        ) { stepIndex ->
                            val step = steps[stepIndex]
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = step.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = step.accentColor,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = step.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    step.highlights.forEach { highlight ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = step.accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = highlight,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Page Dots Indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            steps.indices.forEach { index ->
                                Box(
                                    modifier = Modifier
                                        .height(8.dp)
                                        .width(if (index == currentStep) 24.dp else 8.dp)
                                        .background(
                                            color = if (index == currentStep) steps[currentStep].accentColor else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Navigation Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentStep > 0) {
                                OutlinedButton(onClick = { currentStep-- }) {
                                    Text("Back")
                                }
                            } else {
                                TextButton(onClick = onDismiss) {
                                    Text("Skip")
                                }
                            }

                            Button(
                                onClick = {
                                    if (currentStep < steps.size - 1) {
                                        currentStep++
                                    } else {
                                        onDismiss()
                                    }
                                }
                            ) {
                                Text(if (currentStep == steps.size - 1) "Get Started" else "Next")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.NavigateNext,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
