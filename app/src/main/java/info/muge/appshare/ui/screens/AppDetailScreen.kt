package info.muge.appshare.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import info.muge.appshare.Global
import info.muge.appshare.R
import info.muge.appshare.items.AppItem
import info.muge.appshare.tasks.HashTask
import info.muge.appshare.ui.dialogs.AppBottomSheet
import info.muge.appshare.ui.dialogs.AppBottomSheetDualActions
import info.muge.appshare.ui.ToastManager
import info.muge.appshare.ui.theme.AppDimens
import info.muge.appshare.utils.AXMLPrinter
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.SPUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile

/**
 * 应用详情页面 - 1:1 匹配原始 AppDetailActivity 实现
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppDetailScreen(
    packageName: String? = null,
    apkUri: Uri? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 获取应用信息
    var appItem by remember { mutableStateOf<AppItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 显示选项对话框
    var showIconOptions by remember { mutableStateOf(false) }

    // Tab配置 - 与原 AppDetailPagerAdapter 完全一致（11个Tab）
    val tabs = listOf(
        stringResource(R.string.tab_app_info),      // 0 - 信息
        stringResource(R.string.tab_signature),     // 1 - 签名
        stringResource(R.string.tab_hash),          // 2 - Hash
        stringResource(R.string.tab_permissions),   // 3 - 权限
        stringResource(R.string.tab_activities),    // 4 - Activities
        stringResource(R.string.tab_services),      // 5 - Services
        stringResource(R.string.tab_receivers),     // 6 - Receivers
        stringResource(R.string.tab_providers),     // 7 - Providers
        stringResource(R.string.tab_static_loaders),// 8 - Static Loaders
        stringResource(R.string.tab_native_libs),   // 9 - Native Libs
        stringResource(R.string.tab_manifest)       // 10 - Manifest
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
                    // 处理外部APK - 复制到缓存目录以避免权限问题
                    val cacheFile = java.io.File(context.externalCacheDir, "temp_analysis_${System.currentTimeMillis()}.apk")

                    // 尝试获取持久化权限（如果还没有）
                    try {
                        val persistedUris = context.contentResolver.persistedUriPermissions
                        val hasPermission = persistedUris.any {
                            it.uri == apkUri && it.isReadPermission
                        }
                        if (!hasPermission) {
                            context.contentResolver.takePersistableUriPermission(
                                apkUri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                    } catch (e: Exception) {
                        // 某些URI不支持持久化权限，继续尝试读取
                        e.printStackTrace()
                    }

                    // 读取并复制文件
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

    // 主页面结构 - 与原 activity_app_detail.xml 完全一致
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 运行按钮 - 与原 menu_app_detail 一致
                    if (appItem?.getInstallSource() != "External File") {
                        IconButton(onClick = {
                            try {
                                context.startActivity(
                                    context.packageManager.getLaunchIntentForPackage(
                                        appItem!!.getPackageName()
                                    )
                                )
                            } catch (e: Exception) {
                                ToastManager.showToast(
                                    context,
                                    "应用没有界面,无法运行",
                                    Toast.LENGTH_SHORT
                                )
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.icon_play),
                                contentDescription = "运行",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    // 导出按钮
                    IconButton(onClick = {
                        appItem?.let { item ->
                            val list = ArrayList<AppItem>()
                            list.add(AppItem(item, false, false))
                            Global.checkAndExportCertainAppItemsToSetPathWithoutShare(
                                context as android.app.Activity,
                                list,
                                false,
                                object : Global.ExportTaskFinishedListener {
                                    override fun onFinished(errorMessage: String) {
                                        if (errorMessage.trim().isNotEmpty()) {
                                            // 显示错误对话框
                                        } else {
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
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_download),
                            contentDescription = "导出",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
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
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (appItem != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 可折叠头部区域 - 与原 CollapsingToolbarLayout 一致
                AppDetailHeader(
                    appItem = appItem!!,
                    onIconClick = { showIconOptions = true }
                )

                // TabRow - 与原 TabLayout 一致 (scrollable mode)
                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    edgePadding = 0.dp
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
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Pager内容
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val pkgName = appItem?.getPackageName() ?: ""

                    when (page) {
                        0 -> AppInfoContent(appItem!!)
                        1 -> SignatureContent(appItem!!)
                        2 -> HashContent(appItem!!)
                        3 -> ComponentListContent(context, appItem!!, ComponentType.PERMISSION)
                        4 -> ComponentListContent(context, appItem!!, ComponentType.ACTIVITY)
                        5 -> ComponentListContent(context, appItem!!, ComponentType.SERVICE)
                        6 -> ComponentListContent(context, appItem!!, ComponentType.RECEIVER)
                        7 -> ComponentListContent(context, appItem!!, ComponentType.PROVIDER)
                        8 -> ComponentListContent(context, appItem!!, ComponentType.STATIC_LOADER)
                        9 -> SoLibContent(appItem!!)
                        10 -> ManifestContent(appItem!!)
                    }
                }
            }
        }
    }

    // 图标选项对话框
    if (showIconOptions) {
        appItem?.let { item ->
            val isExternal = item.getInstallSource() == "External File"
            val options = if (isExternal) {
                listOf(stringResource(R.string.action_save_icon))
            } else {
                listOf(
                    stringResource(R.string.menu_open_system_settings),
                    stringResource(R.string.action_save_icon)
                )
            }

            AppBottomSheet(
                title = "选择操作",
                onDismiss = { showIconOptions = false },
                content = {
                    Column {
                        options.forEachIndexed { index, option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showIconOptions = false
                                        when {
                                            isExternal && index == 0 -> {
                                                EnvironmentUtil.saveDrawableToGallery(
                                                    context,
                                                    item.getIcon(),
                                                    item.getAppName()
                                                )
                                            }
                                            !isExternal && index == 0 -> {
                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                                intent.data = Uri.fromParts("package", item.getPackageName(), null)
                                                context.startActivity(intent)
                                            }
                                            !isExternal && index == 1 -> {
                                                EnvironmentUtil.saveDrawableToGallery(
                                                    context,
                                                    item.getIcon(),
                                                    item.getAppName()
                                                )
                                            }
                                        }
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(option, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                },
                actions = {
                    AppBottomSheetDualActions(
                        onConfirm = { showIconOptions = false },
                        onDismiss = { showIconOptions = false },
                        confirmText = stringResource(android.R.string.ok),
                        dismissText = stringResource(android.R.string.cancel)
                    )
                }
            )
        }
    }
}

/**
 * 应用详情头部 - 与原 CollapsingToolbarLayout 内容一致
 */
