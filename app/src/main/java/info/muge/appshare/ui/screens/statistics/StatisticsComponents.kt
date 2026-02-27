package info.muge.appshare.ui.screens.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.muge.appshare.ui.screens.StatisticsType
import info.muge.appshare.ui.theme.AppDimens

/**
 * Tab 分组定义：将 13 个 StatisticsType 归并为 8 个 Tab
 */
private enum class TabGroup(
    val label: String,
    val subTypes: List<StatisticsType>
) {
    SDK_VERSION("SDK 版本", listOf(StatisticsType.TARGET_SDK, StatisticsType.MIN_SDK, StatisticsType.COMPILE_SDK)),
    APP_FEATURES("应用特征", listOf(StatisticsType.KOTLIN, StatisticsType.ABI, StatisticsType.PAGE_SIZE_16K, StatisticsType.APP_BUNDLE)),
    INSTALLER("安装来源", listOf(StatisticsType.INSTALLER)),
    APP_TYPE("应用类型", listOf(StatisticsType.APP_TYPE)),
    SIZE("大小分布", listOf(StatisticsType.SIZE_DISTRIBUTION)),
    INSTALL("安装时间", listOf(StatisticsType.INSTALL_TIME)),
    EXPORT("导出", listOf(StatisticsType.EXPORT_STATS)),
    PERMISSION("权限", listOf(StatisticsType.PERMISSION));

    val hasSubTypes: Boolean get() = subTypes.size > 1
}

/**
 * 现代化扁平 Tab 设计（分组版）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StatisticsTabRow(
    currentType: StatisticsType,
    onTypeChange: (StatisticsType) -> Unit
) {
    val currentGroup = remember(currentType) {
        TabGroup.entries.first { currentType in it.subTypes }
    }
    val selectedGroupIndex = TabGroup.entries.indexOf(currentGroup)

    Column {
        ScrollableTabRow(
            selectedTabIndex = selectedGroupIndex,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 0.dp,
            divider = {},
            indicator = { tabPositions ->
                if (selectedGroupIndex in tabPositions.indices) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedGroupIndex])
                            .height(3.dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        ) {
            TabGroup.entries.forEachIndexed { index, group ->
                val selected = selectedGroupIndex == index
                Tab(
                    selected = selected,
                    onClick = {
                        // 切换到该分组的第一个子类型（或保持当前子类型如果属于同组）
                        if (currentType !in group.subTypes) {
                            onTypeChange(group.subTypes.first())
                        }
                    },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = group.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }

        // 子类型 FilterChip 行（仅当分组有多个子类型时显示）
        AnimatedVisibility(
            visible = currentGroup.hasSubTypes,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Space.lg, vertical = AppDimens.Space.sm),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space.sm)
            ) {
                currentGroup.subTypes.forEach { subType ->
                    FilterChip(
                        selected = currentType == subType,
                        onClick = { onTypeChange(subType) },
                        label = {
                            Text(
                                text = subType.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        shape = RoundedCornerShape(AppDimens.Radius.full)
                    )
                }
            }
        }
    }
}

/**
 * 完全融合的无边界明细列表项（置于一个父颜色容器中）
 */
@Composable
internal fun StatisticsDetailRow(
    label: String,
    count: Int,
    percentage: Float,
    maxCount: Int,
    color: Color,
    onClick: () -> Unit
) {
    // 列表进场动画 (进度条)
    val widthProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        widthProgress.animateTo(
            targetValue = count.toFloat() / maxCount.coerceAtLeast(1).toFloat(),
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.Space.lg, vertical = 14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )

                Spacer(modifier = Modifier.width(AppDimens.Space.md))

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = String.format("%.1f%%", percentage),
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 替代原生的细 LinearProgressIndicator 为更现代的圆滑大粗条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(AppDimens.Radius.full))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(widthProgress.value)
                        .clip(RoundedCornerShape(AppDimens.Radius.full))
                        .background(color)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${count}个关联应用",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/**
 * 纯平空态卡片
 */
@Composable
internal fun StatsMessagePanel(
    text: String,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Radius.xl))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(AppDimens.Space.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(AppDimens.Space.md))
                Text(
                    text = "重新加载",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppDimens.Radius.sm))
                        .clickable(onClick = onRetry)
                        .padding(AppDimens.Space.xs)
                )
            }
        }
    }
}

/**
 * 应用列表项 — ListItem 风格扁平行
 */
@Composable
internal fun StatisticsAppListItem(
    app: info.muge.appshare.items.AppItem,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isSystemApp = remember { app.isRedMarked() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Radius.lg))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(AppDimens.Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        androidx.compose.foundation.Image(
            painter = coil3.compose.rememberAsyncImagePainter(app.getIcon()),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(AppDimens.Radius.md))
        )

        Spacer(modifier = Modifier.width(AppDimens.Space.md))

        // 主内容区
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.getAppName(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isSystemApp) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppDimens.Radius.full))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "系统",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = app.getPackageName(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(AppDimens.Space.md))

        // 大小 — 干净的右侧文字
        Text(
            text = android.text.format.Formatter.formatFileSize(context, app.getSize()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}
