package info.muge.appshare.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import info.muge.appshare.Global
import info.muge.appshare.R
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.ToastManager
import info.muge.appshare.ui.dialogs.AppBottomSheet
import info.muge.appshare.ui.screens.appdetail.AppInfoContent
import info.muge.appshare.ui.screens.appdetail.ComponentListContent
import info.muge.appshare.ui.screens.appdetail.ComponentType
import info.muge.appshare.ui.screens.appdetail.HashContent
import info.muge.appshare.ui.screens.appdetail.ManifestContent
import info.muge.appshare.ui.screens.appdetail.SignatureContent
import info.muge.appshare.ui.screens.appdetail.SoLibContent
import info.muge.appshare.ui.theme.AppDimens
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.findActivity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 应用详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String? = null,
    apkUri: Uri? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 获取应用信息
    var appItem by remember { mutableStateOf<AppItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 显示选项对话框
    var showIconOptions by remember { mutableStateOf(false) }

    // 折叠头部状态
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            // 上滑（delta < 0）→ 折叠 header，在子内容滚动前消费
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta >= 0) return Offset.Zero // 下滑不在这里处理
                val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                val consumed = newOffset - headerOffsetPx
                headerOffsetPx = newOffset
                return Offset(0f, consumed)
            }

            // 下滑（delta > 0）→ 子内容滚到顶后，剩余的用来展开 header
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta <= 0) return Offset.Zero // 上滑剩余不处理
                val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                val consumedY = newOffset - headerOffsetPx
                headerOffsetPx = newOffset
                return Offset(0f, consumedY)
            }
        }
    }

    // Tab配置
    val tabs = listOf(
        stringResource(R.string.tab_app_info),
        stringResource(R.string.tab_signature),
        stringResource(R.string.tab_hash),
        stringResource(R.string.tab_permissions),
        stringResource(R.string.tab_activities),
        stringResource(R.string.tab_services),
        stringResource(R.string.tab_receivers),
        stringResource(R.string.tab_providers),
        stringResource(R.string.tab_static_loaders),
        stringResource(R.string.tab_native_libs),
        stringResource(R.string.tab_manifest)
    )

    val pagerState = rememberPagerState { tabs.size }

    // 加载应用信息
    LaunchedEffect(packageName, apkUri) {
        try {
            appItem = when {
                packageName != null -> {
                    synchronized(Global.app_list) {
                        Global.getAppItemByPackageNameFromList(Global.app_list, packageName)
                    }
                }
                apkUri != null -> {
                    val cacheFile = java.io.File(context.externalCacheDir, "temp_analysis_${System.currentTimeMillis()}.apk")
                    try {
                        val persistedUris = context.contentResolver.persistedUriPermissions
                        val hasPermission = persistedUris.any {
                            it.uri == apkUri && it.isReadPermission
                        }
                        if (!hasPermission) {
                            context.contentResolver.takePersistableUriPermission(
                                apkUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                    } catch (_: Exception) { }

                    context.contentResolver.openInputStream(apkUri)?.use { input ->
                        java.io.FileOutputStream(cacheFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw Exception("无法打开文件")
                    AppItem(context, cacheFile.absolutePath)
                }
                else -> null
            }
            if (appItem == null) {
                errorMessage = "无法获取应用信息"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "解析失败: ${e.message}"
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // 折叠时显示应用名（渐显效果）
                    appItem?.let {
                        val progress = if (headerHeightPx > 0f) {
                            (-headerOffsetPx / headerHeightPx).coerceIn(0f, 1f)
                        } else 0f
                        Text(
                            text = it.getAppName(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.graphicsLayer { alpha = progress }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.systemBars.exclude(WindowInsets.navigationBars)
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val item = appItem ?: return@Scaffold
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .nestedScroll(nestedScrollConnection)
            ) {
                // 可折叠头部 — 首次 wrapContent 测量、之后动态高度折叠
                val headerWrapperModifier = if (headerHeightPx == 0f) {
                    // 首次渲染：自然高度，测量实际尺寸
                    Modifier.fillMaxWidth()
                } else {
                    // 已测量：动态高度 + 裁剪实现折叠
                    val collapsedHeightDp = with(density) {
                        (headerHeightPx + headerOffsetPx).coerceAtLeast(0f).toDp()
                    }
                    Modifier
                        .fillMaxWidth()
                        .height(collapsedHeightDp)
                        .clipToBounds()
                }
                Box(modifier = headerWrapperModifier) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                if (headerHeightPx == 0f) {
                                    headerHeightPx = coordinates.size.height.toFloat()
                                }
                            }
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        AppDetailHeader(
                            appItem = item,
                            isExternal = item.getInstallSource() == "External File",
                            onIconClick = { showIconOptions = true },
                            onLaunchClick = {
                                try {
                                    context.startActivity(
                                        context.packageManager.getLaunchIntentForPackage(item.getPackageName())
                                    )
                                } catch (_: Exception) {
                                    ToastManager.showToast(context, "应用没有界面,无法运行", Toast.LENGTH_SHORT)
                                }
                            },
                            onExportClick = {
                                val list = ArrayList<AppItem>()
                                list.add(AppItem(item, false, false))
                                Global.checkAndExportCertainAppItemsToSetPathWithoutShare(
                                    context.findActivity(),
                                    list,
                                    false,
                                    object : Global.ExportTaskFinishedListener {
                                        override fun onFinished(errorMessage: String) {
                                            if (errorMessage.trim().isEmpty()) {
                                                ToastManager.showToast(
                                                    context,
                                                    context.getString(R.string.toast_export_complete),
                                                    Toast.LENGTH_SHORT
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }
                }

                // Tab 栏（自然吸顶）
                SecondaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    edgePadding = AppDimens.Space.lg
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 内容区域（weight 填满剩余空间 + 导航栏底部 padding）
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                ) { page ->
                    when (page) {
                        0 -> AppInfoContent(item)
                        1 -> SignatureContent(item)
                        2 -> HashContent(item)
                        3 -> ComponentListContent(context, item, ComponentType.PERMISSION)
                        4 -> ComponentListContent(context, item, ComponentType.ACTIVITY)
                        5 -> ComponentListContent(context, item, ComponentType.SERVICE)
                        6 -> ComponentListContent(context, item, ComponentType.RECEIVER)
                        7 -> ComponentListContent(context, item, ComponentType.PROVIDER)
                        8 -> ComponentListContent(context, item, ComponentType.STATIC_LOADER)
                        9 -> SoLibContent(item)
                        10 -> ManifestContent(item)
                    }
                }
            }
        }
    }

    // 图标选项对话框
    if (showIconOptions) {
        appItem?.let { item ->
            val isExternal = item.getInstallSource() == "External File"

            AppBottomSheet(
                title = item.getAppName(),
                onDismiss = { showIconOptions = false },
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimens.Space.lg)
                    ) {
                        if (!isExternal) {
                            OptionRow(
                                icon = Icons.Outlined.Settings,
                                label = stringResource(R.string.menu_open_system_settings),
                                onClick = {
                                    showIconOptions = false
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    intent.data = Uri.fromParts("package", item.getPackageName(), null)
                                    context.startActivity(intent)
                                }
                            )
                        }
                        OptionRow(
                            icon = Icons.Outlined.SaveAlt,
                            label = stringResource(R.string.action_save_icon),
                            onClick = {
                                showIconOptions = false
                                EnvironmentUtil.saveDrawableToGallery(
                                    context,
                                    item.getIcon(),
                                    item.getAppName()
                                )
                            }
                        )
                    }
                }
            )
        }
    }
}

/**
 * 底部弹窗选项行
 */
@Composable
private fun OptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Radius.md))
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.Space.lg, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(AppDimens.Space.lg))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 应用详情头部 — MD3 现代化设计
 */
@Composable
private fun AppDetailHeader(
    appItem: AppItem,
    isExternal: Boolean,
    onIconClick: () -> Unit,
    onLaunchClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Space.lg)
            .padding(top = 4.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 应用图标
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(AppDimens.Radius.xl))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onIconClick),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(appItem.getIcon())
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(AppDimens.Radius.lg)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 应用名
        Text(
            text = appItem.getAppName(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 版本
        Text(
            text = "v${appItem.getVersionName()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 操作按钮组
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(onClick = onExportClick) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("导出")
            }

            if (!isExternal) {
                OutlinedButton(onClick = onLaunchClick) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("打开")
                }
            }
        }
    }
}
