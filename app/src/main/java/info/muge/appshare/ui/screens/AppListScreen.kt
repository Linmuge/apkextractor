package info.muge.appshare.ui.screens

import android.content.Context
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import info.muge.appshare.Constants
import info.muge.appshare.Global
import info.muge.appshare.R
import info.muge.appshare.items.AppItem
import info.muge.appshare.tasks.RefreshInstalledListTask
import info.muge.appshare.tasks.SearchAppItemTask
import info.muge.appshare.ui.ToastManager
import info.muge.appshare.ui.dialogs.SortConfigDialog
import info.muge.appshare.utils.PermissionExts
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.StorageUtil
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.Locale

/**
 * 应用列表页面 - 1:1 匹配原始 AppFragment 实现
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppListScreen(
    viewMode: Int = 0,
    isSearchMode: Boolean = false,
    searchText: String = "",
    isMultiSelectMode: Boolean = false,
    onMultiSelectModeChange: (Boolean) -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToDetailWithUri: (android.net.Uri) -> Unit = {},
    showSortDialog: Boolean = false,
    onSortDialogDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 应用列表状态
    var appList by remember { mutableStateOf<List<AppItem>>(emptyList()) }

    // 加载状态
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0 to 0) }

    // 显示系统应用
    var showSystemApp by remember {
        mutableStateOf(
            SPUtil.getGlobalSharedPreferences(context).getBoolean(
                Constants.PREFERENCE_SHOW_SYSTEM_APP,
                Constants.PREFERENCE_SHOW_SYSTEM_APP_DEFAULT
            )
        )
    }
    var isShowSystemAppEnabled by remember { mutableStateOf(true) }

    // 权限状态
    var hasPermission by remember {
        mutableStateOf(
            SPUtil.getGlobalSharedPreferences(context).getBoolean("show_app", false)
        )
    }

    // 多选状态
    val selectedItems = remember { mutableStateListOf<String>() }
    var selectedSize by remember { mutableLongStateOf(0L) }

    // 高亮关键词
    var highlightKeyword by remember { mutableStateOf<String?>(null) }

    // 可用存储空间
    var availableStorage by remember { mutableStateOf("") }

    // 滚动状态 - 与原实现一致
    var isScrollable by remember { mutableStateOf(false) }
    var isBottomCardVisible by remember { mutableStateOf(true) }
    var isMultiSelectCardVisible by remember { mutableStateOf(false) }

    // 刷新任务
    var refreshTask by remember { mutableStateOf<RefreshInstalledListTask?>(null) }
    var searchTask by remember { mutableStateOf<SearchAppItemTask?>(null) }

    // 刷新应用列表
    fun refreshAppList() {
        if (!hasPermission) return

        refreshTask?.setInterrupted()
        isLoading = true
        loadingProgress = 0 to 0
        isScrollable = false
        appList = emptyList()
        isShowSystemAppEnabled = false

        refreshTask = RefreshInstalledListTask(context, object : RefreshInstalledListTask.RefreshInstalledListTaskCallback {
            override fun onRefreshProgressStarted(total: Int) {
                loadingProgress = 0 to total
            }

            override fun onRefreshProgressUpdated(current: Int, total: Int) {
                loadingProgress = current to total
            }

            override fun onRefreshCompleted(list: List<AppItem>) {
                isLoading = false
                isRefreshing = false
                appList = list
                isShowSystemAppEnabled = true
            }
        })
        refreshTask?.start()
    }

    // 搜索功能
    LaunchedEffect(isSearchMode, searchText) {
        if (isSearchMode && searchText.isNotEmpty()) {
            searchTask?.setInterrupted()
            isRefreshing = true
            highlightKeyword = searchText
            appList = emptyList()

            searchTask = SearchAppItemTask(
                Global.app_list.toList(),
                searchText,
                object : SearchAppItemTask.SearchTaskCompletedCallback {
                    override fun onSearchTaskCompleted(appItems: List<AppItem>, keyword: String) {
                        isRefreshing = false
                        appList = appItems
                    }
                }
            )
            searchTask?.start()
        } else if (!isSearchMode) {
            highlightKeyword = null
            appList = Global.app_list.toList()
        }
    }

    // 初始化
    LaunchedEffect(Unit) {
        refreshAvailableStorage(context) { availableStorage = it }
    }

    // 权限授予后刷新
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            refreshAppList()
        }
    }

    // 监听系统应用开关 - 与原实现一致，先禁用checkbox再刷新
    LaunchedEffect(showSystemApp) {
        if (hasPermission) {
            isShowSystemAppEnabled = false
            SPUtil.getGlobalSharedPreferences(context)
                .edit()
                .putBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP, showSystemApp)
                .apply()
            refreshAppList()
        }
    }

    // 多选模式状态变化
    LaunchedEffect(isMultiSelectMode) {
        if (isMultiSelectMode) {
            isBottomCardVisible = false
            isMultiSelectCardVisible = true
        } else {
            isMultiSelectCardVisible = false
            isBottomCardVisible = !isSearchMode
            selectedItems.clear()
            selectedSize = 0
        }
    }

    // 搜索模式状态变化
    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            isBottomCardVisible = false
            isMultiSelectCardVisible = false
            if (isMultiSelectMode) {
                onMultiSelectModeChange(false)
            }
        } else {
            isBottomCardVisible = !isMultiSelectMode
            highlightKeyword = null
            appList = Global.app_list.toList()
        }
    }

    // 排序对话框
    if (showSortDialog) {
        SortConfigDialog(
            onDismiss = onSortDialogDismiss,
            onOptionSelected = { sortValue ->
                AppItem.sort_config = sortValue
                synchronized(Global.app_list) {
                    Collections.sort(Global.app_list)
                }
                appList = Global.app_list.toList()
                onSortDialogDismiss()
            }
        )
    }

    // 滚动监听 - 与原实现完全一致
    val listState = rememberLazyListState()

    // 处理滚动方向变化
    var previousFirstVisibleItemIndex by remember { mutableStateOf(0) }
    var previousFirstVisibleItemScrollOffset by remember { mutableStateOf(0) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset

        // 判断滚动方向
        if (currentIndex > previousFirstVisibleItemIndex ||
            (currentIndex == previousFirstVisibleItemIndex && currentOffset > previousFirstVisibleItemScrollOffset)) {
            // 向下滚动
            if (isScrollable) {
                if (isMultiSelectMode) {
                    if (isMultiSelectCardVisible) {
                        isMultiSelectCardVisible = false
                    }
                } else if (!isSearchMode) {
                    if (isBottomCardVisible) {
                        isBottomCardVisible = false
                    }
                }
            }
            isScrollable = true
        } else if (currentIndex < previousFirstVisibleItemIndex ||
            (currentIndex == previousFirstVisibleItemIndex && currentOffset < previousFirstVisibleItemScrollOffset)) {
            // 向上滚动
            if (isScrollable) {
                if (isMultiSelectMode) {
                    if (!isMultiSelectCardVisible) {
                        isMultiSelectCardVisible = true
                    }
                } else if (!isSearchMode) {
                    if (!isBottomCardVisible) {
                        isBottomCardVisible = true
                    }
                }
            }
        }

        previousFirstVisibleItemIndex = currentIndex
        previousFirstVisibleItemScrollOffset = currentOffset
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 主内容区域
        if (!hasPermission) {
            // 无权限时显示空白背景，底部卡片显示权限请求
        } else if (isLoading && appList.isEmpty()) {
            // 加载中 - 水平进度条（与原实现一致）
            LoadingContent(
                current = loadingProgress.first,
                total = loadingProgress.second
            )
        } else if (appList.isEmpty() && !isSearchMode) {
            // 空内容（与原实现一致）
            EmptyContent()
        } else if (appList.isEmpty() && isSearchMode && searchText.isNotEmpty()) {
            // 搜索中
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // 应用列表
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    if (!isSearchMode && !isMultiSelectMode) {
                        refreshAppList()
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                if (viewMode == 0) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState
                    ) {
                        items(appList, key = { it.getPackageName() }) { app ->
                            LinearAppItem(
                                app = app,
                                isSelected = selectedItems.contains(app.getPackageName()),
                                isMultiSelectMode = isMultiSelectMode,
                                highlightKeyword = highlightKeyword,
                                onClick = {
                                    if (isMultiSelectMode) {
                                        val pkgName = app.getPackageName()
                                        if (selectedItems.contains(pkgName)) {
                                            selectedItems.remove(pkgName)
                                            selectedSize -= app.getSize()
                                        } else {
                                            selectedItems.add(pkgName)
                                            selectedSize += app.getSize()
                                        }
                                        if (selectedItems.isEmpty()) {
                                            onMultiSelectModeChange(false)
                                        }
                                    } else {
                                        onNavigateToDetail(app.getPackageName())
                                    }
                                },
                                onLongClick = {
                                    if (!isSearchMode) {
                                        onMultiSelectModeChange(true)
                                        val pkgName = app.getPackageName()
                                        if (!selectedItems.contains(pkgName)) {
                                            selectedItems.add(pkgName)
                                            selectedSize += app.getSize()
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(appList, key = { it.getPackageName() }) { app ->
                            GridAppItem(
                                app = app,
                                isSelected = selectedItems.contains(app.getPackageName()),
                                onClick = {
                                    if (isMultiSelectMode) {
                                        val pkgName = app.getPackageName()
                                        if (selectedItems.contains(pkgName)) {
                                            selectedItems.remove(pkgName)
                                            selectedSize -= app.getSize()
                                        } else {
                                            selectedItems.add(pkgName)
                                            selectedSize += app.getSize()
                                        }
                                        if (selectedItems.isEmpty()) {
                                            onMultiSelectModeChange(false)
                                        }
                                    } else {
                                        onNavigateToDetail(app.getPackageName())
                                    }
                                },
                                onLongClick = {
                                    if (!isSearchMode) {
                                        onMultiSelectModeChange(true)
                                        val pkgName = app.getPackageName()
                                        if (!selectedItems.contains(pkgName)) {
                                            selectedItems.add(pkgName)
                                            selectedSize += app.getSize()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 底部卡片 - 常规模式
        AnimatedVisibility(
            visible = isBottomCardVisible,
            enter = slideInVertically(animationSpec = tween(300)) { it },
            exit = slideOutVertically(animationSpec = tween(300)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ExportCard(
                hasPermission = hasPermission,
                availableStorage = availableStorage,
                showSystemApp = showSystemApp,
                onShowSystemAppChange = { showSystemApp = it },
                isShowSystemAppEnabled = isShowSystemAppEnabled && !isLoading,
                onPermissionGranted = {
                    hasPermission = true
                    SPUtil.getGlobalSharedPreferences(context)
                        .edit()
                        .putBoolean("show_app", true)
                        .apply()
                    refreshAppList()
                }
            )
        }

        // 底部卡片 - 多选模式
        AnimatedVisibility(
            visible = isMultiSelectCardVisible,
            enter = slideInVertically(animationSpec = tween(300)) { it },
            exit = slideOutVertically(animationSpec = tween(300)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            MultiSelectCard(
                selectedCount = selectedItems.size,
                selectedSize = selectedSize,
                onSelectAll = {
                    if (selectedItems.size == appList.size) {
                        selectedItems.clear()
                        selectedSize = 0
                    } else {
                        selectedItems.clear()
                        selectedSize = 0
                        appList.forEach { app ->
                            selectedItems.add(app.getPackageName())
                            selectedSize += app.getSize()
                        }
                    }
                },
                onCopyPackageNames = {
                    if (selectedItems.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.snack_bar_no_app_selected))
                        }
                        return@MultiSelectCard
                    }
                    val separator = SPUtil.getGlobalSharedPreferences(context)
                        .getString(Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR, ",") ?: ","
                    val resultString = selectedItems.joinToString(separator)

                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("message", resultString))

                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.snack_bar_clipboard))
                    }
                }
            )
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(snackbarData = data)
        }
    }
}

/**
 * 加载中内容 - MD3 风格
 */
