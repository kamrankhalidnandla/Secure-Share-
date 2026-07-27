package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 160.dp
) {
    Box(
        modifier = modifier
            .size(sizeDp)
            .background(Color.White, shape = RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp - 24.dp)) {
            val gridCount = 15
            val cellSize = this.size.width / gridCount

            val hash = data.hashCode()

            for (row in 0 until gridCount) {
                for (col in 0 until gridCount) {
                    // Draw QR Corner Finder patterns
                    val isTopLeftCorner = row < 4 && col < 4
                    val isTopRightCorner = row < 4 && col >= gridCount - 4
                    val isBottomLeftCorner = row >= gridCount - 4 && col < 4

                    val isFinderBorder = (isTopLeftCorner && (row == 0 || row == 3 || col == 0 || col == 3)) ||
                            (isTopRightCorner && (row == 0 || row == 3 || col == gridCount - 1 || col == gridCount - 4)) ||
                            (isBottomLeftCorner && (row == gridCount - 1 || row == gridCount - 4 || col == 0 || col == 3))

                    val isFinderCenter = (isTopLeftCorner && row in 1..2 && col in 1..2) ||
                            (isTopRightCorner && row in 1..2 && col in gridCount - 3..gridCount - 2) ||
                            (isBottomLeftCorner && row in gridCount - 3..gridCount - 2 && col in 1..2)

                    val pseudoBit = abs((row * 31 + col * 17 + hash) xor (row shl 3)) % 2 == 0

                    if (isFinderBorder || isFinderCenter || (!isTopLeftCorner && !isTopRightCorner && !isBottomLeftCorner && pseudoBit)) {
                        drawRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size = Size(cellSize * 0.92f, cellSize * 0.92f)
                        )
                    }
                }
            }
        }
    }
}
