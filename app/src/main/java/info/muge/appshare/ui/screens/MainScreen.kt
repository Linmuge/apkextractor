package info.muge.appshare.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import info.muge.appshare.R
import info.muge.appshare.ui.theme.AppDimens

/**
 * 底部导航项
 */
data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val title: String
)

/**
 * 主页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToAppDetail: (String) -> Unit = {},
    onNavigateToAppDetailWithUri: (Uri) -> Unit = {},
    onNavigateToAppChange: () -> Unit = {},
    onNavigateToThemeSettings: () -> Unit = {},
    appListViewModel: AppListViewModel = viewModel()
) {
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }

    // 多选模式状态
    var isMultiSelectMode by rememberSaveable { mutableStateOf(false) }

    // 视图模式（0=列表，1=网格）
    var viewMode by rememberSaveable { mutableIntStateOf(0) }

    // 排序选项对话框
    var showSortDialog by rememberSaveable { mutableStateOf(false) }

    // 筛选弹窗状态
    var showFilterSheet by remember { mutableStateOf(false) }

    // 分组弹窗状态
    var showGroupDialog by remember { mutableStateOf(false) }

    val appListState by appListViewModel.uiState.collectAsStateWithLifecycle()

    val navItems = listOf(
        BottomNavItem(
            route = "home",
            icon = Icons.Default.Home,
            title = stringResource(R.string.main_page_export)
        ),
        BottomNavItem(
            route = "statistics",
            icon = Icons.Default.BarChart,
            title = stringResource(R.string.nav_statistics)
        ),
        BottomNavItem(
            route = "settings",
            icon = Icons.Default.Settings,
            title = stringResource(R.string.action_settings)
        )
    )

    // 返回键处理：仅在可关闭的页面态拦截
    BackHandler(enabled = isMultiSelectMode || isSearchMode) {
        if (isMultiSelectMode) {
            isMultiSelectMode = false
        } else if (isSearchMode) {
            isSearchMode = false
            searchText = ""
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // 筛选底部弹窗
    if (showFilterSheet) {
        FilterBottomSheet(
            currentConfig = appListState.filterConfig,
            availableInstallers = appListState.availableInstallers,
            onApply = { config ->
                appListViewModel.updateFilter(config)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    // 分组选择对话框
    if (showGroupDialog) {
        GroupModeDialog(
            currentMode = appListState.groupMode,
            onSelect = { mode ->
                appListViewModel.setGroupMode(mode)
                showGroupDialog = false
            },
            onDismiss = { showGroupDialog = false }
        )
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isSearchMode,
                enter = fadeIn(tween(200)) + slideInHorizontally(tween(200)) { it },
                exit = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it }
            ) {
                SearchTopBar(
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    onClose = {
                        isSearchMode = false
                        searchText = ""
                    }
                )
            }

            AnimatedVisibility(
                visible = !isSearchMode,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                MainTopBar(
                    title = when (currentTab) {
                        0 -> stringResource(R.string.app_name)
                        1 -> stringResource(R.string.nav_statistics)
                        2 -> stringResource(R.string.action_settings)
                        else -> stringResource(R.string.app_name)
                    },
                    showSearch = currentTab == 0,
                    showMenu = currentTab == 0,
                    viewMode = viewMode,
                    activeFilterCount = appListState.filterConfig.activeCount,
                    groupMode = appListState.groupMode,
                    onSearchClick = { isSearchMode = true },
                    onFilterClick = { showFilterSheet = true },
                    onGroupClick = { showGroupDialog = true },
                    onSortClick = { showSortDialog = true },
                    onViewModeClick = { viewMode = if (viewMode == 0) 1 else 0 },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        bottomBar = {
            MainNavigationBar(
                items = navItems,
                currentSelection = currentTab,
                onSelectionChange = { currentTab = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 使用 Box 堆叠所有 Tab 页，通过透明度和交互控制切换
            // 这样切换 Tab 时可以保持各页面的滚动位置和状态
            // 活跃 Tab 通过 zIndex 浮到最上层，确保接收触摸事件
            Box(modifier = Modifier
                .fillMaxSize()
                .zIndex(if (currentTab == 0) 1f else 0f)
                .graphicsLayer {
                    alpha = if (currentTab == 0) 1f else 0f
                }
            ) {
                if (currentTab == 0 || !isSearchMode) {
                    AppListScreen(
                        viewMode = viewMode,
                        isSearchMode = isSearchMode,
                        searchText = searchText,
                        isMultiSelectMode = isMultiSelectMode,
                        onMultiSelectModeChange = { isMultiSelectMode = it },
                        onNavigateToDetail = onNavigateToAppDetail,
                        showSortDialog = showSortDialog,
                        onSortDialogDismiss = { showSortDialog = false },
                        viewModel = appListViewModel
                    )
                }
            }

            Box(modifier = Modifier
                .fillMaxSize()
                .zIndex(if (currentTab == 1) 1f else 0f)
                .graphicsLayer {
                    alpha = if (currentTab == 1) 1f else 0f
                }
            ) {
                StatisticsScreen(onNavigateToAppDetail = onNavigateToAppDetail)
            }

            Box(modifier = Modifier
                .fillMaxSize()
                .zIndex(if (currentTab == 2) 1f else 0f)
                .graphicsLayer {
                    alpha = if (currentTab == 2) 1f else 0f
                }
            ) {
                SettingsScreen(
                    onNavigateToAppChange = onNavigateToAppChange,
                    onNavigateToThemeSettings = onNavigateToThemeSettings
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    title: String,
    showSearch: Boolean,
    showMenu: Boolean,
    viewMode: Int,
    activeFilterCount: Int = 0,
    groupMode: GroupMode = GroupMode.NONE,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit = {},
    onGroupClick: () -> Unit = {},
    onSortClick: () -> Unit,
    onViewModeClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge
            )
        },
        actions = {
            if (showSearch) {
                FilledTonalIconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_label)
                    )
                }
            }
            if (showMenu) {
                // 筛选按钮（带 Badge）
                FilledTonalIconButton(onClick = onFilterClick) {
                    if (activeFilterCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(activeFilterCount.toString())
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.menu_filter)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.menu_filter)
                        )
                    }
                }

                // 更多菜单（收纳分组/排序/视图切换）
                Box {
                    FilledTonalIconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.menu_more)
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.action_sort))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onSortClick()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (groupMode != GroupMode.NONE) {
                                        stringResource(R.string.menu_group_active, groupMode.label)
                                    } else {
                                        stringResource(R.string.menu_group)
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = if (groupMode != GroupMode.NONE) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onGroupClick()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (viewMode == 0) stringResource(R.string.menu_switch_to_grid) else stringResource(R.string.menu_switch_to_list)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (viewMode == 0) Icons.Default.Apps else Icons.AutoMirrored.Filled.List,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onViewModeClick()
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        scrollBehavior = scrollBehavior
    )
}

/**
 * 分组模式选择对话框
 */
