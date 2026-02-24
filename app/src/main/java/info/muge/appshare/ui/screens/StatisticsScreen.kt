package info.muge.appshare.ui.screens

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import info.muge.appshare.Global
import info.muge.appshare.R
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.dialogs.DialogStyles
import info.muge.appshare.ui.theme.AppDimens
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    APP_TYPE("App Type"),
    SIZE_DISTRIBUTION("Size"),
    INSTALL_TIME("Install")
}

private enum class ChartMode {
    PIE,
    BAR
}

enum class DetailSortMode(val label: String) {
    COUNT_DESC("按数量"),
    LABEL_ASC("按名称")
}

private enum class SheetSortMode {
    SIZE_DESC,
    SIZE_ASC,
    NAME_ASC
}

/**
 * 统计页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = viewModel(),
    onNavigateToAppDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 当应用列表加载完成后自动刷新
    val appListSize = remember { mutableIntStateOf(Global.app_list.size) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500L)
            val currentSize = Global.app_list.size
            if (currentSize != appListSize.intValue) {
                appListSize.intValue = currentSize
                if (currentSize > 0) {
                    viewModel.onEvent(StatisticsEvent.Refresh)
                }
            }
        }
    }

    var chartMode by remember { mutableStateOf(ChartMode.PIE) }
    var showConfigMenu by remember { mutableStateOf(false) }

    // Bottom sheet 状态
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedLabel by remember { mutableStateOf("") }
    var selectedApps by remember { mutableStateOf<List<AppItem>>(emptyList()) }

    val colorScheme = MaterialTheme.colorScheme
    val chartColors = remember(colorScheme) {
        listOf(
            colorScheme.primary,
            colorScheme.tertiary,
            colorScheme.secondary,
            colorScheme.error,
            colorScheme.primaryContainer,
            colorScheme.secondaryContainer,
            colorScheme.tertiaryContainer,
            colorScheme.errorContainer,
            colorScheme.outline,
            colorScheme.primary.copy(alpha = 0.5f),
            colorScheme.secondary.copy(alpha = 0.5f),
            colorScheme.tertiary.copy(alpha = 0.5f)
        )
    }

    fun showAppList(label: String) {
        val apps = uiState.currentData[label].orEmpty()
        if (apps.isEmpty()) return
        selectedLabel = label
        selectedApps = apps
        showBottomSheet = true
    }

    val sortedEntries = remember(uiState.currentData, uiState.detailSortMode) {
        val entries = uiState.currentData.entries.toList()
        when (uiState.detailSortMode) {
            DetailSortMode.COUNT_DESC -> entries.sortedByDescending { it.value.size }
            DetailSortMode.LABEL_ASC -> entries.sortedBy { it.key.lowercase(Locale.getDefault()) }
        }
    }

    val visibleEntries = remember(sortedEntries, uiState.displayLimit, uiState.hideSingleItems) {
        val filtered = if (uiState.hideSingleItems) {
            sortedEntries.filter { it.value.size > 1 }
        } else {
            sortedEntries
        }
        if (uiState.displayLimit == 0) filtered else filtered.take(uiState.displayLimit)
    }

    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                horizontal = AppDimens.Space.lg,
                vertical = AppDimens.Space.md
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space.md),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            item {
                SummaryCard(
                    totalCount = uiState.currentData.values.sumOf { it.size },
                    categoryCount = uiState.currentData.size,
                    topCategory = sortedEntries.firstOrNull()?.key,
                    topCategoryCount = sortedEntries.firstOrNull()?.value?.size ?: 0,
                    lastUpdatedAt = uiState.lastUpdatedAt,
                    onRefresh = { viewModel.onEvent(StatisticsEvent.Refresh) },
                    onShowConfig = { showConfigMenu = true },
                    showConfigMenu = showConfigMenu,
                    onDismissConfigMenu = { showConfigMenu = false },
                    uiState = uiState,
                    viewModel = viewModel
                )
            }

            item {
                StatisticsTabRow(
                    currentType = uiState.currentType,
                    onTypeChange = { viewModel.onEvent(StatisticsEvent.ChangeType(it)) }
                )
            }

            item {
                ChartCard(
                    entries = visibleEntries,
                    chartMode = chartMode,
                    chartColors = chartColors,
                    isRefreshing = uiState.isRefreshing,
                    onChartToggle = {
                        chartMode = if (chartMode == ChartMode.PIE) ChartMode.BAR else ChartMode.PIE
                    },
                    onSliceClick = { label -> showAppList(label) }
                )
            }

            item {
                Text(
                    text = "详细数据 (${visibleEntries.size}/${sortedEntries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
            }

            if (visibleEntries.isEmpty()) {
                item {
                    StatsMessagePanel(
                        text = uiState.loadError?.let { "加载失败：$it" } ?: "当前筛选条件下没有数据",
                        onRetry = uiState.loadError?.let { { viewModel.onEvent(StatisticsEvent.Refresh) } }
                    )
                }
            } else {
                item {
                    val maxCount = visibleEntries.maxOfOrNull { it.value.size } ?: 1
                    val total = visibleEntries.sumOf { it.value.size }.toFloat().coerceAtLeast(1f)

                    // 去卡片化：整体放在一个容器中
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDimens.Radius.xl))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(vertical = AppDimens.Space.sm)
                    ) {
                        if (uiState.loadError != null) {
                            Text(
                                text = "部分数据可能未更新：${uiState.loadError}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppDimens.Space.lg)
                                    .clickable { viewModel.onEvent(StatisticsEvent.Refresh) }
                            )
                        }

                        visibleEntries.forEachIndexed { index, entry ->
                            val color = chartColors[index % chartColors.size]
                            StatisticsDetailRow(
                                label = entry.key,
                                count = entry.value.size,
                                percentage = (entry.value.size / total) * 100f,
                                maxCount = maxCount,
                                color = color,
                                onClick = { showAppList(entry.key) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        // 半透明加载遮罩层
        androidx.compose.animation.AnimatedVisibility(
            visible = uiState.isRefreshing,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .pointerInput(uiState.isRefreshing) { },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppDimens.Radius.xl))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(AppDimens.Space.xl)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }
            }
        }
    }

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
 * 纯平扁平化概览卡片，使用主题色容器组合
 */
