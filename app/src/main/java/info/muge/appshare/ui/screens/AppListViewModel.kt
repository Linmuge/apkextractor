package info.muge.appshare.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.muge.appshare.Constants
import info.muge.appshare.Global
import info.muge.appshare.items.AppItem
import info.muge.appshare.tasks.RefreshInstalledListTask
import info.muge.appshare.tasks.SearchAppItemTask
import info.muge.appshare.utils.PinyinUtil
import info.muge.appshare.utils.SPUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

/**
 * 应用类型筛选
 */
enum class AppTypeFilter(val label: String) {
    ALL("全部"),
    USER("用户应用"),
    SYSTEM("系统应用")
}

/**
 * 大小范围筛选
 */
enum class SizeRange(val label: String, val minBytes: Long, val maxBytes: Long) {
    ALL("全部", 0L, Long.MAX_VALUE),
    LESS_1MB("< 1 MB", 0L, 1L * 1024 * 1024),
    MB_1_10("1 - 10 MB", 1L * 1024 * 1024, 10L * 1024 * 1024),
    MB_10_50("10 - 50 MB", 10L * 1024 * 1024, 50L * 1024 * 1024),
    MB_50_100("50 - 100 MB", 50L * 1024 * 1024, 100L * 1024 * 1024),
    MB_100_500("100 - 500 MB", 100L * 1024 * 1024, 500L * 1024 * 1024),
    MORE_500MB("> 500 MB", 500L * 1024 * 1024, Long.MAX_VALUE)
}

/**
 * 筛选配置
 */
data class FilterConfig(
    val appType: AppTypeFilter = AppTypeFilter.ALL,
    val sizeRange: SizeRange = SizeRange.ALL,
    val installerSources: Set<String> = emptySet()
) {
    val isActive: Boolean
        get() = appType != AppTypeFilter.ALL ||
                sizeRange != SizeRange.ALL ||
                installerSources.isNotEmpty()

    val activeCount: Int
        get() {
            var count = 0
            if (appType != AppTypeFilter.ALL) count++
            if (sizeRange != SizeRange.ALL) count++
            if (installerSources.isNotEmpty()) count++
            return count
        }
}

/**
 * 分组模式
 */
enum class GroupMode(val label: String) {
    NONE("不分组"),
    BY_TYPE("按类型"),
    BY_INSTALLER("按安装来源"),
    BY_UPDATE_TIME("按更新时间"),
    BY_SIZE("按大小范围"),
    BY_FIRST_LETTER("按首字母")
}

/**
 * AppListScreen 的 UI 状态
 */
