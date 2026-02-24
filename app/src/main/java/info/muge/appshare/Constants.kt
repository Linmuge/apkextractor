package info.muge.appshare

import androidx.appcompat.app.AppCompatDelegate
import info.muge.appshare.utils.StorageUtil

/**
 * 常量定义
 */
object Constants {

    /**
     * this preference stands for a string value;
     */
    const val PREFERENCE_NAME = "settings"

    /**
     * this preference stands for a string value;
     */
    const val PREFERENCE_SAVE_PATH = "savepath"
    val PREFERENCE_SAVE_PATH_DEFAULT = "${StorageUtil.getMainExternalStoragePath()}/Download/AppKit"

    /**
     * this preference stands for a boolean value;
     */
    const val PREFERENCE_STORAGE_PATH_EXTERNAL = "save_external"
    const val PREFERENCE_STORAGE_PATH_EXTERNAL_DEFAULT = false

    /**
     * this preference stands for a string value;
     */
    const val PREFERENCE_FILENAME_FONT_APK = "font_apk"

    /**
     * this preference stands for a string value;
     */
    const val PREFERENCE_FILENAME_FONT_ZIP = "font_zip"

    /**
     * this preference stands for a int value;
     */
    const val PREFERENCE_ZIP_COMPRESS_LEVEL = "zip_level"
    const val PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT = -1

    /**
     * this preference stands for a int value;
     */
    const val PREFERENCE_SHAREMODE = "share_mode"

    /**
     * this preference stands for a int value;
     */
    const val PREFERENCE_SORT_CONFIG = "sort_config"

    /**
     * 安装包项目的排序方式，int值
     */
    const val PREFERENCE_SORT_CONFIG_IMPORT_ITEMS = "sort_config_import"

    /**
     * this preference stands for a int value
     */
    const val PREFERENCE_MAIN_PAGE_VIEW_MODE = "main_view_mode"
    const val PREFERENCE_MAIN_PAGE_VIEW_MODE_DEFAULT = 1
    const val PREFERENCE_MAIN_PAGE_VIEW_MODE_IMPORT = "main_view_mode_import"
    const val PREFERENCE_MAIN_PAGE_VIEW_MODE_IMPORT_DEFAULT = 0

    /**
     * this preference stands for a boolean value;
     */
    const val PREFERENCE_SHOW_SYSTEM_APP = "show_system_app"
    const val PREFERENCE_SHOW_SYSTEM_APP_DEFAULT = false

    /**
     * stands for a boolean value
     */
    const val PREFERENCE_LOAD_PERMISSIONS = "load_permissions"

    /**
     * stands for a boolean value
     */
    const val PREFERENCE_LOAD_ACTIVITIES = "load_activities"

    /**
     * stands for a boolean value
     */
    const val PREFERENCE_LOAD_RECEIVERS = "load_receivers"

    /**
     * boolean value
     */
    const val PREFERENCE_LOAD_SERVICES = "load_services"

    /**
     * boolean value
     */
    const val PREFERENCE_LOAD_PROVIDERS = "load_providers"

    /**
     * stands for a boolean value
     */
    const val PREFERENCE_LOAD_STATIC_LOADERS = "load_static_receivers"

    /**
     * stands for a boolean value
     */
    const val PREFERENCE_LOAD_APK_SIGNATURE = "load_apk_signature"

    /**
     * stands for a boolean value
     */
    const val PREFERENCE_LOAD_FILE_HASH = "load_file_hash"

    /**
     * stands for a int value
     */
    const val PREFERENCE_NIGHT_MODE = "night_mode"
    const val PREFERENCE_DYNAMIC_COLOR = "dynamic_color"
    const val PREFERENCE_THEME_COLOR = "theme_seed_color"
    const val PREFERENCE_THEME_COLOR_DEFAULT = 0xFF4285F4.toInt() // Google Blue
    const val PREFERENCE_AMOLED = "amoled_mode"

    /**
     * int value
     */
    const val PREFERENCE_LANGUAGE = "language"
    const val LANGUAGE_FOLLOW_SYSTEM = 0
    const val LANGUAGE_CHINESE = 1
    const val LANGUAGE_ENGLISH = 2
    const val PREFERENCE_LANGUAGE_DEFAULT = LANGUAGE_FOLLOW_SYSTEM

    const val PREFERENCE_NIGHT_MODE_DEFAULT = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

    const val PREFERENCE_LOAD_PERMISSIONS_DEFAULT = true
    const val PREFERENCE_LOAD_ACTIVITIES_DEFAULT = true
    const val PREFERENCE_LOAD_RECEIVERS_DEFAULT = true
    const val PREFERENCE_LOAD_SERVICES_DEFAULT = true
    const val PREFERENCE_LOAD_PROVIDERS_DEFAULT = true
    const val PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT = false
    const val PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT = true
    const val PREFERENCE_LOAD_FILE_HASH_DEFAULT = true

    const val SHARE_MODE_DIRECT = -1
    const val SHARE_MODE_AFTER_EXTRACT = 0
    const val PREFERENCE_SHAREMODE_DEFAULT = SHARE_MODE_DIRECT

    const val FONT_AUTO_SEQUENCE_NUMBER = "?A"
    const val FONT_APP_NAME = "?N"
    const val FONT_APP_PACKAGE_NAME = "?P"
    const val FONT_APP_VERSIONCODE = "?C"
    const val FONT_APP_VERSIONNAME = "?V"
    const val FONT_YEAR = "?Y"
    const val FONT_MONTH = "?M"
    const val FONT_DAY_OF_MONTH = "?D"
    const val FONT_HOUR_OF_DAY = "?H"
    const val FONT_MINUTE = "?I"
    const val FONT_SECOND = "?S"

    const val ZIP_LEVEL_STORED = 0
    const val ZIP_LEVEL_LOW = 1
    const val ZIP_LEVEL_NORMAL = 5
    const val ZIP_LEVEL_HIGH = 9

    const val PREFERENCE_FILENAME_FONT_DEFAULT = "$FONT_APP_PACKAGE_NAME-$FONT_APP_VERSIONCODE"

    const val ACTION_REFRESH_APP_LIST = "info.muge.appshare.refresh_applist"
    const val ACTION_REFRESH_IMPORT_ITEMS_LIST = "info.muge.appshare.refresh_import_items_list"
    const val ACTION_REFRESH_AVAILIBLE_STORAGE = "info.muge.appshare.refresh_storage"

    /**
     * 绑定的端口号，1024~65535之间，int值
     */
    const val PREFERENCE_NET_PORT = "port_number"
    const val PREFERENCE_NET_PORT_DEFAULT = 6565

    /**
     * 设备名称
     */
    const val PREFERENCE_DEVICE_NAME = "device_name"
    const val PREFERENCE_DEVICE_NAME_DEFAULT = "MyDevice"

    /**
     * 导出压缩包的扩展名
     */
    const val PREFERENCE_COMPRESSING_EXTENSION = "compressing_extension"
    const val PREFERENCE_COMPRESSING_EXTENSION_DEFAULT = "zip"

    /**
     * 安装包扫描范围
     */
    const val PREFERENCE_PACKAGE_SCOPE = "package_scope"
    const val PACKAGE_SCOPE_ALL = 0
    const val PACKAGE_SCOPE_EXPORTING_PATH = 1
    const val PREFERENCE_PACKAGE_SCOPE_DEFAULT = PACKAGE_SCOPE_EXPORTING_PATH

    /**
     * 批量复制包名的分隔内容
     */
    const val PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR = "copying_package_name_separator"
    const val PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR_DEFAULT = ","
}

