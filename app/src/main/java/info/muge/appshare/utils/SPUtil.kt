package info.muge.appshare.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import info.muge.appshare.Constants

/**
 * SharedPreferences工具类
 */
object SPUtil {

    @JvmStatic
    fun getDisplayingExportPath(context: Context): String {
        return "内置存储/Download/AppKit/"
    }

    /**
     * 获取当前应用导出的内置主路径
     * @return 应用导出内置路径，最后没有文件分隔符，例如 /storage/emulated/0
     */
    @JvmStatic
    fun getInternalSavePath(): String {
        return Constants.PREFERENCE_SAVE_PATH_DEFAULT
    }

    /**
     * 获取全局配置
     */
    @JvmStatic
    fun getGlobalSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 判断是否存储到了外置设备上
     * @return true-存储到了外置存储上
     */
    @JvmStatic
    fun getIsSaved2ExternalStorage(context: Context): Boolean {
        return false
    }

    /**
     * 获取外置存储的uri值
     */
    @JvmStatic
    fun getExternalStorageUri(context: Context): String {
        return Uri.parse(Constants.PREFERENCE_SAVE_PATH_DEFAULT).toString()
    }

    /**
     * 发送/接收 端口号，默认6565
     */
    @JvmStatic
    fun getPortNumber(context: Context): Int {
        return getGlobalSharedPreferences(context).getInt(
            Constants.PREFERENCE_NET_PORT,
            Constants.PREFERENCE_NET_PORT_DEFAULT
        )
    }

    /**
     * 获取导出压缩包的扩展名
     */
    @JvmStatic
    fun getCompressingExtensionName(context: Context): String {
        return getGlobalSharedPreferences(context).getString(
            Constants.PREFERENCE_COMPRESSING_EXTENSION,
            Constants.PREFERENCE_COMPRESSING_EXTENSION_DEFAULT
        ) ?: Constants.PREFERENCE_COMPRESSING_EXTENSION_DEFAULT
    }

    /**
     * 获取设备名称
     */
    @JvmStatic
    fun getDeviceName(context: Context): String {
        return try {
            getGlobalSharedPreferences(context)
                .getString(Constants.PREFERENCE_DEVICE_NAME, Build.BRAND)
                ?: Constants.PREFERENCE_DEVICE_NAME_DEFAULT
        } catch (e: Exception) {
            e.printStackTrace()
            Constants.PREFERENCE_DEVICE_NAME_DEFAULT
        }
    }
}