@Composable
private fun AppDetailHeader(
    appItem: AppItem,
    onIconClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 应用图标 - 96dp, ExtraLarge圆角 - 与原布局一致
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(appItem.getIcon())
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(AppDimens.Radius.xl))
                .clickable(onClick = onIconClick),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 应用名称 - headlineSmall, bold - 与原布局一致
        Text(
            text = appItem.getAppName(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 版本号 - bodyMedium, onSurfaceVariant - 与原布局一致
        Text(
            text = appItem.getVersionName(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * 组件类型枚举
 */
enum class ComponentType {
    PERMISSION, ACTIVITY, SERVICE, RECEIVER, PROVIDER, STATIC_LOADER
}

/**
 * 复制到剪贴板
 */
private fun copyToClipboard(context: Context, text: String?) {
    if (text.isNullOrEmpty()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("text", text))
    ToastManager.showToast(context, "已复制", Toast.LENGTH_SHORT)
}

// ==================== App Info Content ====================

/**
 * 应用信息内容 - 与原 fragment_app_info.xml 完全一致
 */
@Composable
private fun AppInfoContent(appItem: AppItem) {
    val context = LocalContext.current
    val packageInfo = appItem.getPackageInfo()
    val appInfo = packageInfo.applicationInfo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppDimens.Space.lg)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.Radius.xl),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                // 包名
                InfoItemVertical(
                    label = stringResource(R.string.activity_detail_package_name),
                    value = appItem.getPackageName(),
                    onClick = { copyToClipboard(context, appItem.getPackageName()) }
                )
                InfoDivider()

                // 版本名
                InfoItemHorizontal(
                    label = stringResource(R.string.activity_detail_version_name),
                    value = appItem.getVersionName(),
                    onClick = { copyToClipboard(context, appItem.getVersionName()) }
                )

                // 版本号
                InfoItemHorizontal(
                    label = stringResource(R.string.activity_detail_version_code),
                    value = appItem.getVersionCode().toString(),
                    onClick = { copyToClipboard(context, appItem.getVersionCode().toString()) }
                )

                // 大小
                InfoItemHorizontal(
                    label = stringResource(R.string.activity_detail_package_size),
                    value = Formatter.formatFileSize(context, appItem.getSize()),
                    onClick = { copyToClipboard(context, Formatter.formatFileSize(context, appItem.getSize())) }
                )

                InfoDivider()

                // 首次安装时间
                val installTime = SimpleDateFormat.getDateTimeInstance().format(Date(packageInfo.firstInstallTime))
                InfoItemHorizontal(
                    label = stringResource(R.string.activity_detail_first_install_time),
                    value = installTime,
                    onClick = { copyToClipboard(context, installTime) }
                )

                // 上次更新时间
                val updateTime = SimpleDateFormat.getDateTimeInstance().format(Date(packageInfo.lastUpdateTime))
                InfoItemHorizontal(
                    label = stringResource(R.string.activity_detail_last_update_time),
                    value = updateTime,
                    onClick = { copyToClipboard(context, updateTime) }
                )

                // 安装来源
                InfoItemHorizontal(
                    label = stringResource(R.string.activity_detail_installer_name),
                    value = appItem.getInstallSource(),
                    onClick = { copyToClipboard(context, appItem.getInstallSource()) }
                )

                InfoDivider()

                // 最低API
                InfoItemHorizontal(
                    label = stringResource(R.string.activity_detail_minimum_api),
                    value = appInfo?.minSdkVersion?.toString() ?: "-",
                    onClick = { copyToClipboard(context, appInfo?.minSdkVersion?.toString()) }
                )

                // 目标API
                InfoItemHorizontal(
                    label = stringResource(R.string.activity_detail_target_api),
                    value = appInfo?.targetSdkVersion?.toString() ?: "-",
                    onClick = { copyToClipboard(context, appInfo?.targetSdkVersion?.toString()) }
                )

                // 系统应用
                val isSystemApp = ((appInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM) > 0
                val systemAppText = if (isSystemApp) stringResource(R.string.word_yes) else stringResource(R.string.word_no)
                InfoItemHorizontal(
                    label = stringResource(R.string.activity_detail_is_system_app),
                    value = systemAppText,
                    onClick = { copyToClipboard(context, systemAppText) }
                )

                // UID
                InfoItemHorizontal(
                    label = stringResource(R.string.activity_detail_uid),
                    value = appInfo?.uid?.toString() ?: "-",
                    onClick = { copyToClipboard(context, appInfo?.uid?.toString()) }
                )

                InfoDivider()

                // 路径
                InfoItemVertical(
                    label = stringResource(R.string.activity_detail_path),
                    value = appInfo?.sourceDir ?: "-",
                    onClick = { copyToClipboard(context, appInfo?.sourceDir) }
                )

                // 主启动类
                InfoItemVertical(
                    label = stringResource(R.string.activity_detail_launch_intent),
                    value = appItem.getLaunchingClass() ?: "-",
                    onClick = { copyToClipboard(context, appItem.getLaunchingClass()) }
                )

                InfoDivider()

                // 数据目录
                InfoItemVertical(
                    label = stringResource(R.string.activity_detail_data_dir),
                    value = appInfo?.dataDir ?: "-",
                    onClick = { copyToClipboard(context, appInfo?.dataDir) }
                )

                // Native库目录
                InfoItemVertical(
                    label = stringResource(R.string.activity_detail_native_lib_dir),
                    value = appInfo?.nativeLibraryDir ?: "-",
                    onClick = { copyToClipboard(context, appInfo?.nativeLibraryDir) }
                )

                // 进程名
                InfoItemVertical(
                    label = stringResource(R.string.activity_detail_process_name),
                    value = appInfo?.processName ?: "-",
                    onClick = { copyToClipboard(context, appInfo?.processName) }
                )

                InfoDivider()

                // Flags
                val flagsString = getFlagsString(appInfo?.flags ?: 0)
                InfoItemVertical(
                    label = stringResource(R.string.activity_detail_flags),
                    value = flagsString,
                    onClick = { copyToClipboard(context, flagsString) }
                )
            }
        }
    }
}

