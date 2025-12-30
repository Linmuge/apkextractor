package info.muge.appshare.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.material.tabs.TabLayoutMediator
import info.muge.appshare.Global
import info.muge.appshare.Global.ExportTaskFinishedListener
import info.muge.appshare.R
import info.muge.appshare.adapters.AppDetailPagerAdapter
import info.muge.appshare.base.BaseActivity
import info.muge.appshare.databinding.ActivityAppDetailBinding
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.ToastManager
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.OutputUtil
import info.muge.appshare.utils.SPUtil

/**
 * 应用详情页面
 * 使用 TabLayout + ViewPager2 展示应用的各项信息
 */
class AppDetailActivity : BaseActivity<ActivityAppDetailBinding>() {

    private var appItem: AppItem? = null
    private var isReceiverRegistered = false
    private lateinit var pagerAdapter: AppDetailPagerAdapter

    private val uninstallReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            try {
                if (intent.action == Intent.ACTION_PACKAGE_REMOVED || 
                    intent.action == Intent.ACTION_PACKAGE_REPLACED) {
                    val data = intent.dataString
                    val packageName = data!!.substring(data.indexOf(":") + 1)
                    if (packageName.equals(appItem?.getPackageName(), ignoreCase = true)) {
                        finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun ActivityAppDetailBinding.initView() {
        // 初始化 appItem
        val packageName = intent.getStringExtra(BaseActivity.EXTRA_PACKAGE_NAME)
            ?: intent.getStringExtra(EXTRA_PACKAGE_NAME)

        if (packageName == null) {
            // 尝试获取 APK URI
            val apkUriString = intent.getStringExtra(EXTRA_APK_URI)
            if (apkUriString != null) {
                // 处理外部 APK
                val apkUri = Uri.parse(apkUriString)
                handleExternalApk(apkUri)
            } else {
                ToastManager.showToast(
                    this@AppDetailActivity,
                    "无法获取应用信息",
                    Toast.LENGTH_SHORT
                )
                finish()
                return
            }
        } else {
            // 处理已安装应用
            synchronized(Global.app_list) {
                appItem = Global.getAppItemByPackageNameFromList(Global.app_list, packageName)
            }

            if (appItem == null) {
                ToastManager.showToast(
                    this@AppDetailActivity,
                    "(-_-)The AppItem info is null, try to restart this application.",
                    Toast.LENGTH_SHORT
                )
                finish()
                return
            }
        }

        // 设置 Toolbar
        setSupportActionBar(toolbarAppDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""  // 不显示标题，避免与下方名称重复

        // 设置顶部信息
        appDetailName.text = appItem!!.getAppName()
        appDetailVersionNameTitle.text = appItem!!.getVersionName()
        appDetailIcon.setImageDrawable(appItem!!.getIcon())

        // 点击图标进入系统详情
        // 点击图标显示选项
        // 点击图标显示选项
        appDetailIcon.setOnClickListener {
            val isExternal = appItem?.getInstallSource() == "External File"
            val items = if (isExternal) {
                arrayOf(getString(R.string.action_save_icon))
            } else {
                arrayOf(
                    getString(R.string.menu_open_system_settings),
                    getString(R.string.action_save_icon)
                )
            }
            
            AlertDialog.Builder(this@AppDetailActivity)
                .setItems(items) { _, which ->
                    if (isExternal) {
                        if (which == 0) EnvironmentUtil.saveDrawableToGallery(this@AppDetailActivity, appItem!!.getIcon(), appItem!!.getAppName())
                    } else {
                        when (which) {
                            0 -> openSystemAppDetails()
                            1 -> EnvironmentUtil.saveDrawableToGallery(this@AppDetailActivity, appItem!!.getIcon(), appItem!!.getAppName())
                        }
                    }
                }
                .show()
        }

        // 设置 ViewPager2 和 TabLayout
        setupViewPager(appItem?.getPackageName() ?: "")

        // 注册广播接收器
        registerUninstallReceiver()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app_detail, menu)
        if (appItem?.getInstallSource() == "External File") {
            menu.findItem(R.id.action_run)?.isVisible = false
        }
        return true
    }

    private fun ActivityAppDetailBinding.setupViewPager(packageName: String) {
        pagerAdapter = AppDetailPagerAdapter(this@AppDetailActivity, packageName)
        viewPager.adapter = pagerAdapter

        // 关联 TabLayout 和 ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getString(pagerAdapter.getTabTitleResId(position))
        }.attach()
    }

    private fun registerUninstallReceiver() {
        try {
            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            registerReceiver(uninstallReceiver, intentFilter)
            isReceiverRegistered = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleExternalApk(uri: Uri) {
        try {
            // 需要将文件复制到缓存目录，因为 getPackageArchiveInfo 需要文件路径
            val cacheFile = java.io.File(externalCacheDir, "temp_analysis.apk")
            contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            appItem = AppItem(this, cacheFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastManager.showToast(
                this,
                "解析 APK 失败: ${e.message}",
                Toast.LENGTH_SHORT
            )
            finish()
        }
    }

    private fun getSingleItemArrayList(): ArrayList<AppItem> {
        val list = ArrayList<AppItem>()
        val item = AppItem(appItem!!, false, false)
        list.add(item)
        return list
    }

    private fun openSystemAppDetails() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", appItem!!.getPackageName(), null)
        startActivity(intent)
    }

    private fun runApp() {
        try {
            startActivity(packageManager.getLaunchIntentForPackage(appItem!!.getPackageName()))
        } catch (e: Exception) {
            ToastManager.showToast(
                this,
                "应用没有界面,无法运行",
                Toast.LENGTH_SHORT
            )
        }
    }

    private fun exportApp() {
        val singleList = getSingleItemArrayList()
        val item = singleList[0]
        Global.checkAndExportCertainAppItemsToSetPathWithoutShare(
            this,
            singleList,
            false,
            object : ExportTaskFinishedListener {
                override fun onFinished(errorMessage: String) {
                    if (errorMessage.trim().isNotEmpty()) {
                        AlertDialog.Builder(this@AppDetailActivity)
                            .setTitle(getString(R.string.exception_title))
                            .setMessage(getString(R.string.exception_message) + errorMessage)
                            .setPositiveButton(getString(R.string.dialog_button_confirm)) { _, _ -> }
                            .show()
                        return
                    }
                    ToastManager.showToast(
                        this@AppDetailActivity,
                        getString(R.string.toast_export_complete) + " " +
                                SPUtil.getDisplayingExportPath() +
                                OutputUtil.getWriteFileNameForAppItem(
                                    this@AppDetailActivity,
                                    singleList[0],
                                    if (item.exportData || item.exportObb) 
                                        SPUtil.getCompressingExtensionName(this@AppDetailActivity) 
                                    else "apk",
                                    1
                                ),
                        Toast.LENGTH_SHORT
                    )
                }
            }
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            checkHeightAndFinish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun checkHeightAndFinish() {
        if (Build.VERSION.SDK_INT >= 28) {
            ActivityCompat.finishAfterTransition(this)
        } else {
            finish()
        }
    }

    override fun finish() {
        super.finish()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(uninstallReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> checkHeightAndFinish()
            R.id.action_run -> runApp()
            R.id.action_export -> exportApp()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"
        const val EXTRA_APK_URI = "EXTRA_APK_URI"
    }
}
