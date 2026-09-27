package com.hrshd1eux.expensetracker.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * An interactive 3x3 Pattern Lock View using Compose Canvas and gesture detection.
 * Dots are indexed 0 to 8:
 *  0  1  2
 *  3  4  5
 *  6  7  8
 */
@Composable
fun PatternLockView(
    onPatternComplete: (String) -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val selectedDots = remember { mutableStateListOf<Int>() }
    var currentTouchPosition by remember { mutableStateOf<Offset?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val dotColor = MaterialTheme.colorScheme.outlineVariant
    val activeColor = if (isError) errorColor else primaryColor

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(24.dp)
            .pointerInput(isError) {
                detectDragGestures(
                    onDragStart = { offset ->
                        selectedDots.clear()
                        currentTouchPosition = offset
                        val dotIndex = getDotIndexAtOffset(offset, size.width.toFloat(), size.height.toFloat())
                        if (dotIndex != null && !selectedDots.contains(dotIndex)) {
                            selectedDots.add(dotIndex)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentTouchPosition = change.position
                        val dotIndex = getDotIndexAtOffset(change.position, size.width.toFloat(), size.height.toFloat())
                        if (dotIndex != null && !selectedDots.contains(dotIndex)) {
                            selectedDots.add(dotIndex)
                        }
                    },
                    onDragEnd = {
                        currentTouchPosition = null
                        if (selectedDots.isNotEmpty()) {
                            val patternString = selectedDots.joinToString("-")
                            onPatternComplete(patternString)
                        }
                    },
                    onDragCancel = {
                        currentTouchPosition = null
                        selectedDots.clear()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cellWidth = width / 3f
            val cellHeight = height / 3f

            fun getDotCenter(index: Int): Offset {
                val row = index / 3
                val col = index % 3
                return Offset(
                    x = col * cellWidth + cellWidth / 2f,
                    y = row * cellHeight + cellHeight / 2f
                )
            }

            // Draw connecting lines between selected dots
            if (selectedDots.size > 1) {
                for (i in 0 until selectedDots.size - 1) {
                    val start = getDotCenter(selectedDots[i])
                    val end = getDotCenter(selectedDots[i + 1])
                    drawLine(
                        color = activeColor.copy(alpha = 0.7f),
                        start = start,
                        end = end,
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw live line from last selected dot to current touch pointer
            if (selectedDots.isNotEmpty() && currentTouchPosition != null) {
                val start = getDotCenter(selectedDots.last())
                drawLine(
                    color = activeColor.copy(alpha = 0.5f),
                    start = start,
                    end = currentTouchPosition!!,
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draw 3x3 dots
            for (i in 0 until 9) {
                val center = getDotCenter(i)
                val isSelected = selectedDots.contains(i)

                if (isSelected) {
                    // Outer highlighted glow ring
                    drawCircle(
                        color = activeColor.copy(alpha = 0.25f),
                        radius = 28.dp.toPx(),
                        center = center
                    )
                    // Inner colored dot
                    drawCircle(
                        color = activeColor,
                        radius = 12.dp.toPx(),
                        center = center
                    )
                } else {
                    // Inactive normal dot
                    drawCircle(
                        color = dotColor,
                        radius = 8.dp.toPx(),
                        center = center
                    )
                }
            }
        }
    }
}

private fun getDotIndexAtOffset(offset: Offset, width: Float, height: Float): Int? {
    val cellWidth = width / 3f
    val cellHeight = height / 3f
    val touchRadius = cellWidth * 0.38f

    for (row in 0 until 3) {
        for (col in 0 until 3) {
            val centerX = col * cellWidth + cellWidth / 2f
            val centerY = row * cellHeight + cellHeight / 2f
            val distance = hypot(offset.x - centerX, offset.y - centerY)
            if (distance <= touchRadius) {
                return row * 3 + col
            }
        }
    }
    return null
}