@Composable
private fun InfoItemHorizontal(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InfoItemVertical(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        thickness = 1.dp
    )
}

private fun getFlagsString(flags: Int): String {
    val flagList = mutableListOf<String>()
    if (flags and ApplicationInfo.FLAG_SYSTEM != 0) flagList.add("SYSTEM")
    if (flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) flagList.add("DEBUGGABLE")
    if (flags and ApplicationInfo.FLAG_HAS_CODE != 0) flagList.add("HAS_CODE")
    if (flags and ApplicationInfo.FLAG_PERSISTENT != 0) flagList.add("PERSISTENT")
    if (flags and ApplicationInfo.FLAG_FACTORY_TEST != 0) flagList.add("FACTORY_TEST")
    if (flags and ApplicationInfo.FLAG_ALLOW_TASK_REPARENTING != 0) flagList.add("ALLOW_TASK_REPARENTING")
    if (flags and ApplicationInfo.FLAG_ALLOW_CLEAR_USER_DATA != 0) flagList.add("ALLOW_CLEAR_USER_DATA")
    if (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) flagList.add("UPDATED_SYSTEM_APP")
    if (flags and ApplicationInfo.FLAG_TEST_ONLY != 0) flagList.add("TEST_ONLY")
    if (flags and ApplicationInfo.FLAG_VM_SAFE_MODE != 0) flagList.add("VM_SAFE_MODE")
    if (flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0) flagList.add("ALLOW_BACKUP")
    if (flags and ApplicationInfo.FLAG_KILL_AFTER_RESTORE != 0) flagList.add("KILL_AFTER_RESTORE")
    if (flags and ApplicationInfo.FLAG_RESTORE_ANY_VERSION != 0) flagList.add("RESTORE_ANY_VERSION")
    if (flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE != 0) flagList.add("EXTERNAL_STORAGE")
    if (flags and ApplicationInfo.FLAG_LARGE_HEAP != 0) flagList.add("LARGE_HEAP")
    if (flags and ApplicationInfo.FLAG_STOPPED != 0) flagList.add("STOPPED")
    if (flags and ApplicationInfo.FLAG_SUPPORTS_RTL != 0) flagList.add("SUPPORTS_RTL")
    if (flags and ApplicationInfo.FLAG_INSTALLED != 0) flagList.add("INSTALLED")
    if (flags and ApplicationInfo.FLAG_IS_DATA_ONLY != 0) flagList.add("IS_DATA_ONLY")
    return if (flagList.isEmpty()) "-" else flagList.joinToString(", ")
}

