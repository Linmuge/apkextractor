package info.muge.appshare.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions

/**
 * 主题工具类
 * 用于处理动态取色和备用颜色方案
 */
object ThemeUtil {
    
    /**
     * 检查设备是否支持动态取色
     * 动态取色需要 Android 12 (API 31) 及以上版本
     */
    fun isDynamicColorAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    
    /**
     * 为 Activity 应用动态取色
     * 如果设备不支持动态取色，则使用基于 #4285f4 的 MD3 颜色方案
     * 
     * @param activity 要应用主题的 Activity
     */
    fun applyDynamicColors(activity: Activity) {
        if (isDynamicColorAvailable()) {
            // Android 12+ 支持动态取色
            val options = DynamicColorsOptions.Builder()
                .setThemeOverlay(0) // 使用默认的动态颜色覆盖
                .build()
            DynamicColors.applyToActivityIfAvailable(activity, options)
        } else {
            // Android 12 以下使用备用颜色方案
            // 主题已经在 styles.xml 中定义，这里不需要额外操作
            // 颜色资源会自动使用 colors.xml 中定义的 MD3 颜色
        }
    }
    
    /**
     * 为整个应用应用动态取色
     * 如果设备不支持动态取色，则使用基于 #4285f4 的 MD3 颜色方案
     * 
     * @param context 应用上下文
     */
    fun applyDynamicColorsToApp(context: Context) {
        if (isDynamicColorAvailable()) {
            // Android 12+ 支持动态取色
            DynamicColors.applyToActivitiesIfAvailable(context.applicationContext as android.app.Application)
        }
        // Android 12 以下会自动使用 colors.xml 中定义的静态颜色
    }
    
    /**
     * 检查当前是否正在使用动态取色
     * 
     * @param context 上下文
     * @return 如果正在使用动态取色返回 true，否则返回 false
     */
    fun isDynamicColorApplied(context: Context): Boolean {
        return isDynamicColorAvailable() && DynamicColors.isDynamicColorAvailable()
    }
}

