package info.muge.appshare

import android.app.Application
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.ThemeUtil
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors

/**
 * 应用程序类
 * 负责初始化全局配置，包括夜间模式和动态取色
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        // 设置夜间模式
        val settings = SPUtil.getGlobalSharedPreferences(this)
        val night_mode = settings.getInt(Constants.PREFERENCE_NIGHT_MODE, Constants.PREFERENCE_NIGHT_MODE_DEFAULT)
        AppCompatDelegate.setDefaultNightMode(night_mode)

        // 应用动态取色
        // Android 12+ 会使用系统壁纸颜色
        // Android 12 以下会使用基于 #4285F4 的 MD3 颜色方案
        ThemeUtil.applyDynamicColorsToApp(this)
    }


    companion object {

        lateinit var instance: Application
            private set
    }
}