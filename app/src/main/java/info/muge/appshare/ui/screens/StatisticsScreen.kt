package info.muge.appshare.ui.screens

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import info.muge.appshare.Global
import info.muge.appshare.R
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.dialogs.AppBottomSheet
import info.muge.appshare.ui.theme.AppDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 统计类型
 */
enum class StatisticsType(val label: String) {
    TARGET_SDK("Target SDK"),
    MIN_SDK("Min SDK"),
    COMPILE_SDK("Compile SDK"),
    KOTLIN("Kotlin"),
    ABI("ABI"),
    PAGE_SIZE_16K("16K"),
    APP_BUNDLE("Bundle"),
    INSTALLER("Installer"),
    APP_TYPE("App Type")
}

/**
 * 统计页面 - 完全匹配原 StatisticsFragment 实现，纯 Compose 图表
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(
    onNavigateToAppDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentType by remember { mutableStateOf(StatisticsType.TARGET_SDK) }
    var isPieChart by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var lastUpdatedAt by remember { mutableStateOf<Long?>(null) }
    var displayLimit by remember { mutableIntStateOf(10) } // 0 表示全部
    var hideSingleItems by remember { mutableStateOf(false) }
    var loadJob by remember { mutableStateOf<Job?>(null) }

    val statisticsCache = remember { mutableStateMapOf<StatisticsType, Map<String, List<AppItem>>>() }
    var currentData by remember { mutableStateOf<Map<String, List<AppItem>>>(emptyMap()) }

    // Bottom sheet 状态
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedLabel by remember { mutableStateOf("") }
    var selectedApps by remember { mutableStateOf<List<AppItem>>(emptyList()) }

    val colorScheme = MaterialTheme.colorScheme
    // MD3 配色：优先使用主题色系，兼容动态取色
    val chartColors = remember(colorScheme) {
        listOf(
            colorScheme.primary,
            colorScheme.tertiary,
            colorScheme.secondary,
            colorScheme.primaryContainer,
            colorScheme.secondaryContainer,
            colorScheme.tertiaryContainer,
            colorScheme.inversePrimary,
            colorScheme.error,
            colorScheme.outline,
            colorScheme.primary.copy(alpha = 0.75f),
            colorScheme.tertiary.copy(alpha = 0.75f),
            colorScheme.secondary.copy(alpha = 0.75f)
        )
    }

    // 加载统计数据
    fun loadStatistics(type: StatisticsType, forceRefresh: Boolean = false) {
        if (!forceRefresh && statisticsCache.containsKey(type)) {
            currentData = statisticsCache[type] ?: emptyMap()
            return
        }

        loadJob?.cancel()
        loadJob = coroutineScope.launch {
            isRefreshing = true
            try {
                val appList = withContext(Dispatchers.Default) {
                    synchronized(Global.app_list) {
                        Global.app_list.toList()
                    }
                }
                val statistics = withContext(Dispatchers.Default) {
                    collectStatistics(type, appList)
                }

                statisticsCache[type] = statistics
                if (currentType == type) {
                    currentData = statistics
                }
                lastUpdatedAt = System.currentTimeMillis()
            } finally {
                if (currentType == type) {
                    isRefreshing = false
                }
            }
        }
    }

    // 显示应用列表
    fun showAppListForLabel(label: String) {
        val apps = statisticsCache[currentType]?.get(label) ?: emptyList()
        if (apps.isEmpty()) {
            Toast.makeText(context, "暂无应用", Toast.LENGTH_SHORT).show()
            return
        }
        selectedLabel = label
        selectedApps = apps
        showBottomSheet = true
    }

    // 初始化加载数据
    LaunchedEffect(Unit) {
        loadStatistics(currentType, forceRefresh = true)
    }

    // 切换类型时加载数据
    LaunchedEffect(currentType) {
        loadStatistics(currentType)
    }

    DisposableEffect(Unit) {
        onDispose {
            loadJob?.cancel()
        }
    }

    val sortedEntries = remember(currentData) {
        currentData.entries.sortedByDescending { it.value.size }
    }
    val visibleEntries = remember(sortedEntries, hideSingleItems, displayLimit) {
        val base = if (hideSingleItems) sortedEntries.filter { it.value.size > 1 } else sortedEntries
        if (displayLimit == 0) base else base.take(displayLimit)
    }
    val chartData = remember(visibleEntries) {
        buildMap<String, List<AppItem>> {
            visibleEntries.forEach { put(it.key, it.value) }
        }
    }
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            horizontal = AppDimens.Space.lg,
            vertical = AppDimens.Space.md
        ),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Space.sm),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        item {
            SummaryCard(
                totalCount = currentData.values.sumOf { it.size },
                categoryCount = currentData.size,
                topCategory = sortedEntries.firstOrNull()?.key,
                topCategoryCount = sortedEntries.firstOrNull()?.value?.size ?: 0,
                lastUpdatedAt = lastUpdatedAt,
                isRefreshing = isRefreshing,
                onRefresh = { loadStatistics(currentType, forceRefresh = true) }
            )
        }
        item {
            StatisticsFilterPanel(
                currentType = currentType,
                displayLimit = displayLimit,
                hideSingleItems = hideSingleItems,
                onTypeChange = { currentType = it },
                onLimitChange = { displayLimit = it },
                onToggleHideSingleItems = { hideSingleItems = !hideSingleItems }
            )
        }

        item {
            ChartCard(
                data = chartData,
                isPieChart = isPieChart,
                chartColors = chartColors,
                isRefreshing = isRefreshing,
                onChartToggle = { isPieChart = !isPieChart },
                onSliceClick = { label -> showAppListForLabel(label) }
            )
        }

        item {
            Text(
                text = "详细数据 (${visibleEntries.size}/${sortedEntries.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Space.xs, vertical = AppDimens.Space.xs)
            )
        }

        if (visibleEntries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.none)
                ) {
                    Text(
                        text = "当前筛选条件下没有数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(AppDimens.Space.lg),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val maxCount = visibleEntries.maxOfOrNull { it.value.size } ?: 1
            val total = visibleEntries.sumOf { it.value.size }.toFloat()

            items(visibleEntries.size) { index ->
                val entry = visibleEntries[index]
                val color = chartColors[index % chartColors.size]
                val percentage = if (total > 0) (entry.value.size / total) * 100f else 0f

                StatisticsDetailItem(
                    label = entry.key,
                    count = entry.value.size,
                    percentage = percentage,
                    color = color,
                    maxCount = maxCount,
                    onClick = { showAppListForLabel(entry.key) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(AppDimens.Space.xs)) }
    }

    // 应用列表底部弹窗
    if (showBottomSheet) {
        StatisticsAppListBottomSheet(
            title = selectedLabel,
            apps = selectedApps,
            onDismiss = { showBottomSheet = false },
            onAppClick = { app ->
                showBottomSheet = false
                onNavigateToAppDetail(app.getPackageName())
            }
        )
    }
}

/**
 * 概览卡片
 */