@Composable
private fun GroupModeDialog(
    currentMode: GroupMode,
    onSelect: (GroupMode) -> Unit,
    onDismiss: () -> Unit
) {
    info.muge.appshare.ui.dialogs.AppBottomSheet(
        title = stringResource(R.string.dialog_select_group_mode),
        onDismiss = onDismiss,
        content = {
            Column {
                GroupMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDimens.Radius.sm))
                            .selectable(
                                selected = currentMode == mode,
                                onClick = { onSelect(mode) }
                            )
                            .padding(vertical = AppDimens.Space.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = null
                        )
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = AppDimens.Space.sm)
                        )
                    }
                }
            }
        },
        actions = {
            info.muge.appshare.ui.dialogs.AppBottomSheetActions(
                onConfirm = onDismiss,
                confirmText = stringResource(R.string.action_cancel)
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = AppDimens.Elevation.none
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Space.sm, vertical = AppDimens.Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_label),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            BasicTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    TextFieldDefaults.DecorationBox(
                        value = searchText,
                        innerTextField = innerTextField,
                        enabled = true,
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = interactionSource,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(AppDimens.Radius.xl),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
                            start = AppDimens.Space.lg,
                            end = if (searchText.isNotEmpty()) AppDimens.Space.xs else AppDimens.Space.lg,
                            top = AppDimens.Space.md,
                            bottom = AppDimens.Space.md
                        ),
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchTextChange("") },
                                    modifier = Modifier.clip(RoundedCornerShape(AppDimens.Radius.full))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.clear_label),
                                        modifier = Modifier.padding(AppDimens.Space.xs),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.padding(horizontal = AppDimens.Space.xs))
        }
    }
}

@Composable
private fun MainNavigationBar(
    items: List<BottomNavItem>,
    currentSelection: Int,
    onSelectionChange: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column {
            NavigationBar(
                modifier = Modifier.height(60.dp),
                containerColor = Color.Transparent,
                tonalElevation = AppDimens.Elevation.none,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = currentSelection == index,
                        onClick = { onSelectionChange(index) },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    }
}
