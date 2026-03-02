package com.cattrack.app.ui.data

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pure Compose canvas-based chart components (alternative to MPAndroidChart)
 */
@Composable
fun ComposeLineChart(
    dataPoints: List<Float>,
    lineColor: Color,
    fillColor: Color = lineColor.copy(alpha = 0.2f),
    modifier: Modifier = Modifier,
    height: Dp = 160.dp
) {
    if (dataPoints.size < 2) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val maxVal = dataPoints.max()
        val minVal = dataPoints.min()
        val range = (maxVal - minVal).coerceAtLeast(1f)
        val xStep = size.width / (dataPoints.size - 1).coerceAtLeast(1)
        val padding = 16.dp.toPx()

        // Draw grid lines
        val gridCount = 4
        repeat(gridCount) { i ->
            val y = padding + (size.height - 2 * padding) * i / (gridCount - 1)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        // Build path
        val path = Path()
        val fillPath = Path()

        dataPoints.forEachIndexed { index, value ->
            val x = xStep * index
            val normalizedY = 1f - (value - minVal) / range
            val y = padding + normalizedY * (size.height - 2 * padding)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = xStep * (index - 1)
                val controlX = (prevX + x) / 2
                path.cubicTo(controlX, path.getBounds().bottom, controlX, y, x, y)
                fillPath.lineTo(x, y)
            }
        }

        // Close fill path
        fillPath.lineTo(xStep * (dataPoints.size - 1), size.height)
        fillPath.close()

        drawPath(fillPath, color = fillColor)
        drawPath(
            path, color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw data points
        dataPoints.forEachIndexed { index, value ->
            val x = xStep * index
            val normalizedY = 1f - (value - minVal) / range
            val y = padding + normalizedY * (size.height - 2 * padding)
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun ComposeBarChart(
    dataPoints: List<Float>,
    barColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp
) {
    if (dataPoints.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val maxVal = dataPoints.max().coerceAtLeast(1f)
        val barWidth = (size.width / dataPoints.size) * 0.6f
        val gap = (size.width / dataPoints.size) * 0.4f / 2
        val bottomPad = 8.dp.toPx()

        dataPoints.forEachIndexed { index, value ->
            val x = index * (barWidth + gap * 2) + gap
            val barHeight = (value / maxVal) * (size.height - bottomPad)
            val y = size.height - barHeight - bottomPad

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
        }
    }
}
