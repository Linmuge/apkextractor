package info.muge.appshare.base

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import info.muge.appshare.Constants
import info.muge.appshare.R
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.ThemeUtil
import info.muge.appshare.utils.setStatusBarIconColorMode
import java.lang.reflect.ParameterizedType
import java.util.Locale

/**
 * Activity 基类
 * 提供 ViewBinding 支持、语言设置和主题配置
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity(), BaseViewBinding<VB>{


    internal val binding: VB by lazy(mode = LazyThreadSafetyMode.NONE) {
        getViewBinding(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 应用主题（必须在 super.onCreate 之前调用）
        applyTheme()

        super.onCreate(savedInstanceState)
        val window = window
        setContentView(binding.root)
        setAndRefreshLanguage()
        setStatusBarIconColorMode()

        binding.initView()
    }

    /**
     * 应用主题
     * 如果设备支持动态取色（Android 12+），使用系统壁纸颜色
     * 否则使用基于 #4285F4 的 MD3 颜色方案
     */
    private fun applyTheme() {
        if (!ThemeUtil.isDynamicColorAvailable()) {
            // Android 12 以下使用静态 MD3 主题
            setTheme(R.style.AppTheme_MD3_NoActionBar)
        }
        // Android 12+ 会在 MyApplication 中自动应用动态取色
    }


    fun <VB: ViewBinding> Any.getViewBinding(inflater: LayoutInflater): VB {
        val vbClass =  (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments.filterIsInstance<Class<VB>>()
        val inflate = vbClass[0].getDeclaredMethod("inflate", LayoutInflater::class.java)
        return  inflate.invoke(null, inflater) as VB
    }    fun setAndRefreshLanguage() {
        // 获得res资源对象
        val resources = resources
        // 获得屏幕参数：主要是分辨率，像素等。
        val metrics = resources.displayMetrics
        // 获得配置对象
        val config = resources.configuration
        //区别17版本（其实在17以上版本通过 config.locale设置也是有效的，不知道为什么还要区别）
        //在这里设置需要转换成的语言，也就是选择用哪个values目录下的strings.xml文件
        val value = SPUtil.getGlobalSharedPreferences(this)
            .getInt(Constants.PREFERENCE_LANGUAGE, Constants.PREFERENCE_LANGUAGE_DEFAULT)
        var locale: Locale? = null
        when (value) {
            Constants.LANGUAGE_FOLLOW_SYSTEM -> locale = Locale.getDefault()
            Constants.LANGUAGE_CHINESE -> locale = Locale.SIMPLIFIED_CHINESE
            Constants.LANGUAGE_ENGLISH -> locale = Locale.ENGLISH
            else -> {}
        }
        if (locale == null) return
        config.setLocale(locale)
        resources.updateConfiguration(config, metrics)
    }
    val Activity.isDarkTheme:Boolean get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK=== Configuration.UI_MODE_NIGHT_NO
    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
    }

}

interface BaseViewBinding<VB : ViewBinding> {
    fun VB.initView()
}