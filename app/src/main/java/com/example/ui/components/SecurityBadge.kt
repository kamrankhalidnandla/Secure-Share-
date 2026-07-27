package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileStatus

@Composable
fun SecurityBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon, label) = when (status) {
        FileStatus.ACTIVE.name -> Quadruple(
            Color(0xFF065F46),
            Color(0xFF34D399),
            Icons.Default.CheckCircle,
            "ACTIVE (E2EE)"
        )
        FileStatus.EXPIRED.name -> Quadruple(
            Color(0xFF78350F),
            Color(0xFFFBBF24),
            Icons.Default.LockClock,
            "EXPIRED"
        )
        FileStatus.REVOKED.name -> Quadruple(
            Color(0xFF7F1D1D),
            Color(0xFFFCA5A5),
            Icons.Default.Block,
            "REVOKED"
        )
        FileStatus.SELF_DESTRUCTED.name -> Quadruple(
            Color(0xFF450A0A),
            Color(0xFFEF4444),
            Icons.Default.Warning,
            "PURGED / DESTRUCTED"
        )
        else -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.CheckCircle,
            status
        )
    }

    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
