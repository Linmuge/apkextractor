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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.muge.appshare.R

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
    onNavigateToAppDetailWithUri: (Uri) -> Unit = {}
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

    val navItems = listOf(
        BottomNavItem(
            route = "home",
            icon = Icons.Default.Home,
            title = stringResource(R.string.main_page_export)
        ),
        BottomNavItem(
            route = "statistics",
            icon = Icons.Default.BarChart,
            title = "统计"
        ),
        BottomNavItem(
            route = "settings",
            icon = Icons.Default.Settings,
            title = stringResource(R.string.action_settings)
        )
    )

    // 返回键处理
    BackHandler(enabled = true) {
        when {
            isMultiSelectMode -> {
                isMultiSelectMode = false
            }
            isSearchMode -> {
                isSearchMode = false
                searchText = ""
            }
            else -> {
                // 退出应用
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

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
                        1 -> "统计"
                        2 -> stringResource(R.string.action_settings)
                        else -> stringResource(R.string.app_name)
                    },
                    showSearch = currentTab == 0,
                    showMenu = currentTab == 0,
                    viewMode = viewMode,
                    onSearchClick = { isSearchMode = true },
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
                .imePadding()
        ) {
            when (currentTab) {
                0 -> AppListScreen(
                    viewMode = viewMode,
                    isSearchMode = isSearchMode,
                    searchText = searchText,
                    isMultiSelectMode = isMultiSelectMode,
                    onMultiSelectModeChange = { isMultiSelectMode = it },
                    onNavigateToDetail = onNavigateToAppDetail,
                    onNavigateToDetailWithUri = onNavigateToAppDetailWithUri,
                    showSortDialog = showSortDialog,
                    onSortDialogDismiss = { showSortDialog = false }
                )
                1 -> StatisticsScreen(onNavigateToAppDetail = onNavigateToAppDetail)
                2 -> SettingsScreen()
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
    onSearchClick: () -> Unit,
    onSortClick: () -> Unit,
    onViewModeClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
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
                        contentDescription = "搜索"
                    )
                }
            }
            if (showMenu) {
                FilledTonalIconButton(onClick = onSortClick) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = stringResource(R.string.action_sort)
                    )
                }
                FilledTonalIconButton(onClick = onViewModeClick) {
                    Icon(
                        imageVector = if (viewMode == 0) Icons.Default.Apps else Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.action_view)
                    )
                }
            }
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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
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
                                text = "搜索应用...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
                            start = 16.dp,
                            end = if (searchText.isNotEmpty()) 4.dp else 16.dp,
                            top = 12.dp,
                            bottom = 12.dp
                        ),
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchTextChange("") },
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "清除",
                                        modifier = Modifier.padding(4.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@Composable
private fun MainNavigationBar(
    items: List<BottomNavItem>,
    currentSelection: Int,
    onSelectionChange: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
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
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun Spacer(modifier: Modifier = Modifier) {
    Box(modifier = modifier)
}
