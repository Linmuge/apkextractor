package info.muge.appshare.utils

import android.util.Log

/**
 * 统一日志工具类
 * 提供一致的日志记录接口，便于调试和错误追踪
 */
object LogUtil {
    private const val TAG = "ApkExtractor"

    /**
     * 是否启用调试日志
     * 可根据 BuildConfig.DEBUG 动态配置
     */
    var isDebugMode: Boolean = true

    /**
     * 输出调试日志
     */
    fun d(message: String, tag: String = TAG) {
        if (isDebugMode) {
            Log.d(tag, message)
        }
    }

    /**
     * 输出信息日志
     */
    fun i(message: String, tag: String = TAG) {
        Log.i(tag, message)
    }

    /**
     * 输出警告日志
     */
    fun w(message: String, throwable: Throwable? = null, tag: String = TAG) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }

    /**
     * 输出错误日志
     */
    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    /**
     * 输出详细日志
     */
    fun v(message: String, tag: String = TAG) {
        if (isDebugMode) {
            Log.v(tag, message)
        }
    }

    /**
     * 记录异常（替代 e.printStackTrace()）
     */
    fun logException(message: String, throwable: Throwable, tag: String = TAG) {
        e(message, throwable, tag)
    }

    /**
     * 记录异常（无消息）
     */
    fun logException(throwable: Throwable, tag: String = TAG) {
        e(throwable.message ?: "Unknown error", throwable, tag)
    }
}