@Composable
private fun StatisticsFilterPanel(
    currentType: StatisticsType,
    displayLimit: Int,
    hideSingleItems: Boolean,
    onTypeChange: (StatisticsType) -> Unit,
    onLimitChange: (Int) -> Unit,
    onToggleHideSingleItems: () -> Unit
) {
    StatsPanel(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(
            text = "统计维度",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(AppDimens.Space.md))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space.sm),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space.sm)
        ) {
            StatisticsType.values().forEach { type ->
                FilterChip(
                    selected = currentType == type,
                    onClick = { onTypeChange(type) },
                    label = { Text(type.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.Space.md))

        Text(
            text = "显示范围",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(AppDimens.Space.sm))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space.sm),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space.sm)
        ) {
            FilterChip(
                selected = displayLimit == 5,
                onClick = { onLimitChange(5) },
                label = { Text("Top 5") }
            )
            FilterChip(
                selected = displayLimit == 10,
                onClick = { onLimitChange(10) },
                label = { Text("Top 10") }
            )
            FilterChip(
                selected = displayLimit == 0,
                onClick = { onLimitChange(0) },
                label = { Text("全部") }
            )
            FilterChip(
                selected = hideSingleItems,
                onClick = onToggleHideSingleItems,
                label = { Text("隐藏仅 1 个") }
            )
        }
    }
}

@Composable
private fun StatsPanel(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.none)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = AppDimens.Space.lg, vertical = AppDimens.Space.md),
            content = content
        )
    }
}

/**
 * 概览卡片
 */
@Composable
private fun SummaryCard(
    totalCount: Int,
    categoryCount: Int,
    topCategory: String?,
    topCategoryCount: Int,
    lastUpdatedAt: Long?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    StatsPanel(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "统计概览",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = totalCount.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "应用总数",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "分类: $categoryCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = AppDimens.Space.sm)
                )
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新统计",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!topCategory.isNullOrBlank()) {
                Text(
                    text = "最多: $topCategory ($topCategoryCount)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = lastUpdatedAt?.let { "更新于 ${formatTime(it)}" } ?: "尚未更新",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } 
}

/**
 * 图表卡片 - 纯 Compose 实现
 */
@Composable
private fun ChartCard(
    data: Map<String, List<AppItem>>,
    isPieChart: Boolean,
    chartColors: List<Color>,
    isRefreshing: Boolean,
    onChartToggle: () -> Unit,
    onSliceClick: (String) -> Unit
) {
    StatsPanel(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (data.isEmpty()) {
                // 空状态
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_rule),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                    Spacer(modifier = Modifier.height(AppDimens.Space.lg))
                    Text(
                        text = "暂无数据",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 图表容器
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp)
                ) {
                    if (isPieChart) {
                        ComposePieChart(
                            data = data,
                            chartColors = chartColors,
                            onSliceClick = onSliceClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        )
                    } else {
                        ComposeBarChart(
                            data = data,
                            chartColors = chartColors,
                            onBarClick = onSliceClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        )
                    }
                }
            }

            // 图表切换按钮 - 右上角
            IconButton(
                onClick = onChartToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = AppDimens.Space.xs, end = AppDimens.Space.xs)
            ) {
                Icon(
                    imageVector = if (isPieChart) Icons.Default.BarChart else Icons.Default.PieChart,
                    contentDescription = "切换图表类型",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    } 
}