@Composable
private fun SummaryCard(
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
 * 现代化扁平 Tab 设计
 */
@Composable
private fun StatisticsTabRow(
    currentType: StatisticsType,
    onTypeChange: (StatisticsType) -> Unit
) {
    val selectedTabIndex = StatisticsType.entries.indexOf(currentType)

    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 0.dp,
        divider = {}, // 无底部分割线
        indicator = { tabPositions ->
            // 自定义极简底部指示器
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[selectedTabIndex])
                    .height(3.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    ) {
        StatisticsType.entries.forEachIndexed { index, type ->
            val selected = selectedTabIndex == index
            Tab(
                selected = selected,
                onClick = { onTypeChange(type) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

/**
 * 存放饼图和柱状图的容器（完全去阴影扁平容器）
 */
@Composable
private fun ChartCard(
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

                // 原有条形图的底色背景在此去掉，显得更加纯粹；直接画有进度的部分
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

/**
 * 完全融合的无边界明细列表项（置于一个父颜色容器中）
 */
@Composable
private fun StatisticsDetailRow(
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
private fun StatsMessagePanel(
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun StatisticsAppListBottomSheet(
    title: String,
    apps: List<AppItem>,
    onDismiss: () -> Unit,
    onAppClick: (AppItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val appListState = rememberLazyListState()

    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SheetSortMode.SIZE_DESC) }

    val filteredApps = remember(apps, query, sortMode) {
        val queryLower = query.trim().lowercase(Locale.getDefault())
        val filtered = if (queryLower.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.getAppName().lowercase(Locale.getDefault()).contains(queryLower) ||
                    it.getPackageName().lowercase(Locale.getDefault()).contains(queryLower)
            }
        }

        when (sortMode) {
            SheetSortMode.SIZE_DESC -> filtered.sortedByDescending { it.getSize() }
            SheetSortMode.SIZE_ASC -> filtered.sortedBy { it.getSize() }
            SheetSortMode.NAME_ASC -> filtered.sortedBy { it.getAppName().lowercase(Locale.getDefault()) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = true,
        shape = DialogStyles.getBottomSheetShape(sheetState = sheetState),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = AppDimens.Space.md, bottom = AppDimens.Space.xs)
                    .size(width = 48.dp, height = 4.dp)
                    .clip(RoundedCornerShape(AppDimens.Radius.full))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 固定顶部：标题 + 搜索 + 排序
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Space.lg)
            ) {
                // 标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppDimens.Radius.full))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                    ) {
                        Text(
                            text = "${filteredApps.size} 个",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.Space.md))

                // 搜索框
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    placeholder = {
                        Text(
                            "搜索应用名称或包名",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(AppDimens.Radius.xl)
                )

                Spacer(modifier = Modifier.height(AppDimens.Space.sm))

                // 排序选项
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Space.sm)
                ) {
                    FilterChip(
                        selected = sortMode == SheetSortMode.SIZE_DESC,
                        onClick = { sortMode = SheetSortMode.SIZE_DESC },
                        label = { Text("大小↓", style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(AppDimens.Radius.full)
                    )
                    FilterChip(
                        selected = sortMode == SheetSortMode.SIZE_ASC,
                        onClick = { sortMode = SheetSortMode.SIZE_ASC },
                        label = { Text("大小↑", style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(AppDimens.Radius.full)
                    )
                    FilterChip(
                        selected = sortMode == SheetSortMode.NAME_ASC,
                        onClick = { sortMode = SheetSortMode.NAME_ASC },
                        label = { Text("名称", style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(AppDimens.Radius.full)
                    )
                }

                Spacer(modifier = Modifier.height(AppDimens.Space.sm))
            }

            // 可滚动应用列表
            LazyColumn(
                state = appListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = AppDimens.Space.lg,
                    end = AppDimens.Space.lg,
                    bottom = AppDimens.Space.xl
                ),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Space.sm),
                overscrollEffect = null
            ) {
                if (filteredApps.isEmpty()) {
                    item {
                        Text(
                            text = if (query.isBlank()) "暂无应用数据" else "没有匹配的应用",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(filteredApps, key = { it.getPackageName() }) { app ->
                        StatisticsAppListItem(
                            app = app,
                            onClick = { onAppClick(app) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 应用列表项 — ListItem 风格扁平行
 */
@Composable
private fun StatisticsAppListItem(
    app: AppItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
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
            painter = rememberAsyncImagePainter(app.getIcon()),
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
            text = Formatter.formatFileSize(context, app.getSize()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
