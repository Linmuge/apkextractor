package info.muge.appshare.ui.screens

import android.content.pm.PackageInfo
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
import java.util.zip.ZipFile

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
    val detailSortMode: DetailSortMode = DetailSortMode.COUNT_DESC
)

class StatisticsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private val cache = mutableMapOf<StatisticsType, Map<String, List<AppItem>>>()

    init {
        loadStatistics(forceRefresh = false)
    }

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
                
                // Double check if type hasn't changed during load
                if (currentState.currentType == _uiState.value.currentType) {
                    _uiState.update {
                        it.copy(
                            currentData = statistics,
                            lastUpdatedAt = System.currentTimeMillis()
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
            val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
            sdf.format(java.util.Date(installTimeMs))
        } catch (_: Exception) {
            "未知"
        }
    }
}

sealed class StatisticsEvent {
    data class ChangeType(val type: StatisticsType) : StatisticsEvent()
    data class ChangeLimit(val limit: Int) : StatisticsEvent()
    data class ChangeSortMode(val mode: DetailSortMode) : StatisticsEvent()
    object ToggleHideSingleItems : StatisticsEvent()
    object Refresh : StatisticsEvent()
}