/**
 * 纯 Compose 饼状图
 */
@Composable
private fun ComposePieChart(
    data: Map<String, List<AppItem>>,
    chartColors: List<Color>,
    onSliceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedEntries = remember(data) {
        data.entries.sortedByDescending { it.value.size }
    }
    val totalCount = remember(sortedEntries) {
        sortedEntries.sumOf { it.value.size }
    }
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sortedEntries, totalCount) {
                    detectTapGestures { tapOffset ->
                        if (totalCount <= 0) return@detectTapGestures

                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = tapOffset.x - centerX
                        val dy = tapOffset.y - centerY
                        val distance = sqrt(dx * dx + dy * dy)
                        val outerRadius = (minOf(size.width, size.height).toFloat() / 2f) * 0.85f
                        val innerRadius = outerRadius * 0.55f

                        if (distance !in innerRadius..outerRadius) return@detectTapGestures

                        val angle = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 450.0) % 360.0).toFloat()
                        var startAngle = 0f
                        sortedEntries.forEach { entry ->
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
            val radius = size.minDimension / 2 * 0.85f
            val center = Offset(size.width / 2, size.height / 2)

            sortedEntries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value.size.toFloat() / totalCount.toFloat()) * 360f
                val color = chartColors[index % chartColors.size]

                // 绘制扇形
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
                text = totalCount.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    // 图例
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppDimens.Space.lg)
    ) {
        sortedEntries.take(6).forEachIndexed { index, entry ->
            val color = chartColors[index % chartColors.size]
            val percentage = (entry.value.size.toFloat() / totalCount.toFloat()) * 100f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSliceClick(entry.key) }
                    .padding(vertical = AppDimens.Space.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 纯 Compose 条形图
 */
@Composable
private fun ComposeBarChart(
    data: Map<String, List<AppItem>>,
    chartColors: List<Color>,
    onBarClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedEntries = remember(data) {
        data.entries.sortedByDescending { it.value.size }.take(10)
    }
    val maxCount = remember(sortedEntries) {
        sortedEntries.maxOfOrNull { it.value.size }?.toFloat() ?: 0f
    }

    Column(
        modifier = modifier
    ) {
        sortedEntries.forEachIndexed { index, entry ->
            val color = chartColors[index % chartColors.size]
            val percentage = if (maxCount > 0) entry.value.size.toFloat() / maxCount else 0f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBarClick(entry.key) }
                    .padding(vertical = AppDimens.Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标签
                Text(
                    text = entry.key,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(84.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(AppDimens.Space.sm))

                // 条形
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(AppDimens.Radius.xs))
                        .background(color.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(percentage)
                            .clip(RoundedCornerShape(AppDimens.Radius.xs))
                            .background(color)
                    )
                }

                Spacer(modifier = Modifier.width(AppDimens.Space.sm))

                // 数值
                Text(
                    text = entry.value.size.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * 统计详情列表项
 */
@Composable
private fun StatisticsDetailItem(
    label: String,
    count: Int,
    percentage: Float,
    color: Color,
    maxCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.none),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Space.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 颜色指示点
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )

                Spacer(modifier = Modifier.width(AppDimens.Space.md))

                // 标签
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // 百分比
                Text(
                    text = String.format("%.1f%%", percentage),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.Space.md))

            // 进度条
            LinearProgressIndicator(
                progress = { count.toFloat() / maxCount.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )

            Spacer(modifier = Modifier.height(AppDimens.Space.sm))

            // 数量
            Text(
                text = "$count 个应用",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/**
 * 统计应用列表底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatisticsAppListBottomSheet(
    title: String,
    apps: List<AppItem>,
    onDismiss: () -> Unit,
    onAppClick: (AppItem) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val sortedApps = remember(apps) { apps.sortedByDescending { it.getSize() } }
    AppBottomSheet(
        title = title,
        onDismiss = onDismiss,
        content = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = configuration.screenHeightDp.dp * 0.92f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimens.Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "共 ${apps.size} 个",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = AppDimens.Space.xs),
                contentPadding = PaddingValues(vertical = AppDimens.Space.xs),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Space.xs)
            ) {
                items(sortedApps) { app ->
                    AppListItem(
                        app = app,
                        onClick = { onAppClick(app) }
                    )
                }
            }
        }
        }
    )
}

/**
 * 应用列表项
 */
@Composable
private fun AppListItem(
    app: AppItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isSystemApp = remember { app.isRedMarked() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.none),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 应用图标
            androidx.compose.foundation.Image(
                painter = rememberAsyncImagePainter(app.getIcon()),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(AppDimens.Radius.md))
            )

            Spacer(modifier = Modifier.width(AppDimens.Space.md))

            // 文本信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.getAppName(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSystemApp) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(AppDimens.Space.xs))

                Text(
                    text = app.getPackageName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // 应用大小
            Text(
                text = Formatter.formatFileSize(context, app.getSize()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 收集统计数据
 */
private fun collectStatistics(
    type: StatisticsType,
    appList: List<AppItem>
): Map<String, List<AppItem>> {
    val result = mutableMapOf<String, MutableList<AppItem>>()

    appList.forEach { app ->
        try {
            val packageInfo = app.getPackageInfo()
            val appInfo = packageInfo.applicationInfo ?: return@forEach

            val key = when (type) {
                StatisticsType.TARGET_SDK -> "API ${appInfo.targetSdkVersion}"
                StatisticsType.MIN_SDK -> "API ${appInfo.minSdkVersion}"
                StatisticsType.COMPILE_SDK -> getCompileSdkLabel(packageInfo)
                StatisticsType.KOTLIN -> if (hasKotlinClasses(app)) "有 Kotlin" else "无 Kotlin"
                StatisticsType.ABI -> getAbiLabel(app)
                StatisticsType.PAGE_SIZE_16K -> if (is16kPageSize(app)) "可能支持 16K" else "可能不支持 16K"
                StatisticsType.APP_BUNDLE -> if (isAppBundle(app)) "Split APK / Bundle" else "单 APK"
                StatisticsType.INSTALLER -> app.getInstallSource().ifBlank { "未知来源" }
                StatisticsType.APP_TYPE -> if (app.isRedMarked()) "系统应用" else "用户应用"
            }

            result.getOrPut(key) { mutableListOf() }.add(app)
        } catch (e: Exception) {
            // 忽略错误
        }
    }

    return result
}

private fun hasKotlinClasses(app: AppItem): Boolean {
    return try {
        val paths = buildList {
            add(app.getSourcePath())
            app.getSplitSourceDirs()?.let { addAll(it) }
        }
        paths.any(::containsKotlinMetadata)
    } catch (e: Exception) {
        false
    }
}

private fun getAbis(app: AppItem): List<String> {
    return try {
        val abiSet = linkedSetOf<String>()
        val paths = buildList {
            add(app.getSourcePath())
            app.getSplitSourceDirs()?.let { addAll(it) }
        }
        paths.forEach { path ->
            ZipFile(path).use { zip ->
                zip.entries().asSequence()
                    .map { it.name }
                    .filter { it.startsWith("lib/") && it.count { ch -> ch == '/' } >= 2 }
                    .forEach { entryName ->
                        val abi = entryName.substringAfter("lib/").substringBefore('/')
                        if (abi.isNotBlank()) {
                            abiSet.add(abi)
                        }
                    }
            }
        }
        abiSet.toList()
    } catch (e: Exception) {
        emptyList()
    }
}

private fun is16kPageSize(app: AppItem): Boolean {
    val abis = getAbis(app)
    return abis.isEmpty() || "arm64-v8a" in abis
}

private fun isAppBundle(app: AppItem): Boolean {
    return try {
        app.getSplitSourceDirs()?.isNotEmpty() == true ||
            app.getSourcePath().contains("split_config")
    } catch (e: Exception) {
        false
    }
}

private fun getCompileSdkLabel(packageInfo: android.content.pm.PackageInfo): String {
    val sdk = runCatching {
        android.content.pm.PackageInfo::class.java
            .getField("compileSdkVersion")
            .getInt(packageInfo)
    }.getOrDefault(-1)
    return if (sdk > 0) "API $sdk" else "未知"
}

private fun getAbiLabel(app: AppItem): String {
    val abis = getAbis(app)
    if (abis.isEmpty()) return "无 Native"
    return abis.joinToString(" + ")
}

private fun containsKotlinMetadata(apkPath: String): Boolean {
    return runCatching {
        ZipFile(apkPath).use { zip ->
            zip.entries().asSequence().any { entry ->
                val name = entry.name
                name.endsWith(".kotlin_module") ||
                    name.contains("kotlin", ignoreCase = true) && (
                    name.startsWith("META-INF/") ||
                        name.startsWith("assets/")
                    )
            }
        }
    }.getOrDefault(false)
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
