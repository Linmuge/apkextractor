package info.muge.appshare.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import info.muge.appshare.Global
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.screens.statistics.ChartCard
import info.muge.appshare.ui.screens.statistics.ChartMode
import info.muge.appshare.ui.screens.statistics.ExportStatsCard
import info.muge.appshare.ui.screens.statistics.InstallTrendChart
import info.muge.appshare.ui.screens.statistics.SplitApkOverviewCard
import info.muge.appshare.ui.screens.statistics.StatisticsAppListBottomSheet
import info.muge.appshare.ui.screens.statistics.StatisticsDetailRow
import info.muge.appshare.ui.screens.statistics.StatisticsTabRow
import info.muge.appshare.ui.screens.statistics.StatsMessagePanel
import info.muge.appshare.ui.screens.statistics.StorageOverviewCard
import info.muge.appshare.ui.screens.statistics.SummaryCard
import info.muge.appshare.R
import info.muge.appshare.ui.theme.AppDimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 统计类型
 */
enum class StatisticsType(val label: String, val labelResId: Int? = null) {
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
    INSTALL_TIME("Install"),
    EXPORT_STATS("导出", R.string.stats_tab_export),
    PERMISSION("权限", R.string.stats_tab_permission)
}

enum class DetailSortMode(val label: String, val labelResId: Int? = null) {
    COUNT_DESC("按数量", R.string.stats_sort_by_count),
    LABEL_ASC("按名称", R.string.stats_sort_by_name)
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
    val scope = rememberCoroutineScope()

    // 设置 Context 用于权限查询
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.setContext(context)
    }

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

            // Split APK 概览卡片（APP_BUNDLE Tab）
            if (uiState.currentType == StatisticsType.APP_BUNDLE) {
                item {
                    SplitApkOverviewCard(
                        stats = uiState.splitApkStats
                    )
                }
            }

            // 存储分析概览卡片（SIZE_DISTRIBUTION Tab）
            if (uiState.currentType == StatisticsType.SIZE_DISTRIBUTION) {
                item {
                    StorageOverviewCard(
                        overview = uiState.storageOverview
                    )
                }
            }

            // 安装趋势折线图（INSTALL_TIME Tab）
            if (uiState.currentType == StatisticsType.INSTALL_TIME && uiState.installTrend.isNotEmpty()) {
                item {
                    InstallTrendChart(
                        data = uiState.installTrend,
                        chartColors = chartColors
                    )
                }
            }

            // 导出统计卡片（EXPORT_STATS Tab）
            if (uiState.currentType == StatisticsType.EXPORT_STATS) {
                item {
                    ExportStatsCard()
                }
            }

            if (uiState.currentType != StatisticsType.EXPORT_STATS) {
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
            }

            item {
                val context = LocalContext.current
                Text(
                    text = "${context.getString(R.string.stats_detail_data)} (${visibleEntries.size}/${sortedEntries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
            }

            if (visibleEntries.isEmpty()) {
                item {
                    val context = LocalContext.current
                    StatsMessagePanel(
                        text = uiState.loadError?.let { "${context.getString(R.string.stats_load_failed)}：$it" }
                            ?: context.getString(R.string.stats_no_data),
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
                            val context = LocalContext.current
                            Text(
                                text = "${context.getString(R.string.stats_partial_data_error)}：${uiState.loadError}",
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
        AnimatedVisibility(
            visible = uiState.isRefreshing,
            enter = fadeIn(),
            exit = fadeOut(),
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
