package info.muge.appshare.items

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import info.muge.appshare.Constants
import info.muge.appshare.DisplayItem
import info.muge.appshare.R
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.PinyinUtil
import info.muge.appshare.utils.SPUtil
import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

/**
 * 导入项
 */
class ImportItem : DisplayItem, Comparable<ImportItem> {

    enum class ImportType {
        APK, ZIP
    }

    companion object {
        @JvmField
        var sort_config = 0
    }

    private val context: Context
    private val fileItem: FileItem
    private val length: Long
    private val importType: ImportType
    private val packageInfo: PackageInfo?
    private val drawable: Drawable
    private val version_name: String
    private val version_code: String
    private val minSdkVersion: String
    private val targetSdkVersion: String
    private val lastModified: Long

    @Transient
    var importData = false
    @Transient
    var importObb = false
    @Transient
    var importApk = false

    constructor(context: Context, fileItem: FileItem) {
        this.fileItem = fileItem
        this.context = context
        
        var versionName = context.resources.getString(R.string.word_unknown)
        var versionCode = context.resources.getString(R.string.word_unknown)
        var minSdk = context.resources.getString(R.string.word_unknown)
        var targetSdk = context.resources.getString(R.string.word_unknown)
        var icon = ContextCompat.getDrawable(context, R.drawable.icon_file)!!
        var pkgInfo: PackageInfo? = null
        var type = ImportType.ZIP

        val fileName = fileItem.getName().trim().lowercase()
        
        when {
            fileName.endsWith(".zip") || fileName.endsWith(SPUtil.getCompressingExtensionName(context).lowercase()) -> {
                type = ImportType.ZIP
                icon = ContextCompat.getDrawable(context, R.drawable.icon_zip)!!
            }
            fileName.endsWith(".xapk") -> {
                type = ImportType.ZIP
                icon = ContextCompat.getDrawable(context, R.drawable.icon_xapk)!!
            }
            fileName.endsWith(".apk") -> {
                type = ImportType.APK
                val packageManager = context.applicationContext.packageManager
                
                if (fileItem.isFileInstance()) {
                    try {
                        val settings = SPUtil.getGlobalSharedPreferences(context)
                        var flag = 0
                        if (settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS, Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT)) {
                            flag = flag or PackageManager.GET_PERMISSIONS
                        }
                        if (settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES, Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT)) {
                            flag = flag or PackageManager.GET_ACTIVITIES
                        }
                        if (settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS, Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT)) {
                            flag = flag or PackageManager.GET_RECEIVERS
                        }
                        if (settings.getBoolean(Constants.PREFERENCE_LOAD_APK_SIGNATURE, Constants.PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT)) {
                            flag = flag or PackageManager.GET_SIGNATURES
                        }
                        if (settings.getBoolean(Constants.PREFERENCE_LOAD_SERVICES, Constants.PREFERENCE_LOAD_SERVICES_DEFAULT)) {
                            flag = flag or PackageManager.GET_SERVICES
                        }
                        if (settings.getBoolean(Constants.PREFERENCE_LOAD_PROVIDERS, Constants.PREFERENCE_LOAD_PROVIDERS_DEFAULT)) {
                            flag = flag or PackageManager.GET_PROVIDERS
                        }
                        
                        pkgInfo = packageManager.getPackageArchiveInfo(fileItem.getPath(), flag)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    if (pkgInfo != null) {
                        pkgInfo.applicationInfo!!.sourceDir = fileItem.getPath()
                        pkgInfo.applicationInfo!!.publicSourceDir = fileItem.getPath()
                        icon = pkgInfo.applicationInfo!!.loadIcon(packageManager)
                        versionName = pkgInfo.versionName ?: versionName
                        versionCode = pkgInfo.versionCode.toString()
                        if (Build.VERSION.SDK_INT > 23) {
                            minSdk = pkgInfo.applicationInfo!!.minSdkVersion.toString()
                        }
                        targetSdk = pkgInfo.applicationInfo!!.targetSdkVersion.toString()
                    } else {
                        icon = ContextCompat.getDrawable(context, R.drawable.icon_apk)!!
                    }
                } else {
                    icon = ContextCompat.getDrawable(context, R.drawable.icon_apk)!!
                }
            }
        }

        this.importType = type
        this.drawable = icon
        this.version_name = versionName
        this.version_code = versionCode
        this.minSdkVersion = minSdk
        this.targetSdkVersion = targetSdk
        this.packageInfo = pkgInfo
        this.length = fileItem.length()
        this.lastModified = fileItem.lastModified()
    }

    constructor(wrapper: ImportItem, importData: Boolean, importObb: Boolean, importApk: Boolean) {
        this.drawable = wrapper.drawable
        this.version_name = wrapper.version_name
        this.version_code = wrapper.version_code
        this.minSdkVersion = wrapper.minSdkVersion
        this.targetSdkVersion = wrapper.targetSdkVersion
        this.fileItem = wrapper.fileItem
        this.context = wrapper.context
        this.importType = wrapper.importType
        this.length = wrapper.length
        this.lastModified = wrapper.lastModified
        this.packageInfo = wrapper.packageInfo
        this.importData = importData
        this.importObb = importObb
        this.importApk = importApk
    }

    override fun getIconDrawable(): Drawable = drawable

    override fun getTitle(): String = fileItem.getName()

    override fun getDescription(): String {
        val dateFormat = SimpleDateFormat.getDateTimeInstance()
        return if (importType == ImportType.APK) {
            "${dateFormat.format(Date(lastModified))}($version_name)"
        } else {
            dateFormat.format(Date(lastModified))
        }
    }

    override fun getSize(): Long = length

    override fun isRedMarked(): Boolean = false

    fun getItemName(): String = fileItem.getName()

    fun getLastModified(): Long = lastModified

    fun getImportType(): ImportType = importType

    fun getPackageInfo(): PackageInfo? = packageInfo

    /**
     * 只针对apk的版本名
     */
    fun getVersionName(): String = version_name

    fun getVersionCode(): String = version_code

    fun getMinSdkVersion(): String = minSdkVersion

    fun getTargetSdkVersion(): String = targetSdkVersion

    fun getItemIconDrawable(): Drawable = drawable

    fun getFileItem(): FileItem = fileItem

    /**
     * 当本项目为zip包时的输入流
     */
    @Throws(Exception::class)
    fun getZipInputStream(): InputStream? {
        return if (importType == ImportType.ZIP) fileItem.getInputStream() else null
    }

    fun getUri(): Uri? {
        return when {
            fileItem.isDocumentFile() -> fileItem.getDocumentFile()?.uri
            fileItem.isFileInstance() -> EnvironmentUtil.getUriForFileByFileProvider(context, fileItem.getFile()!!)
            fileItem.isShareUriInstance() -> fileItem.getContentUri()
            else -> null
        }
    }

    /**
     * 如果此导入项为存储到内置存储的Uri.fromFile()
     * @return uri
     */
    fun getUriFromFile(): Uri? {
        return if (fileItem.isFileInstance()) Uri.fromFile(fileItem.getFile()) else null
    }

    override fun compareTo(other: ImportItem): Int {
        return when (sort_config) {
            1 -> {
                try {
                    PinyinUtil.getFirstSpell(getTitle()).lowercase()
                        .compareTo(PinyinUtil.getFirstSpell(other.getTitle()).lowercase())
                } catch (e: Exception) {
                    e.printStackTrace()
                    0
                }
            }
            2 -> {
                try {
                    PinyinUtil.getFirstSpell(other.getTitle()).lowercase()
                        .compareTo(PinyinUtil.getFirstSpell(getTitle()).lowercase())
                } catch (e: Exception) {
                    e.printStackTrace()
                    0
                }
            }
            3 -> {
                when {
                    getSize() - other.getSize() > 0 -> 1
                    getSize() - other.getSize() < 0 -> -1
                    else -> 0
                }
            }
            4 -> {
                when {
                    getSize() - other.getSize() < 0 -> 1
                    getSize() - other.getSize() > 0 -> -1
                    else -> 0
                }
            }
            5 -> {
                when {
                    getLastModified() - other.getLastModified() > 0 -> 1
                    getLastModified() - other.getLastModified() < 0 -> -1
                    else -> 0
                }
            }
            6 -> {
                when {
                    getLastModified() - other.getLastModified() < 0 -> 1
                    getLastModified() - other.getLastModified() > 0 -> -1
                    else -> 0
                }
            }
            else -> 0
        }
    }
}

