package info.muge.appshare.activities

import android.graphics.Color
import android.view.View
import android.view.WindowManager
import info.muge.appshare.base.BaseActivity
import info.muge.appshare.databinding.ActivityLaunchBinding
import info.muge.appshare.ui.showPrivacyDialog
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.anko.startActivity

/**
 * 启动页Activity
 * 用于显示应用启动画面和隐私政策对话框
 */
class LaunchActivity : BaseActivity<ActivityLaunchBinding>() {

    override fun ActivityLaunchBinding.initView() {
        // 设置状态栏样式
        setupStatusBar()

        // 检查是否首次启动
        if (SPUtil.getGlobalSharedPreferences(this@LaunchActivity).getBoolean("start", true)) {
            // 首次启动，显示隐私政策对话框
            showPrivacyDialog()
        } else {
            // 非首次启动，直接进入主页
            startActivity<MainActivity>()
            finish()
        }
    }

    /**
     * 设置状态栏为透明全屏模式
     */
    private fun setupStatusBar() {
        val window = window
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        window.statusBarColor = Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    }
}