data class AppListUiState(
    val appList: List<AppItem> = emptyList(),
    val groupedAppList: Map<String, List<AppItem>> = emptyMap(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val loadingCurrent: Int = 0,
    val loadingTotal: Int = 0,
    val hasPermission: Boolean = false,
    val selectedItems: Set<String> = emptySet(),
    val selectedSize: Long = 0L,
    val highlightKeyword: String? = null,
    val filterConfig: FilterConfig = FilterConfig(),
    val groupMode: GroupMode = GroupMode.NONE,
    val availableInstallers: List<String> = emptyList()
)

/**
 * AppListScreen 的 ViewModel
 */
class AppListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var searchJob: Job? = null

    /**
     * 初始化权限状态
     */
    fun initPermission(context: Context) {
        val hasPermission = SPUtil.getGlobalSharedPreferences(context)
            .getBoolean("show_app", false)
        _uiState.update { it.copy(hasPermission = hasPermission) }
    }

    /**
     * 刷新应用列表
     */
    fun refreshAppList(context: Context) {
        if (!_uiState.value.hasPermission) return

        refreshJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                loadingCurrent = 0,
                loadingTotal = 0,
                appList = emptyList(),
                groupedAppList = emptyMap()
            )
        }

        refreshJob = viewModelScope.launch {
            val task = RefreshInstalledListTask(context)
            val appList = task.execute(
                onProgressStarted = { total ->
                    _uiState.update { it.copy(loadingTotal = total) }
                },
                onProgressUpdated = { current, total ->
                    _uiState.update { it.copy(loadingCurrent = current, loadingTotal = total) }
                }
            )

            // 收集所有安装来源
            val installers = appList.map { it.getInstallSource() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            val currentState = _uiState.value
            val filtered = applyFilter(appList, currentState.filterConfig)
            val grouped = applyGroup(filtered, currentState.groupMode)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    appList = filtered,
                    groupedAppList = grouped,
                    availableInstallers = installers
                )
            }
        }
    }

    /**
     * 搜索应用
     */
    fun searchApps(keyword: String) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                isRefreshing = true,
                highlightKeyword = keyword,
                appList = emptyList(),
                groupedAppList = emptyMap()
            )
        }

        searchJob = viewModelScope.launch {
            val task = SearchAppItemTask(Global.app_list.toList(), keyword)
            val results = task.execute()
            val currentState = _uiState.value
            val filtered = applyFilter(results, currentState.filterConfig)
            val grouped = applyGroup(filtered, currentState.groupMode)
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    appList = filtered,
                    groupedAppList = grouped
                )
            }
        }
    }

    /**
     * 退出搜索模式
     */
    fun exitSearchMode() {
        val currentState = _uiState.value
        val source = Global.app_list.toList()
        val filtered = applyFilter(source, currentState.filterConfig)
        val grouped = applyGroup(filtered, currentState.groupMode)
        _uiState.update {
            it.copy(
                highlightKeyword = null,
                appList = filtered,
                groupedAppList = grouped
            )
        }
    }

    /**
     * 更新筛选配置
     */
    fun updateFilter(filterConfig: FilterConfig) {
        val source = Global.app_list.toList()
        val filtered = applyFilter(source, filterConfig)
        val grouped = applyGroup(filtered, _uiState.value.groupMode)
        _uiState.update {
            it.copy(
                filterConfig = filterConfig,
                appList = filtered,
                groupedAppList = grouped
            )
        }
    }

    /**
     * 重置筛选
     */
    fun resetFilter() {
        updateFilter(FilterConfig())
    }

    /**
     * 设置分组模式
     */
    fun setGroupMode(mode: GroupMode) {
        val grouped = applyGroup(_uiState.value.appList, mode)
        _uiState.update {
            it.copy(
                groupMode = mode,
                groupedAppList = grouped
            )
        }
    }

    /**
     * 应用筛选逻辑
     */
    private fun applyFilter(list: List<AppItem>, config: FilterConfig): List<AppItem> {
        if (!config.isActive) return list

        return list.filter { app ->
            // 应用类型筛选
            val typeMatch = when (config.appType) {
                AppTypeFilter.ALL -> true
                AppTypeFilter.USER -> !app.isRedMarked()
                AppTypeFilter.SYSTEM -> app.isRedMarked()
            }

            // 大小范围筛选
            val sizeMatch = app.getSize() in config.sizeRange.minBytes until config.sizeRange.maxBytes

            // 安装来源筛选
            val installerMatch = config.installerSources.isEmpty() ||
                    config.installerSources.contains(app.getInstallSource())

            typeMatch && sizeMatch && installerMatch
        }
    }

    /**
     * 应用分组逻辑
     */
    private fun applyGroup(list: List<AppItem>, mode: GroupMode): Map<String, List<AppItem>> {
        if (mode == GroupMode.NONE) return emptyMap()

        return list.groupBy { app ->
            when (mode) {
                GroupMode.NONE -> ""
                GroupMode.BY_TYPE -> if (app.isRedMarked()) "系统应用" else "用户应用"
                GroupMode.BY_INSTALLER -> app.getInstallSource().ifBlank { "未知来源" }
                GroupMode.BY_UPDATE_TIME -> {
                    val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    try {
                        sdf.format(Date(app.getPackageInfo().lastUpdateTime))
                    } catch (_: Exception) {
                        "未知"
                    }
                }
                GroupMode.BY_SIZE -> {
                    val mb = app.getSize() / (1024.0 * 1024.0)
                    when {
                        mb < 1 -> "< 1 MB"
                        mb < 10 -> "1 - 10 MB"
                        mb < 50 -> "10 - 50 MB"
                        mb < 100 -> "50 - 100 MB"
                        mb < 500 -> "100 - 500 MB"
                        else -> "> 500 MB"
                    }
                }
                GroupMode.BY_FIRST_LETTER -> {
                    try {
                        val pinyin = PinyinUtil.getFirstSpell(app.getAppName())
                        val first = pinyin.firstOrNull()?.uppercaseChar() ?: '#'
                        if (first in 'A'..'Z') first.toString() else "#"
                    } catch (_: Exception) {
                        "#"
                    }
                }
            }
        }.toSortedMap()
    }

    /**
     * 授予权限
     */
    fun grantPermission(context: Context) {
        SPUtil.getGlobalSharedPreferences(context)
            .edit()
            .putBoolean("show_app", true)
            .apply()
        _uiState.update { it.copy(hasPermission = true) }
        refreshAppList(context)
    }

    /**
     * 切换选中项
     */
    fun toggleSelection(app: AppItem) {
        val pkgName = app.getPackageName()
        _uiState.update { state ->
            val newSelected = state.selectedItems.toMutableSet()
            val newSize = if (newSelected.contains(pkgName)) {
                newSelected.remove(pkgName)
                state.selectedSize - app.getSize()
            } else {
                newSelected.add(pkgName)
                state.selectedSize + app.getSize()
            }
            state.copy(selectedItems = newSelected, selectedSize = newSize)
        }
    }

    /**
     * 全选/取消全选
     */
    fun toggleSelectAll() {
        _uiState.update { state ->
            if (state.selectedItems.size == state.appList.size) {
                state.copy(selectedItems = emptySet(), selectedSize = 0L)
            } else {
                val allPkgs = state.appList.map { it.getPackageName() }.toSet()
                val totalSize = state.appList.sumOf { it.getSize() }
                state.copy(selectedItems = allPkgs, selectedSize = totalSize)
            }
        }
    }

    /**
     * 反选
     */
    fun invertSelection() {
        _uiState.update { state ->
            val allPkgs = state.appList.map { it.getPackageName() }.toSet()
            val inverted = allPkgs - state.selectedItems
            val invertedSize = state.appList
                .filter { inverted.contains(it.getPackageName()) }
                .sumOf { it.getSize() }
            state.copy(selectedItems = inverted, selectedSize = invertedSize)
        }
    }

    /**
     * 清空选择
     */
    fun clearSelection() {
        _uiState.update { it.copy(selectedItems = emptySet(), selectedSize = 0L) }
    }

    /**
     * 排序
     */
    fun sortApps(sortValue: Int) {
        AppItem.sort_config = sortValue
        synchronized(Global.app_list) {
            Collections.sort(Global.app_list)
        }
        val currentState = _uiState.value
        val source = Global.app_list.toList()
        val filtered = applyFilter(source, currentState.filterConfig)
        val grouped = applyGroup(filtered, currentState.groupMode)
        _uiState.update { it.copy(appList = filtered, groupedAppList = grouped) }
    }

    /**
     * 是否有选中项
     */
    fun hasSelection(): Boolean = _uiState.value.selectedItems.isNotEmpty()

    // 应用变更广播监听
    private var packageChangeReceiver: BroadcastReceiver? = null

    /**
     * 注册应用安装/卸载/更新广播监听
     */
    fun registerPackageChangeListener(context: Context) {
        if (packageChangeReceiver != null) return

        packageChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                // 忽略自身包名的变化
                val changedPkg = intent?.data?.schemeSpecificPart
                if (changedPkg == context.packageName) return

                // 记录变更
                val action = intent?.action
                if (changedPkg != null && action != null) {
                    val changeType = when (action) {
                        Intent.ACTION_PACKAGE_ADDED -> {
                            if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))
                                info.muge.appshare.data.ChangeType.UPDATED
                            else
                                info.muge.appshare.data.ChangeType.INSTALLED
                        }
                        Intent.ACTION_PACKAGE_REMOVED -> {
                            if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))
                                return // PACKAGE_REMOVED + REPLACING = 更新前的移除，忽略
                            else
                                info.muge.appshare.data.ChangeType.UNINSTALLED
                        }
                        else -> return
                    }

                    // 获取应用名（已卸载的无法获取名称）
                    val appName = try {
                        val pm = context.packageManager
                        val appInfo = pm.getApplicationInfo(changedPkg, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        changedPkg
                    }

                    // 获取版本号
                    val versionName = try {
                        context.packageManager.getPackageInfo(changedPkg, 0).versionName
                    } catch (_: Exception) {
                        null
                    }

                    info.muge.appshare.data.AppChangeRepository.addRecord(
                        context,
                        info.muge.appshare.data.AppChangeRecord(
                            packageName = changedPkg,
                            appName = appName,
                            changeType = changeType,
                            versionName = versionName
                        )
                    )
                }

                // 自动刷新列表
                if (_uiState.value.hasPermission && !_uiState.value.isLoading) {
                    refreshAppList(context)
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(packageChangeReceiver, filter)
    }

    /**
     * 注销广播监听
     */
    fun unregisterPackageChangeListener(context: Context) {
        packageChangeReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) { }
            packageChangeReceiver = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
        searchJob?.cancel()
    }
}
