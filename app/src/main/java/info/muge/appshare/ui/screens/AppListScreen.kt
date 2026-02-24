package info.muge.appshare.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import info.muge.appshare.Constants
import info.muge.appshare.R
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.components.AlphabetIndexBar
import info.muge.appshare.ui.dialogs.SortConfigDialog
import info.muge.appshare.ui.theme.AppDimens
import info.muge.appshare.utils.PinyinUtil
import info.muge.appshare.utils.SPUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 应用列表页面 - 使用 ViewModel 管理状态
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
    onSortDialogDismiss: () -> Unit = {},
    viewModel: AppListViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 初始化
    LaunchedEffect(Unit) {
        viewModel.initPermission(context)
    }

    // 监听应用安装/卸载/更新，自动刷新列表
    DisposableEffect(viewModel) {
        viewModel.registerPackageChangeListener(context)
        onDispose {
            viewModel.unregisterPackageChangeListener(context)
        }
    }

    // 搜索功能（带防抖）
    LaunchedEffect(isSearchMode, searchText) {
        if (isSearchMode && searchText.isNotEmpty()) {
            delay(300L)
            viewModel.searchApps(searchText)
        } else if (!isSearchMode) {
            viewModel.exitSearchMode()
        }
    }

    // 权限授予后刷新
    LaunchedEffect(state.hasPermission) {
        if (state.hasPermission && state.appList.isEmpty()) {
            viewModel.refreshAppList(context)
        }
    }

    // 多选模式状态变化
    LaunchedEffect(isMultiSelectMode) {
        if (!isMultiSelectMode) {
            viewModel.clearSelection()
        }
    }

    // 搜索模式状态变化
    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            if (isMultiSelectMode) {
                onMultiSelectModeChange(false)
            }
        } else {
            viewModel.exitSearchMode()
        }
    }

    // 排序对话框
    if (showSortDialog) {
        SortConfigDialog(
            onDismiss = onSortDialogDismiss,
            onOptionSelected = { sortValue ->
                viewModel.sortApps(sortValue)
                onSortDialogDismiss()
            }
        )
    }

    val listState = rememberLazyListState()

    // 字母索引条：列表视图 + 非搜索 + 无分组时显示
    val showAlphabetIndex = viewMode == 0 && !isSearchMode && state.groupMode == GroupMode.NONE

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        bottomBar = {
            when {
                !state.hasPermission -> PermissionBottomBar(
                    onPermissionGranted = {
                        viewModel.grantPermission(context)
                    }
                )
                isMultiSelectMode -> MultiSelectCard(
                    selectedCount = state.selectedItems.size,
                    selectedSize = state.selectedSize,
                    onSelectAll = { viewModel.toggleSelectAll() },
                    onInvertSelection = { viewModel.invertSelection() },
                    onDeselectAll = {
                        viewModel.clearSelection()
                        onMultiSelectModeChange(false)
                    },
                    onCopyPackageNames = {
                        if (state.selectedItems.isEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.snack_bar_no_app_selected))
                            }
                            return@MultiSelectCard
                        }
                        val separator = SPUtil.getGlobalSharedPreferences(context)
                            .getString(Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR, ",") ?: ","
                        val resultString = state.selectedItems.joinToString(separator)

                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("message", resultString))

                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.snack_bar_clipboard))
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!state.hasPermission) {
                EmptyContent()
            } else if (state.isLoading && state.appList.isEmpty()) {
                LoadingContent(
                    current = state.loadingCurrent,
                    total = state.loadingTotal
                )
            } else if (state.appList.isEmpty() && !isSearchMode) {
                EmptyContent()
            } else if (state.appList.isEmpty() && isSearchMode && searchText.isNotEmpty()) {
                if (state.isRefreshing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    SearchEmptyContent()
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = {
                        if (!isSearchMode && !isMultiSelectMode) {
                            viewModel.refreshAppList(context)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (viewMode == 0) {
                        // 列表视图
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (state.groupMode != GroupMode.NONE && state.groupedAppList.isNotEmpty()) {
                                // 分组模式
                                GroupedAppList(
                                    groupedApps = state.groupedAppList,
                                    selectedItems = state.selectedItems,
                                    isMultiSelectMode = isMultiSelectMode,
                                    highlightKeyword = state.highlightKeyword,
                                    isSearchMode = isSearchMode,
                                    onAppClick = { app ->
                                        if (isMultiSelectMode) {
                                            viewModel.toggleSelection(app)
                                            if (!viewModel.hasSelection()) {
                                                onMultiSelectModeChange(false)
                                            }
                                        } else {
                                            onNavigateToDetail(app.getPackageName())
                                        }
                                    },
                                    onAppLongClick = { app ->
                                        if (!isSearchMode) {
                                            onMultiSelectModeChange(true)
                                            viewModel.toggleSelection(app)
                                        }
                                    }
                                )
                            } else {
                                // 普通列表
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    state = listState,
                                    contentPadding = PaddingValues(bottom = AppDimens.Space.xs)
                                ) {
                                    items(state.appList) { app ->
                                        LinearAppItem(
                                            app = app,
                                            isSelected = state.selectedItems.contains(app.getPackageName()),
                                            isMultiSelectMode = isMultiSelectMode,
                                            highlightKeyword = state.highlightKeyword,
                                            onClick = {
                                                if (isMultiSelectMode) {
                                                    viewModel.toggleSelection(app)
                                                    if (!viewModel.hasSelection()) {
                                                        onMultiSelectModeChange(false)
                                                    }
                                                } else {
                                                    onNavigateToDetail(app.getPackageName())
                                                }
                                            },
                                            onLongClick = {
                                                if (!isSearchMode) {
                                                    onMultiSelectModeChange(true)
                                                    viewModel.toggleSelection(app)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // 字母索引条
                            if (showAlphabetIndex) {
                                AlphabetIndexBar(
                                    onLetterSelected = { letter ->
                                        scope.launch {
                                            val index = state.appList.indexOfFirst { app ->
                                                try {
                                                    val pinyin = PinyinUtil.getFirstSpell(app.getAppName())
                                                    val first = pinyin.firstOrNull()?.uppercaseChar() ?: '#'
                                                    val appLetter = if (first in 'A'..'Z') first else '#'
                                                    appLetter == letter
                                                } catch (_: Exception) {
                                                    letter == '#'
                                                }
                                            }
                                            if (index >= 0) {
                                                listState.animateScrollToItem(index)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = AppDimens.Space.xs)
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = AppDimens.Space.xs)
                        ) {
                            items(state.appList) { app ->
                                GridAppItem(
                                    app = app,
                                    isSelected = state.selectedItems.contains(app.getPackageName()),
                                    onClick = {
                                        if (isMultiSelectMode) {
                                            viewModel.toggleSelection(app)
                                            if (!viewModel.hasSelection()) {
                                                onMultiSelectModeChange(false)
                                            }
                                        } else {
                                            onNavigateToDetail(app.getPackageName())
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSearchMode) {
                                            onMultiSelectModeChange(true)
                                            viewModel.toggleSelection(app)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 分组列表
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupedAppList(
    groupedApps: Map<String, List<AppItem>>,
    selectedItems: Set<String>,
    isMultiSelectMode: Boolean,
    highlightKeyword: String?,
    isSearchMode: Boolean,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppDimens.Space.xs)
    ) {
        groupedApps.forEach { (group, apps) ->
            stickyHeader(key = "header_$group") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(
                            horizontal = AppDimens.Space.lg,
                            vertical = AppDimens.Space.sm
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = group,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${apps.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppDimens.Radius.full))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = AppDimens.Space.sm, vertical = AppDimens.Space.xs)
                    )
                }
            }

            items(apps) { app ->
                LinearAppItem(
                    app = app,
                    isSelected = selectedItems.contains(app.getPackageName()),
                    isMultiSelectMode = isMultiSelectMode,
                    highlightKeyword = highlightKeyword,
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) }
                )
            }
        }
    }
}
