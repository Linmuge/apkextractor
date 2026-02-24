package info.muge.appshare.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.muge.appshare.Constants
import info.muge.appshare.Global
import info.muge.appshare.items.AppItem
import info.muge.appshare.tasks.RefreshInstalledListTask
import info.muge.appshare.tasks.SearchAppItemTask
import info.muge.appshare.utils.SPUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * AppListScreen 的 UI 状态
 */
data class AppListUiState(
    val appList: List<AppItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val loadingCurrent: Int = 0,
    val loadingTotal: Int = 0,
    val hasPermission: Boolean = false,
    val selectedItems: Set<String> = emptySet(),
    val selectedSize: Long = 0L,
    val highlightKeyword: String? = null
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
                appList = emptyList()
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
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    appList = appList
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
                appList = emptyList()
            )
        }

        searchJob = viewModelScope.launch {
            val task = SearchAppItemTask(Global.app_list.toList(), keyword)
            val results = task.execute()
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    appList = results
                )
            }
        }
    }

    /**
     * 退出搜索模式
     */
    fun exitSearchMode() {
        _uiState.update {
            it.copy(
                highlightKeyword = null,
                appList = Global.app_list.toList()
            )
        }
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
        _uiState.update { it.copy(appList = Global.app_list.toList()) }
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