@Composable
private fun LoadingContent(
    current: Int,
    total: Int
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 进度指示器容器
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 0.dp
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                // 圆形进度背景
                CircularProgressIndicator(
                    progress = { if (total > 0) current.toFloat() / total else 0f },
                    modifier = Modifier.size(100.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // 中心百分比
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (total > 0) "${(current * 100 / total)}%" else "...",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$current/$total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.dialog_loading_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "正在扫描应用...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 空内容 - MD3 风格
 */
@Composable
private fun EmptyContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标容器
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.word_content_blank),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "下拉刷新以加载应用列表",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 底部导出卡片 - MD3 风格
 */
@Composable
private fun ExportCard(
    hasPermission: Boolean,
    availableStorage: String,
    showSystemApp: Boolean,
    onShowSystemAppChange: (Boolean) -> Unit,
    isShowSystemAppEnabled: Boolean,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            if (!hasPermission) {
                // 权限请求区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 权限图标
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "需要授予读取应用列表权限",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = {
                            PermissionExts.requestreadInstallApps(context as android.app.Activity) {
                                onPermissionGranted()
                            }
                        }
                    ) {
                        Text("授予")
                    }
                }
            } else {
                // 应用列表信息区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 存储图标
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.main_card_remaining_storage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = availableStorage,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 显示系统应用开关
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.main_card_show_system_app),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = showSystemApp,
                            onCheckedChange = {
                                if (isShowSystemAppEnabled) {
                                    onShowSystemAppChange(it)
                                }
                            },
                            enabled = isShowSystemAppEnabled,
                            modifier = Modifier.height(24.dp),
                            thumbContent = {
                                if (showSystemApp) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 分析 APK 按钮已隐藏
            }
        }
    }
}

