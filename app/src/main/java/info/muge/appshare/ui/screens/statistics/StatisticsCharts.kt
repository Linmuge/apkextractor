package info.muge.appshare.ui.screens.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.muge.appshare.R
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.theme.AppDimens
import kotlin.math.atan2
import kotlin.math.sqrt

internal enum class ChartMode {
    PIE,
    BAR
}

/**
 * 存放饼图和柱状图的容器（完全去阴影扁平容器）
 */
@Composable
internal fun ChartCard(
    entries: List<Map.Entry<String, List<AppItem>>>,
    chartMode: ChartMode,
    chartColors: List<Color>,
    isRefreshing: Boolean,
    onChartToggle: () -> Unit,
    onSliceClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Radius.xl))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(AppDimens.Space.lg)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (entries.isEmpty() && !isRefreshing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_rule),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(AppDimens.Space.md))
                    Text(
                        text = "暂无数据",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp)
                ) {
                    if (chartMode == ChartMode.PIE) {
                        ComposeAnimatedDonutChart(
                            entries = entries,
                            chartColors = chartColors,
                            onSliceClick = onSliceClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        )
                    } else {
                        ComposeAnimatedBarChart(
                            entries = entries,
                            chartColors = chartColors,
                            onBarClick = onSliceClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onChartToggle,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (chartMode == ChartMode.PIE) Icons.Default.BarChart else Icons.Default.PieChart,
                    contentDescription = "切换图表",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 带有丝滑画入动画的环形饼图
 */
@Composable
private fun ComposeAnimatedDonutChart(
    entries: List<Map.Entry<String, List<AppItem>>>,
    chartColors: List<Color>,
    onSliceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = remember(entries) { entries.sumOf { it.value.size } }
    
    // 进场展开动画
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(entries) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(entries, totalCount) {
                    detectTapGestures { tapOffset ->
                        if (entries.isEmpty() || totalCount <= 0) return@detectTapGestures

                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = tapOffset.x - centerX
                        val dy = tapOffset.y - centerY
                        val distance = sqrt(dx * dx + dy * dy)
                        val outerRadius = (minOf(size.width, size.height) / 2f) * 0.85f
                        val innerRadius = outerRadius * 0.65f // Donut inner radius

                        if (distance !in innerRadius..outerRadius) return@detectTapGestures

                        val angle = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 450.0) % 360.0).toFloat()
                        var startAngle = 0f
                        entries.forEach { entry ->
                            val sweep = (entry.value.size.toFloat() / totalCount.toFloat()) * 360f
                            val endAngle = startAngle + sweep
                            if (angle in startAngle..endAngle) {
                                onSliceClick(entry.key)
                                return@detectTapGestures
                            }
                            startAngle = endAngle
                        }
                    }
                }
        ) {
            var startAngle = -90f
            val radius = minOf(size.width, size.height) / 2f * 0.75f
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeWidth = radius * 0.38f

            entries.forEachIndexed { index, entry ->
                // 计算动画控制的扫略角
                val targetSweepAngle = (entry.value.size.toFloat() / totalCount.coerceAtLeast(1).toFloat()) * 360f
                val currentSweepAngle = targetSweepAngle * animationProgress.value
                val color = chartColors[index % chartColors.size]

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = currentSweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += targetSweepAngle // startAngle 依赖完整计算
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "总计",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = totalCount.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    Spacer(modifier = Modifier.height(AppDimens.Space.lg))

    // 底部标签图示 (图例)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Space.xs)
    ) {
        entries.take(6).forEachIndexed { index, entry ->
            val color = chartColors[index % chartColors.size]
            val percentage = (entry.value.size.toFloat() / totalCount.coerceAtLeast(1).toFloat()) * 100f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDimens.Radius.sm))
                    .clickable { onSliceClick(entry.key) }
                    .padding(vertical = AppDimens.Space.xs, horizontal = AppDimens.Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(AppDimens.Space.sm))
                Text(
                    text = entry.key,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = String.format("%.1f%%", percentage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 带有进入动效的极简单色条形图
 */
@Composable
private fun ComposeAnimatedBarChart(
    entries: List<Map.Entry<String, List<AppItem>>>,
    chartColors: List<Color>,
    onBarClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = remember(entries) { entries.take(12) }
    val maxCount = remember(data) { data.maxOfOrNull { it.value.size }?.toFloat() ?: 0f }

    // 条形图展宽动画
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(entries) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        data.forEachIndexed { index, entry ->
            val color = chartColors[index % chartColors.size]
            val targetPercentage = if (maxCount > 0f) entry.value.size.toFloat() / maxCount else 0f
            val currentPercentage = targetPercentage * animationProgress.value

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDimens.Radius.sm))
                    .clickable { onBarClick(entry.key) }
                    .padding(vertical = AppDimens.Space.sm, horizontal = AppDimens.Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.key,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(92.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(AppDimens.Space.sm))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(currentPercentage)
                            .clip(RoundedCornerShape(AppDimens.Radius.full))
                            .background(color)
                    )
                }

                Spacer(modifier = Modifier.width(AppDimens.Space.sm))

                Text(
                    text = entry.value.size.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