// ==================== Component List Content ====================

/**
 * 组件列表内容 - 与原 ComponentListFragment 完全一致
 */
@Composable
private fun ComponentListContent(
    context: Context,
    appItem: AppItem,
    componentType: ComponentType
) {
    val components = remember { mutableStateListOf<ComponentItem>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(appItem, componentType) {
        isLoading = true
        components.clear()

        withContext(Dispatchers.IO) {
            val packageInfo = appItem.getPackageInfo()
            val items = mutableListOf<ComponentItem>()

            when (componentType) {
                ComponentType.PERMISSION -> {
                    packageInfo.requestedPermissions?.forEach { permission ->
                        if (permission != null) {
                            items.add(ComponentItem(permission, null, false, false, null))
                        }
                    }
                }
                ComponentType.ACTIVITY -> {
                    packageInfo.activities?.forEach { activityInfo ->
                        items.add(ComponentItem(
                            activityInfo.name,
                            activityInfo.packageName,
                            true,
                            activityInfo.exported,
                            activityInfo.permission
                        ))
                    }
                }
                ComponentType.SERVICE -> {
                    packageInfo.services?.forEach { serviceInfo ->
                        items.add(ComponentItem(
                            serviceInfo.name,
                            serviceInfo.packageName,
                            true,
                            serviceInfo.exported,
                            serviceInfo.permission
                        ))
                    }
                }
                ComponentType.RECEIVER -> {
                    packageInfo.receivers?.forEach { receiverInfo ->
                        items.add(ComponentItem(
                            receiverInfo.name,
                            null,
                            false,
                            receiverInfo.exported,
                            receiverInfo.permission
                        ))
                    }
                }
                ComponentType.PROVIDER -> {
                    packageInfo.providers?.forEach { providerInfo ->
                        items.add(ComponentItem(
                            providerInfo.name,
                            null,
                            false,
                            providerInfo.exported,
                            providerInfo.readPermission
                        ))
                    }
                }
                ComponentType.STATIC_LOADER -> {
                    val bundle = appItem.getStaticReceiversBundle()
                    bundle.keySet().forEach { key ->
                        val filters = bundle.getStringArrayList(key)
                        val description = filters?.joinToString(", ") ?: ""
                        items.add(ComponentItem(key, description, false, false, null))
                    }
                }
            }

            withContext(Dispatchers.Main) {
                components.addAll(items)
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (components.isEmpty()) {
            Text(
                text = "暂无内容",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(components) { item ->
                    ComponentItemCard(
                        item = item,
                        componentType = componentType,
                        onClick = { copyToClipboard(context, item.name) },
                        onLongClick = {
                            handleComponentLongClick(context, item, componentType)
                        },
                        canLongClick = item.canLaunch
                    )
                }
            }
        }
    }
}

data class ComponentItem(
    val name: String,
    val packageName: String?,
    val canLaunch: Boolean,
    val isExported: Boolean,
    val permission: String?
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComponentItemCard(
    item: ComponentItem,
    componentType: ComponentType,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    canLongClick: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (canLongClick) onLongClick else null
            ),
        shape = RoundedCornerShape(AppDimens.Radius.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Space.lg)
        ) {
            // 组件名称
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 提示文字（Activity/Service）
            if (item.canLaunch) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "点击复制 · 长按启动",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Exported 状态
            if (item.isExported) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Exported: true",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50) // Green
                )
            } else if (item.canLaunch) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Exported: false",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Permission
            if (item.permission != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Permission: ${item.permission}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun handleComponentLongClick(
    context: Context,
    item: ComponentItem,
    componentType: ComponentType
) {
    val packageName = item.packageName ?: return

    when (componentType) {
        ComponentType.ACTIVITY -> {
            try {
                val intent = Intent()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.setClassName(packageName, item.name)
                context.startActivity(intent)
            } catch (e: Exception) {
                ToastManager.showToast(context, e.toString(), Toast.LENGTH_SHORT)
            }
        }
        ComponentType.SERVICE -> {
            try {
                val intent = Intent()
                intent.setClassName(packageName, item.name)
                context.startService(intent)
            } catch (e: Exception) {
                ToastManager.showToast(context, e.toString(), Toast.LENGTH_SHORT)
            }
        }
        else -> {}
    }
}

// ==================== Signature Content ====================

/**
 * 签名数据类
 */
data class SignatureInfo(
    val subject: String,
    val issuer: String,
    val serial: String,
    val notBefore: String,
    val notAfter: String,
    val md5: String,
    val sha1: String,
    val sha256: String
)

/**
 * 签名内容 - 与原 SignatureFragment 完全一致，纯 Compose 实现
 */
@Composable
private fun SignatureContent(appItem: AppItem) {
    val context = LocalContext.current
    var signatureInfo by remember { mutableStateOf<SignatureInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(appItem) {
        withContext(Dispatchers.IO) {
            try {
                val packageInfo = appItem.getPackageInfo()
                val sourceDir = packageInfo.applicationInfo?.sourceDir ?: ""

                // 获取签名信息
                val signInfos = EnvironmentUtil.getAPKSignInfo(sourceDir)
                val md5 = EnvironmentUtil.getSignatureMD5StringOfPackageInfo(packageInfo)
                val sha1 = EnvironmentUtil.getSignatureSHA1OfPackageInfo(packageInfo)
                val sha256 = EnvironmentUtil.getSignatureSHA256OfPackageInfo(packageInfo)

                withContext(Dispatchers.Main) {
                    signatureInfo = SignatureInfo(
                        subject = signInfos.getOrElse(0) { "" },
                        issuer = signInfos.getOrElse(1) { "" },
                        serial = signInfos.getOrElse(2) { "" },
                        notBefore = signInfos.getOrElse(3) { "" },
                        notAfter = signInfos.getOrElse(4) { "" },
                        md5 = md5,
                        sha1 = sha1,
                        sha256 = sha256
                    )
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppDimens.Space.lg)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 48.dp)
            )
        } else if (signatureInfo != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimens.Radius.xl),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    // 签名主题 (Subject)
                    SignatureItem(
                        label = stringResource(R.string.activity_detail_signature_issuer),
                        value = signatureInfo!!.subject,
                        onClick = { copyToClipboard(context, signatureInfo!!.subject) }
                    )

                    // 颁发者 (Issuer)
                    SignatureItem(
                        label = stringResource(R.string.activity_detail_signature_subject),
                        value = signatureInfo!!.issuer,
                        onClick = { copyToClipboard(context, signatureInfo!!.issuer) }
                    )

                    // 序列号
                    SignatureItem(
                        label = stringResource(R.string.activity_detail_signature_serial),
                        value = signatureInfo!!.serial,
                        onClick = { copyToClipboard(context, signatureInfo!!.serial) }
                    )

                    // 有效期开始
                    SignatureItem(
                        label = stringResource(R.string.activity_detail_signature_start),
                        value = signatureInfo!!.notBefore,
                        onClick = { copyToClipboard(context, signatureInfo!!.notBefore) }
                    )

                    // 有效期结束
                    SignatureItem(
                        label = stringResource(R.string.activity_detail_signature_end),
                        value = signatureInfo!!.notAfter,
                        onClick = { copyToClipboard(context, signatureInfo!!.notAfter) }
                    )

                    // MD5
                    SignatureItem(
                        label = stringResource(R.string.activity_detail_signature_md5),
                        value = signatureInfo!!.md5,
                        onClick = { copyToClipboard(context, signatureInfo!!.md5) }
                    )

                    // SHA1
                    SignatureItem(
                        label = stringResource(R.string.activity_detail_signature_sha1),
                        value = signatureInfo!!.sha1,
                        onClick = { copyToClipboard(context, signatureInfo!!.sha1) }
                    )

                    // SHA256
                    SignatureItem(
                        label = stringResource(R.string.activity_detail_signature_sha256),
                        value = signatureInfo!!.sha256,
                        onClick = { copyToClipboard(context, signatureInfo!!.sha256) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SignatureItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== Hash Content ====================

/**
 * Hash内容 - 与原 HashFragment 完全一致
 */
@Composable
private fun HashContent(appItem: AppItem) {
    val context = LocalContext.current
    val fileItem = appItem.getFileItem()

    var md5Hash by remember { mutableStateOf<String?>(null) }
    var sha1Hash by remember { mutableStateOf<String?>(null) }
    var sha256Hash by remember { mutableStateOf<String?>(null) }
    var crc32Hash by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(fileItem) {
        HashTask(fileItem, HashTask.HashType.MD5, object : HashTask.CompletedCallback {
            override fun onHashCompleted(result: String) {
                md5Hash = result
            }
        }).start()

        HashTask(fileItem, HashTask.HashType.SHA1, object : HashTask.CompletedCallback {
            override fun onHashCompleted(result: String) {
                sha1Hash = result
            }
        }).start()

        HashTask(fileItem, HashTask.HashType.SHA256, object : HashTask.CompletedCallback {
            override fun onHashCompleted(result: String) {
                sha256Hash = result
            }
        }).start()

        HashTask(fileItem, HashTask.HashType.CRC32, object : HashTask.CompletedCallback {
            override fun onHashCompleted(result: String) {
                crc32Hash = result
            }
        }).start()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppDimens.Space.lg)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.Radius.xl),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                // MD5
                HashItem(
                    label = "MD5",
                    value = md5Hash,
                    onClick = { copyToClipboard(context, md5Hash) }
                )
                InfoDivider()

                // SHA1
                HashItem(
                    label = "SHA1",
                    value = sha1Hash,
                    onClick = { copyToClipboard(context, sha1Hash) }
                )
                InfoDivider()

                // SHA256
                HashItem(
                    label = "SHA256",
                    value = sha256Hash,
                    onClick = { copyToClipboard(context, sha256Hash) }
                )
                InfoDivider()

                // CRC32
                HashItem(
                    label = "CRC32",
                    value = crc32Hash,
                    onClick = { copyToClipboard(context, crc32Hash) }
                )
            }
        }
    }
}

@Composable
private fun HashItem(
    label: String,
    value: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = value != null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ==================== Manifest Content ====================

/**
 * Manifest内容 - 与原 ManifestFragment 完全一致
 */
@Composable
private fun ManifestContent(appItem: AppItem) {
    val context = LocalContext.current
    var manifestContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(appItem) {
        withContext(Dispatchers.IO) {
            try {
                val zipFile = ZipFile(appItem.getSourcePath())
                val entry = zipFile.getEntry("AndroidManifest.xml")

                if (entry != null) {
                    val inputStream = zipFile.getInputStream(entry)
                    manifestContent = AXMLPrinter.decode(inputStream)
                    inputStream.close()
                } else {
                    error = "未找到 AndroidManifest.xml"
                }
                zipFile.close()
            } catch (e: Exception) {
                error = e.toString()
            }

            withContext(Dispatchers.Main) {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (error != null) {
            Text(
                text = error!!,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(AppDimens.Space.lg)
            )
        } else if (manifestContent != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(AppDimens.Space.lg)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDimens.Radius.xl),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Text(
                        text = manifestContent!!,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.Space.lg)
                    )
                }
            }
        }
    }
}

// ==================== SoLib Content ====================

/**
 * Native库内容 - 与原 SoLibFragment 完全一致
 */
data class SoLibItem(
    val name: String,
    val arch: String,
    val fullPath: String
)

@Composable
private fun SoLibContent(appItem: AppItem) {
    val context = LocalContext.current
    val libs = remember { mutableStateListOf<SoLibItem>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(appItem) {
        isLoading = true
        libs.clear()

        withContext(Dispatchers.IO) {
            val items = mutableListOf<SoLibItem>()

            try {
                val sourcePath = appItem.getSourcePath()
                if (sourcePath.isNotEmpty()) {
                    val zipFile = ZipFile(sourcePath)
                    val entries = zipFile.entries()

                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name

                        if (name.startsWith("lib/") && name.endsWith(".so")) {
                            val parts = name.split("/")
                            if (parts.size >= 3) {
                                val arch = parts[1]
                                val fileName = parts.last()
                                items.add(SoLibItem(fileName, arch, name))
                            }
                        }
                    }
                    zipFile.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            items.sortWith(compareBy({ it.arch }, { it.name }))

            withContext(Dispatchers.Main) {
                libs.addAll(items)
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (libs.isEmpty()) {
            Text(
                text = "暂无Native库",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(libs) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable { copyToClipboard(context, item.name) },
                        shape = RoundedCornerShape(AppDimens.Radius.md),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.Space.lg)
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.arch,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
