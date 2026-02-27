package info.muge.appshare.ui.screens.statistics

import android.text.format.Formatter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.muge.appshare.ui.screens.DetailSortMode
import info.muge.appshare.ui.screens.StatisticsEvent
import info.muge.appshare.ui.screens.StatisticsUiState
import info.muge.appshare.ui.screens.StatisticsViewModel
import info.muge.appshare.ui.screens.InstallTrendPoint
import info.muge.appshare.ui.screens.SplitApkStats
import info.muge.appshare.ui.screens.StorageOverview
import info.muge.appshare.ui.theme.AppDimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 纯平扁平化概览卡片，使用主题色容器组合
 */
@Composable
internal fun SummaryCard(
    totalCount: Int,
    categoryCount: Int,
    topCategory: String?,
    topCategoryCount: Int,
    lastUpdatedAt: Long?,
    onRefresh: () -> Unit,
    onShowConfig: () -> Unit,
    showConfigMenu: Boolean,
    onDismissConfigMenu: () -> Unit,
    uiState: StatisticsUiState,
    viewModel: StatisticsViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Radius.xl))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(AppDimens.Space.xl)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "统计概览",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新统计",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box {
                        IconButton(onClick = onShowConfig, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "配置",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showConfigMenu,
                            onDismissRequest = onDismissConfigMenu
                        ) {
                            DropdownMenuItem(
                                text = { Text("显示全部") },
                                onClick = {
                                    viewModel.onEvent(StatisticsEvent.ChangeLimit(0))
                                    onDismissConfigMenu()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Top 10") },
                                onClick = {
                                    viewModel.onEvent(StatisticsEvent.ChangeLimit(10))
                                    onDismissConfigMenu()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Top 20") },
                                onClick = {
                                    viewModel.onEvent(StatisticsEvent.ChangeLimit(20))
                                    onDismissConfigMenu()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (uiState.hideSingleItems) "显示全部项" else "隐藏单位项") },
                                onClick = {
                                    viewModel.onEvent(StatisticsEvent.ToggleHideSingleItems)
                                    onDismissConfigMenu()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("切换排序 (${uiState.detailSortMode.label})") },
                                onClick = {
                                    val nextMode = if (uiState.detailSortMode == DetailSortMode.COUNT_DESC) DetailSortMode.LABEL_ASC else DetailSortMode.COUNT_DESC
                                    viewModel.onEvent(StatisticsEvent.ChangeSortMode(nextMode))
                                    onDismissConfigMenu()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.Space.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = totalCount.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "应用总数",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "分类: $categoryCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!topCategory.isNullOrBlank()) {
                        Text(
                            text = "最多: $topCategory ($topCategoryCount)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.Space.md))

            Text(
                text = lastUpdatedAt?.let { "更新于 ${formatTime(it)}" } ?: "尚未更新",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 存储空间概览卡片
 */
@Composable
internal fun StorageOverviewCard(
    overview: StorageOverview
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Radius.xl))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(AppDimens.Space.lg),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Space.md)
    ) {
        Text(
            text = "存储空间分析",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // 概览数据行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatMetricItem("总占用", Formatter.formatFileSize(context, overview.totalSize))
            StatMetricItem("平均大小", Formatter.formatFileSize(context, overview.avgSize))
            StatMetricItem("中位数", Formatter.formatFileSize(context, overview.medianSize))
        }

        if (overview.maxSizeApp.isNotBlank()) {
            Text(
                text = "最大应用: ${overview.maxSizeApp} (${Formatter.formatFileSize(context, overview.maxSize)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // TOP 10 水平条形图
        if (overview.top10Apps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AppDimens.Space.xs))
            Text(
                text = "TOP 10 应用",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val maxSize = overview.top10Apps.maxOfOrNull { it.second }?.toFloat() ?: 1f

            // 进场动画
            val animProgress = remember { Animatable(0f) }
            LaunchedEffect(overview.top10Apps) {
                animProgress.snapTo(0f)
                animProgress.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
            }

            overview.top10Apps.forEach { (name, size) ->
                val fraction = (size.toFloat() / maxSize) * animProgress.value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(90.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(AppDimens.Space.sm))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(RoundedCornerShape(AppDimens.Radius.full))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.width(AppDimens.Space.sm))
                    Text(
                        text = Formatter.formatFileSize(context, size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(60.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
internal fun StatMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 安装趋势折线图
 */
@Composable
internal fun InstallTrendChart(
    data: List<InstallTrendPoint>,
    chartColors: List<Color>
) {
    if (data.isEmpty()) return

    val lineColor = chartColors.firstOrNull() ?: MaterialTheme.colorScheme.primary
    val fillColor = lineColor.copy(alpha = 0.15f)
    val maxCount = data.maxOf { it.count }.toFloat().coerceAtLeast(1f)

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Radius.xl))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(AppDimens.Space.lg)
    ) {
        Text(
            text = "安装趋势",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(AppDimens.Space.md))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val w = size.width
            val h = size.height
            val padding = 8f

            if (data.size < 2) return@Canvas

            val step = (w - padding * 2) / (data.size - 1)

            val points = data.mapIndexed { i, pt ->
                val x = padding + i * step
                val y = h - padding - (pt.count / maxCount) * (h - padding * 2) * animProgress.value
                Offset(x, y)
            }

            // 填充区域
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, h - padding)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, h - padding)
                close()
            }
            drawPath(path, fillColor)

            // 线条
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = lineColor,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }

            // 数据点
            points.forEach { pt ->
                drawCircle(lineColor, radius = 4f, center = pt)
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.Space.sm))

        // 底部标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { pt ->
                Text(
                    text = pt.label.takeLast(5), // "MM" or "yy-MM"
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 导出统计卡片
 */
@Composable
internal fun ExportStatsCard() {
    val context = LocalContext.current
    val totalCount = remember { info.muge.appshare.data.ExportStatsManager.getTotalCount(context) }
    val totalSize = remember { info.muge.appshare.data.ExportStatsManager.getTotalSize(context) }
    val topApps = remember { info.muge.appshare.data.ExportStatsManager.getTopExportedApps(context) }
    val exportTrend = remember { info.muge.appshare.data.ExportStatsManager.getExportTrend(context) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Radius.xl))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(AppDimens.Space.lg),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Space.md)
    ) {
        Text(
            text = "导出统计",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // 总计数据
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatMetricItem("总导出次数", totalCount.toString())
            StatMetricItem("总导出大小", Formatter.formatFileSize(context, totalSize))
        }

        if (totalCount == 0L) {
            Text(
                text = "暂无导出记录，导出应用后数据将在此显示",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimens.Space.xl)
            )
        }

        // 最常导出的应用
        if (topApps.isNotEmpty()) {
            Text(
                text = "最常导出应用",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val maxExportCount = topApps.maxOf { it.second }.toFloat().coerceAtLeast(1f)

            topApps.forEach { (name, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(100.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(AppDimens.Space.sm))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(count / maxExportCount)
                                .clip(RoundedCornerShape(AppDimens.Radius.full))
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                    }
                    Spacer(modifier = Modifier.width(AppDimens.Space.sm))
                    Text(
                        text = "${count}次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        // 导出趋势
        if (exportTrend.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AppDimens.Space.sm))
            Text(
                text = "导出趋势",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val maxTrend = exportTrend.maxOf { it.second }.toFloat().coerceAtLeast(1f)
            exportTrend.forEach { (month, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = month,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(60.dp)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.Space.sm))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(count / maxTrend)
                                .clip(RoundedCornerShape(AppDimens.Radius.full))
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    }
                    Spacer(modifier = Modifier.width(AppDimens.Space.sm))
                    Text(
                        text = "${count}次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

/**
 * Split APK 概览卡片
 */
@Composable
internal fun SplitApkOverviewCard(
    stats: SplitApkStats
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Radius.xl))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(AppDimens.Space.lg),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Space.md)
    ) {
        Text(
            text = "Split APK 分析",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // 概览数据
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatMetricItem("Split 应用", "${stats.splitAppCount}")
            StatMetricItem("总 Split 数", "${stats.totalSplitCount}")
            StatMetricItem(
                "占比",
                if (stats.totalApps > 0) {
                    "${(stats.splitAppCount * 100 / stats.totalApps)}%"
                } else "0%"
            )
        }

        // 各 split 组件类型分布
        if (stats.splitTypes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AppDimens.Space.xs))
            Text(
                text = "Split 组件类型分布",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val maxCount = stats.splitTypes.values.maxOrNull()?.toFloat() ?: 1f

            stats.splitTypes.forEach { (typeName, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = typeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(110.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(AppDimens.Space.sm))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(count / maxCount)
                                .clip(RoundedCornerShape(AppDimens.Radius.full))
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    }
                    Spacer(modifier = Modifier.width(AppDimens.Space.sm))
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

internal fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
