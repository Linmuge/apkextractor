package info.muge.appshare.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import info.muge.appshare.ui.theme.ChartColors

/**
 * 饼图数据类
 */
data class PieChartData(
    val label: String,
    val value: Float,
    val color: Color? = null
)

/**
 * 饼图组件
 */
@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 200.dp,
    onSliceClick: ((PieChartData) -> Unit)? = null
) {
    val totalValue = data.sumOf { it.value.toDouble() }.toFloat()
    val colors = ChartColors
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(size)
        ) {
            var startAngle = -90f
            val radius = size.toPx() / 2 * 0.85f
            val center = Offset(size.toPx() / 2, size.toPx() / 2)

            data.forEachIndexed { index, item ->
                val sweepAngle = (item.value / totalValue * 360)
                val color = item.color ?: colors[index % colors.size]

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
                startAngle += sweepAngle
            }

            // 绘制中心圆（创建环形效果）
            drawCircle(
                color = surfaceColor,
                radius = radius * 0.55f,
                center = center
            )
        }

        // 中心文字
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "总计",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = totalValue.toInt().toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 条形图数据类
 */
data class BarChartData(
    val label: String,
    val value: Float,
    val color: Color? = null
)

/**
 * 简单条形图组件
 */
@Composable
fun SimpleBarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier,
    maxValue: Float? = null
) {
    val colors = ChartColors
    val max = maxValue ?: (data.maxOfOrNull { it.value } ?: 0f)

    Column(
        modifier = modifier
    ) {
        data.forEachIndexed { index, item ->
            val color = item.color ?: colors[index % colors.size]
            val percentage = if (max > 0) item.value / max else 0f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(60.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(
                                color = color.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percentage)
                            .height(24.dp)
                            .background(
                                color = color,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }

                Text(
                    text = item.value.toInt().toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