/**
 * 多选卡片 - MD3 风格
 */
@Composable
private fun MultiSelectCard(
    selectedCount: Int,
    selectedSize: Long,
    onSelectAll: () -> Unit,
    onCopyPackageNames: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 选中数量和大小
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 数量标签
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "$selectedCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.unit_item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                // 分隔符
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                )

                // 大小
                Text(
                    text = Formatter.formatFileSize(LocalContext.current, selectedSize),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮组
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSelectAll
                ) {
                    Text(
                        text = stringResource(R.string.select_all_change),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FilledTonalButton(
                    onClick = onCopyPackageNames
                ) {
                    Text(
                        text = "复制包名",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

/**
 * 列表项 - 线性模式，MD3 风格
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinearAppItem(
    app: AppItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    highlightKeyword: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        tonalElevation = if (isSelected) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 应用图标容器
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(app.getIcon())
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )

                // 选中指示器
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSelected && isMultiSelectMode,
                    enter = scaleIn(spring(stiffness = Spring.StiffnessMedium)),
                    exit = scaleOut(spring(stiffness = Spring.StiffnessMedium))
                ) {
                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // 应用信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                // 应用名称
                Text(
                    text = highlightText(app.getAppName(), highlightKeyword),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (app.isRedMarked()) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 包名
                Text(
                    text = highlightText(app.getPackageName(), highlightKeyword),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // 选择框或大小
            if (isMultiSelectMode) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(4.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0f)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = Formatter.formatFileSize(LocalContext.current, app.getSize()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * 列表项 - 网格模式，MD3 风格
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridAppItem(
    app: AppItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        tonalElevation = if (isSelected) 0.dp else 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 应用图标
                Box {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(app.getIcon())
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // 选中指示器
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSelected,
                        enter = scaleIn(spring(stiffness = Spring.StiffnessMedium)),
                        exit = scaleOut(spring(stiffness = Spring.StiffnessMedium))
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 应用名称
                Text(
                    text = app.getAppName(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (app.isRedMarked()) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 高亮文本
 */
@Composable
private fun highlightText(text: String, keyword: String?): androidx.compose.ui.text.AnnotatedString {
    if (keyword.isNullOrEmpty()) {
        return androidx.compose.ui.text.AnnotatedString(text)
    }

    return buildAnnotatedString {
        var startIndex = 0
        var index = text.lowercase(Locale.getDefault()).indexOf(keyword.lowercase(Locale.getDefault()))

        while (index != -1) {
            append(text.substring(startIndex, index))
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                append(text.substring(index, index + keyword.length))
            }
            startIndex = index + keyword.length
            index = text.lowercase(Locale.getDefault()).indexOf(keyword.lowercase(Locale.getDefault()), startIndex)
        }
        append(text.substring(startIndex))
    }
}

/**
 * 刷新可用存储空间 - 与原实现完全一致
 */
private fun refreshAvailableStorage(context: Context, onResult: (String) -> Unit) {
    try {
        val isExternal = SPUtil.getIsSaved2ExternalStorage(context)
        var available: Long = 0

        if (isExternal) {
            val files = context.getExternalFilesDirs(null)
            for (file in files) {
                if (file.absolutePath.lowercase(Locale.getDefault())
                        .startsWith(StorageUtil.getMainExternalStoragePath())) continue
                available = StorageUtil.getAvaliableSizeOfPath(file.absolutePath)
            }
        } else {
            available = StorageUtil.getAvaliableSizeOfPath(StorageUtil.getMainExternalStoragePath())
        }

        onResult(Formatter.formatFileSize(context, available))
    } catch (e: Exception) {
        e.printStackTrace()
        onResult("")
    }
}
