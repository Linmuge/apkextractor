package info.muge.appshare.items

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import info.muge.appshare.DisplayItem
import info.muge.appshare.R
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.FileUtil
import info.muge.appshare.utils.PinyinUtil
import java.io.File

/**
 * 单个应用项的所有信息
 */
class AppItem : Comparable<AppItem>, DisplayItem {

    companion object {
        @Transient
        var sort_config = 0
    }

    private val info: PackageInfo
    private val fileItem: FileItem

    /**
     * 程序名
     */
    private val title: String

    /**
     * 应用图标
     */
    private val drawable: Drawable

    /**
     * 应用大小
     */
    private val size: Long

    private val installSource: String
    private val launchingClass: String
    private val static_receivers_bundle: Bundle
    private val splitSourceDirs: Array<String>?

    // 仅当构造ExportTask时用
    @Transient
    var exportData = false
    @Transient
    var exportObb = false

    // 缓存拼音计算结果，避免重复计算
    @Transient
    private var cachedPinyin: String? = null

    /**
     * 初始化一个全新的AppItem
     * @param context context实例，用来获取应用图标、名称等参数
     * @param info PackageInfo实例，对应的本AppItem的信息
     */
    constructor(context: Context, info: PackageInfo) {
        val packageManager = context.applicationContext.packageManager
        this.info = info
        this.fileItem = FileItem(File(info.applicationInfo!!.sourceDir))
        this.title = packageManager.getApplicationLabel(info.applicationInfo!!).toString()
        this.size = FileUtil.getFileOrFolderSize(File(info.applicationInfo!!.sourceDir))
        this.drawable = packageManager.getApplicationIcon(info.applicationInfo!!)
        
        var install_source = context.resources.getString(R.string.word_unknown)
        try {
            val installer_package_name = packageManager.getInstallerPackageName(info.packageName!!)
            val installer_name = EnvironmentUtil.getAppNameByPackageName(context, installer_package_name ?: "")
            install_source = when {
                !TextUtils.isEmpty(installer_name) -> installer_name!!
                !TextUtils.isEmpty(installer_package_name) -> installer_package_name!!
                else -> context.resources.getString(R.string.word_unknown)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        this.installSource = install_source

        var launchingClass = context.resources.getString(R.string.word_unknown)
        try {
            val intent = packageManager.getLaunchIntentForPackage(info.packageName!!)
            launchingClass = if (intent == null) {
                context.resources.getString(R.string.word_none)
            } else {
                intent.component!!.className
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        this.launchingClass = launchingClass
        this.static_receivers_bundle = EnvironmentUtil.getStaticRegisteredReceiversOfBundleTypeForPackageName(context, info.packageName!!)
        this.splitSourceDirs = info.applicationInfo?.splitSourceDirs
    }

    /**
     * 构造一个本Item的副本，用于ExportTask导出应用。
     * @param wrapper 用于创造副本的目标
     * @param flag_data 指定是否导出data
     * @param flag_obb 指定是否导出obb
     */
    constructor(wrapper: AppItem, flag_data: Boolean, flag_obb: Boolean) {
        this.title = wrapper.title
        this.size = wrapper.size
        this.info = wrapper.info
        this.drawable = wrapper.drawable
        this.installSource = wrapper.installSource
        this.launchingClass = wrapper.launchingClass
        this.exportData = flag_data
        this.exportObb = flag_obb
        this.fileItem = wrapper.fileItem
        this.static_receivers_bundle = wrapper.static_receivers_bundle
        this.splitSourceDirs = wrapper.splitSourceDirs
    }

    /**
     * 构造一个AppItem，用于显示未安装的APK文件信息
     */
    constructor(context: Context, filePath: String) {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageArchiveInfo(filePath, 0)

        // 由于是未安装的APK，我们需要手动设置 applicationInfo（安全地处理空值）
        packageInfo?.applicationInfo?.let {
            it.sourceDir = filePath
            it.publicSourceDir = filePath
        }

        this.info = packageInfo ?: android.content.pm.PackageInfo().apply { packageName = "unknown" }
        this.fileItem = FileItem(File(filePath))
        
        // 尝试获取应用名称和图标
        // 注意：未安装的应用可能无法直接通过 getApplicationLabel 获取正确的名称
        // 需要创建一个 assetManager 来读取资源
        var appName = packageInfo?.packageName
        var appIcon = packageManager.defaultActivityIcon
        
        try {
            val appInfo = packageInfo?.applicationInfo
            if (appInfo != null) {
                appInfo.sourceDir = filePath
                appInfo.publicSourceDir = filePath
                
                appName = packageManager.getApplicationLabel(appInfo).toString()
                appIcon = packageManager.getApplicationIcon(appInfo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        this.title = appName ?: ""
        this.drawable = appIcon
        this.size = File(filePath).length()
        this.installSource = "External File"
        this.launchingClass = ""
        this.static_receivers_bundle = Bundle()
        this.splitSourceDirs = null
    }

    override fun getIconDrawable(): Drawable = drawable

    override fun getTitle(): String = "$title(${getVersionName()})"

    override fun getDescription(): String = info.packageName ?: ""

    override fun isRedMarked(): Boolean = (info.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) > 0

    /**
     * 获取应用图标
     */
    fun getIcon(): Drawable = drawable

    /**
     * 获取应用名称
     */
    fun getAppName(): String = title

    /**
     * 获取包名
     */
    fun getPackageName(): String = info.packageName ?: ""

    /**
     * 获取应用源路径
     */
    fun getSourcePath(): String = info.applicationInfo!!.sourceDir.toString()

    /**
     * 获取应用大小（源文件），单位字节
     */
    override fun getSize(): Long = size

    /**
     * 获取应用版本名称
     */
    fun getVersionName(): String = info.versionName ?: ""

    /**
     * 获取应用版本号
     */
    fun getVersionCode(): Int = info.versionCode

    /**
     * 获取本应用Item对应的PackageInfo实例
     */
    fun getPackageInfo(): PackageInfo = info

    fun getInstallSource(): String = installSource

    fun getLaunchingClass(): String = launchingClass

    fun getFileItem(): FileItem = fileItem

    fun getStaticReceiversBundle(): Bundle = static_receivers_bundle

    fun getSplitSourceDirs(): Array<String>? = splitSourceDirs

    /**
     * 获取缓存的拼音首字母，首次调用时计算并缓存
     */
    fun getCachedPinyin(): String {
        return cachedPinyin ?: run {
            val pinyin = try {
                PinyinUtil.getFirstSpell(title)
            } catch (e: Exception) {
                e.printStackTrace()
                title
            }
            cachedPinyin = pinyin
            pinyin
        }
    }

    /**
     * 排序模式。
     * 0 - 默认
     * 1 - 名称升序
     * 2 - 名称降序
     * 3 - 大小升序
     * 4 - 大小降序
     * 5 - 更新日期升序
     * 6 - 更新日期降序
     * 7 - 安装日期升序
     * 8 - 安装日期降序
     * 9 - 包名升序
     * 10 - 包名降序
     */
    override fun compareTo(other: AppItem): Int {
        return when (sort_config) {
            1 -> {
                try {
                    getCachedPinyin().lowercase()
                        .compareTo(other.getCachedPinyin().lowercase())
                } catch (e: Exception) {
                    e.printStackTrace()
                    0
                }
            }
            2 -> {
                try {
                    other.getCachedPinyin().lowercase()
                        .compareTo(getCachedPinyin().lowercase())
                } catch (e: Exception) {
                    e.printStackTrace()
                    0
                }
            }
            3 -> {
                when {
                    size - other.size > 0 -> 1
                    size - other.size < 0 -> -1
                    else -> 0
                }
            }
            4 -> {
                when {
                    size - other.size < 0 -> 1
                    size - other.size > 0 -> -1
                    else -> 0
                }
            }
            5 -> {
                when {
                    info.lastUpdateTime - other.info.lastUpdateTime > 0 -> 1
                    info.lastUpdateTime - other.info.lastUpdateTime < 0 -> -1
                    else -> 0
                }
            }
            6 -> {
                when {
                    info.lastUpdateTime - other.info.lastUpdateTime < 0 -> 1
                    info.lastUpdateTime - other.info.lastUpdateTime > 0 -> -1
                    else -> 0
                }
            }
            7 -> {
                when {
                    info.firstInstallTime - other.info.firstInstallTime > 0 -> 1
                    info.firstInstallTime - other.info.firstInstallTime < 0 -> -1
                    else -> 0
                }
            }
            8 -> {
                when {
                    info.firstInstallTime - other.info.firstInstallTime < 0 -> 1
                    info.firstInstallTime - other.info.firstInstallTime > 0 -> -1
                    else -> 0
                }
            }
            9 -> {
                getPackageName().lowercase().compareTo(other.getPackageName().lowercase())
            }
            10 -> {
                other.getPackageName().lowercase().compareTo(getPackageName().lowercase())
            }
            else -> 0
        }
    }
}

