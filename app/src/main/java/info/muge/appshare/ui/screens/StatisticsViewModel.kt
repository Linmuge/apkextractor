package info.muge.appshare.ui.screens

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.text.format.Formatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.muge.appshare.Global
import info.muge.appshare.items.AppItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile

/**
 * 存储空间概览
 */
data class StorageOverview(
    val totalSize: Long = 0L,
    val avgSize: Long = 0L,
    val medianSize: Long = 0L,
    val maxSize: Long = 0L,
    val maxSizeApp: String = "",
    val appCount: Int = 0,
    val top10Apps: List<Pair<String, Long>> = emptyList()
)

/**
 * 安装趋势数据点
 */
data class InstallTrendPoint(
    val label: String,
    val count: Int
)

/**
 * Split APK 统计概览
 */
data class SplitApkStats(
    val totalApps: Int = 0,
    val splitAppCount: Int = 0,
    val totalSplitCount: Int = 0,
    val splitTypes: Map<String, Int> = emptyMap() // e.g. "config.xxhdpi" -> 5
)

/**
 * StatisticsViewModel 状态
 */
data class StatisticsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loadError: String? = null,
    val currentData: Map<String, List<AppItem>> = emptyMap(),
    val lastUpdatedAt: Long? = null,
    val sourceSignature: Long = 0L,
    val currentType: StatisticsType = StatisticsType.TARGET_SDK,
    val displayLimit: Int = 10,
    val hideSingleItems: Boolean = false,
    val detailSortMode: DetailSortMode = DetailSortMode.COUNT_DESC,
    val storageOverview: StorageOverview = StorageOverview(),
    val installTrend: List<InstallTrendPoint> = emptyList(),
    val splitApkStats: SplitApkStats = SplitApkStats()
)

class StatisticsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private val cache = mutableMapOf<StatisticsType, Map<String, List<AppItem>>>()

    // 使用 ThreadLocal 缓存 SimpleDateFormat 避免并发问题和反复创建
    private val monthFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM", Locale.getDefault())
        }
    }

    init {
        loadStatistics(forceRefresh = false)
    }

    /**
     * 设置 Context 用于权限查询
     */
    fun setContext(context: android.content.Context) {
        appContext = context.applicationContext
    }

    private var appContext: android.content.Context? = null

    fun onEvent(event: StatisticsEvent) {
        when (event) {
            is StatisticsEvent.ChangeType -> {
                _uiState.update { it.copy(currentType = event.type) }
                loadStatistics(forceRefresh = false)
            }
            is StatisticsEvent.ChangeLimit -> {
                _uiState.update { it.copy(displayLimit = event.limit) }
            }
            is StatisticsEvent.ToggleHideSingleItems -> {
                _uiState.update { it.copy(hideSingleItems = !it.hideSingleItems) }
            }
            is StatisticsEvent.ChangeSortMode -> {
                _uiState.update { it.copy(detailSortMode = event.mode) }
            }
            is StatisticsEvent.Refresh -> {
                loadStatistics(forceRefresh = true)
            }
        }
    }

    private fun loadStatistics(forceRefresh: Boolean) {
        val currentState = _uiState.value
        loadJob?.cancel()
        
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, loadError = null) }
            
            try {
                val appList = withContext(Dispatchers.Default) {
                    synchronized(Global.app_list) {
                        Global.app_list.toList()
                    }
                }

                val signature = withContext(Dispatchers.Default) {
                    calculateSourceSignature(appList)
                }

                if (signature != currentState.sourceSignature) {
                    _uiState.update { it.copy(sourceSignature = signature) }
                    cache.clear()
                }

                if (!forceRefresh) {
                    cache[currentState.currentType]?.let { cachedData ->
                        _uiState.update {
                            it.copy(
                                currentData = cachedData,
                                isRefreshing = false,
                                isLoading = false
                            )
                        }
                        return@launch
                    }
                }

                val statistics = withContext(Dispatchers.Default) {
                    collectStatistics(currentState.currentType, appList)
                }

                cache[currentState.currentType] = statistics
                
                // 计算存储分析和安装趋势
                val storageOverview = withContext(Dispatchers.Default) {
                    calculateStorageOverview(appList)
                }
                val installTrend = withContext(Dispatchers.Default) {
                    calculateInstallTrend(appList)
                }
                val splitApkStats = withContext(Dispatchers.Default) {
                    calculateSplitApkStats(appList)
                }

                // Double check if type hasn't changed during load
                if (currentState.currentType == _uiState.value.currentType) {
                    _uiState.update {
                        it.copy(
                            currentData = statistics,
                            lastUpdatedAt = System.currentTimeMillis(),
                            storageOverview = storageOverview,
                            installTrend = installTrend,
                            splitApkStats = splitApkStats
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(loadError = e.message ?: "加载失败") }
            } finally {
                if (currentState.currentType == _uiState.value.currentType) {
                    _uiState.update { it.copy(isRefreshing = false, isLoading = false) }
                }
            }
        }
    }

    private fun calculateSourceSignature(appList: List<AppItem>): Long {
        var signature = appList.size.toLong()
        for (app in appList) {
            signature = 31L * signature + app.getPackageName().hashCode().toLong()
            signature = 31L * signature + app.getSize()
        }
        return signature
    }

    private fun collectStatistics(
        type: StatisticsType,
        appList: List<AppItem>
    ): Map<String, List<AppItem>> {
        val result = mutableMapOf<String, MutableList<AppItem>>()

        for (app in appList) {
            try {
                val packageInfo = app.getPackageInfo()
                val appInfo = packageInfo.applicationInfo ?: continue

                val key = when (type) {
                    StatisticsType.TARGET_SDK -> "API ${appInfo.targetSdkVersion}"
                    StatisticsType.MIN_SDK -> "API ${appInfo.minSdkVersion}"
                    StatisticsType.COMPILE_SDK -> getCompileSdkLabel(packageInfo)
                    StatisticsType.KOTLIN -> if (hasKotlinClasses(app)) "有 Kotlin" else "无 Kotlin"
                    StatisticsType.ABI -> getAbiLabel(app)
                    StatisticsType.PAGE_SIZE_16K -> if (is16kPageSize(app)) "可能支持 16K" else "可能不支持 16K"
                    StatisticsType.APP_BUNDLE -> if (isAppBundle(app)) "Split APK / Bundle" else "单 APK"
                    StatisticsType.INSTALLER -> app.getInstallSource().ifBlank { "未知来源" }
                    StatisticsType.APP_TYPE -> if (app.isRedMarked()) "系统应用" else "用户应用"
                StatisticsType.SIZE_DISTRIBUTION -> getSizeDistributionLabel(app.getSize())
                StatisticsType.INSTALL_TIME -> getInstallTimeLabel(packageInfo.firstInstallTime)
                StatisticsType.EXPORT_STATS -> continue // 导出统计有独立卡片，不走分组逻辑
                StatisticsType.PERMISSION -> {
                    // 权限统计：每个权限对应使用该权限的应用列表
                    try {
                        val ctx = appContext ?: continue
                        val pkgInfo = ctx.packageManager.getPackageInfo(
                            app.getPackageName(),
                            PackageManager.GET_PERMISSIONS
                        )
                        pkgInfo.requestedPermissions
                            ?.map { it.substringAfterLast('.') }
                            ?.distinct()
                            ?.forEach { shortName ->
                                val list = result.getOrPut(shortName) { mutableListOf() }
                                if (list.none { it.getPackageName() == app.getPackageName() }) {
                                    list.add(app)
                                }
                            }
                    } catch (_: Exception) { }
                    continue
                }
                }

                result.getOrPut(key) { mutableListOf() }.add(app)
            } catch (_: Exception) {
                // ignore single app failures
            }
        }

        return result
    }

    private fun hasKotlinClasses(app: AppItem): Boolean {
        return try {
            val paths = buildList {
                add(app.getSourcePath())
                app.getSplitSourceDirs()?.let { addAll(it) }
            }
            paths.any(::containsKotlinMetadata)
        } catch (_: Exception) {
            false
        }
    }

    private fun getAbis(app: AppItem): List<String> {
        return try {
            val abiSet = linkedSetOf<String>()
            val paths = buildList {
                add(app.getSourcePath())
                app.getSplitSourceDirs()?.let { addAll(it) }
            }
            for (path in paths) {
                ZipFile(path).use { zip ->
                    zip.entries().asSequence()
                        .map { it.name }
                        .filter { it.startsWith("lib/") && it.count { ch -> ch == '/' } >= 2 }
                        .forEach { entryName ->
                            val abi = entryName.substringAfter("lib/").substringBefore('/')
                            if (abi.isNotBlank()) {
                                abiSet.add(abi)
                            }
                        }
                }
            }
            abiSet.toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun is16kPageSize(app: AppItem): Boolean {
        val abis = getAbis(app)
        return abis.isEmpty() || "arm64-v8a" in abis
    }

    private fun isAppBundle(app: AppItem): Boolean {
        return try {
            app.getSplitSourceDirs()?.isNotEmpty() == true ||
                app.getSourcePath().contains("split_config")
        } catch (_: Exception) {
            false
        }
    }

    private fun getCompileSdkLabel(packageInfo: PackageInfo): String {
        val appInfo = packageInfo.applicationInfo ?: return "未知"
        val sdk = runCatching {
            // Android 12+ (API 31+) ApplicationInfo has compileSdkVersion
            android.content.pm.ApplicationInfo::class.java
                .getMethod("getCompileSdkVersion")
                .invoke(appInfo) as Int
        }.recoverCatching {
            // Fallback to older reflection (might be restricted/hidden on newer Android versions)
            PackageInfo::class.java
                .getField("compileSdkVersion")
                .getInt(packageInfo)
        }.getOrDefault(-1)
        
        return if (sdk > 0) "API $sdk" else "未知"
    }

    private fun getAbiLabel(app: AppItem): String {
        val abis = getAbis(app)
        if (abis.isEmpty()) return "无 Native"
        return abis.joinToString(" + ")
    }

    private fun containsKotlinMetadata(apkPath: String): Boolean {
        return runCatching {
            ZipFile(apkPath).use { zip ->
                zip.entries().asSequence().any { entry ->
                    val name = entry.name
                    name.endsWith(".kotlin_module") ||
                        name.contains("kotlin", ignoreCase = true) && (
                        name.startsWith("META-INF/") ||
                            name.startsWith("assets/")
                        )
                }
            }
        }.getOrDefault(false)
    }

    private fun getSizeDistributionLabel(sizeBytes: Long): String {
        val mb = sizeBytes / (1024.0 * 1024.0)
        return when {
            mb < 1 -> "< 1 MB"
            mb < 10 -> "1 - 10 MB"
            mb < 50 -> "10 - 50 MB"
            mb < 100 -> "50 - 100 MB"
            mb < 500 -> "100 - 500 MB"
            else -> "> 500 MB"
        }
    }

    private fun getInstallTimeLabel(installTimeMs: Long): String {
        return try {
            monthFormatter.get()?.format(Date(installTimeMs)) ?: "未知"
        } catch (_: Exception) {
            "未知"
        }
    }

    /**
     * 计算存储空间概览
     */
    private fun calculateStorageOverview(appList: List<AppItem>): StorageOverview {
        if (appList.isEmpty()) return StorageOverview()

        val sizes = appList.map { it.getSize() }
        val sorted = sizes.sorted()
        val totalSize = sizes.sum()
        val avgSize = totalSize / appList.size
        val medianSize = sorted[sorted.size / 2]
        val maxApp = appList.maxByOrNull { it.getSize() }

        val top10 = appList.sortedByDescending { it.getSize() }
            .take(10)
            .map { it.getAppName() to it.getSize() }

        return StorageOverview(
            totalSize = totalSize,
            avgSize = avgSize,
            medianSize = medianSize,
            maxSize = maxApp?.getSize() ?: 0L,
            maxSizeApp = maxApp?.getAppName() ?: "",
            appCount = appList.size,
            top10Apps = top10
        )
    }

    /**
     * 计算安装趋势（按月分组）
     */
    private fun calculateInstallTrend(appList: List<AppItem>): List<InstallTrendPoint> {
        val grouped = appList.groupBy { app ->
            try {
                monthFormatter.get()?.format(Date(app.getPackageInfo().firstInstallTime)) ?: "未知"
            } catch (_: Exception) {
                "未知"
            }
        }.filterKeys { it != "未知" }

        return grouped.entries
            .sortedBy { it.key }
            .takeLast(12) // 最近12个月
            .map { InstallTrendPoint(it.key, it.value.size) }
    }

    /**
     * 计算 Split APK 统计
     */
    private fun calculateSplitApkStats(appList: List<AppItem>): SplitApkStats {
        var splitAppCount = 0
        var totalSplitCount = 0
        val typeCounter = mutableMapOf<String, Int>()

        for (app in appList) {
            try {
                val splits = app.getSplitSourceDirs()
                if (!splits.isNullOrEmpty()) {
                    splitAppCount++
                    totalSplitCount += splits.size
                    splits.forEach { splitPath ->
                        val fileName = splitPath.substringAfterLast('/')
                        val typeName = when {
                            fileName.contains("config.") -> {
                                val config = fileName.substringAfter("config.").substringBefore(".")
                                "config.$config"
                            }
                            fileName.contains("split_") -> {
                                fileName.substringBefore(".apk").substringBefore(".").take(30)
                            }
                            else -> "其他"
                        }
                        typeCounter[typeName] = (typeCounter[typeName] ?: 0) + 1
                    }
                }
            } catch (_: Exception) { }
        }

        return SplitApkStats(
            totalApps = appList.size,
            splitAppCount = splitAppCount,
            totalSplitCount = totalSplitCount,
            splitTypes = typeCounter.entries
                .sortedByDescending { it.value }
                .take(15)
                .associate { it.key to it.value }
        )
    }
}

sealed class StatisticsEvent {
    data class ChangeType(val type: StatisticsType) : StatisticsEvent()
    data class ChangeLimit(val limit: Int) : StatisticsEvent()
    data class ChangeSortMode(val mode: DetailSortMode) : StatisticsEvent()
    object ToggleHideSingleItems : StatisticsEvent()
    object Refresh : StatisticsEvent()
}
