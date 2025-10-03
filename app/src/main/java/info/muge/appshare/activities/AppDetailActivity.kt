package info.muge.appshare.activities

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.text.format.Formatter
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.google.android.material.snackbar.Snackbar
import info.muge.appshare.Constants
import info.muge.appshare.Global
import info.muge.appshare.Global.ExportTaskFinishedListener
import info.muge.appshare.R
import info.muge.appshare.base.BaseActivity
import info.muge.appshare.databinding.ActivityAppDetailBinding
import info.muge.appshare.databinding.ContentAppDetailBinding
import info.muge.appshare.items.AppItem
import info.muge.appshare.tasks.GetPackageInfoViewTask
import info.muge.appshare.tasks.GetSignatureInfoTask
import info.muge.appshare.tasks.HashTask
import info.muge.appshare.ui.ToastManager
import info.muge.appshare.utils.OutputUtil
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.toast
import java.text.SimpleDateFormat
import java.util.Date

class AppDetailActivity : BaseActivity<ActivityAppDetailBinding>(), View.OnClickListener {
    private var appItem: AppItem? = null
    private lateinit var contentBinding: ContentAppDetailBinding
    private var isReceiverRegistered = false  // 标记receiver是否已注册
    private val uninstall_receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            try {
                if (intent.getAction() == Intent.ACTION_PACKAGE_REMOVED || intent.getAction() == Intent.ACTION_PACKAGE_REPLACED) {
                    val data = intent.getDataString()
                    val package_name = data!!.substring(data.indexOf(":") + 1)
                    if (package_name.equals(appItem!!.getPackageName(), ignoreCase = true)) finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun ActivityAppDetailBinding.initView() {
        // 绑定content布局 - 必须先初始化，避免后续访问时崩溃
        // 尝试多种方式查找NestedScrollView
        var nestedScrollView = findNestedScrollView(root as ViewGroup)

        // 如果递归查找失败，尝试直接从window的decorView查找
        if (nestedScrollView == null) {
            android.util.Log.w("AppDetailActivity", "First attempt failed, trying from decorView")
            nestedScrollView = findNestedScrollView(window.decorView as ViewGroup)
        }

        if (nestedScrollView == null) {
            // 如果找不到NestedScrollView，记录错误并关闭Activity
            android.util.Log.e("AppDetailActivity", "NestedScrollView not found in layout. Root: ${root.javaClass.simpleName}, childCount: ${(root as? ViewGroup)?.childCount}")
            ToastManager.showToast(
                this@AppDetailActivity,
                "布局初始化失败，请重试",
                Toast.LENGTH_SHORT
            )
            finish()
            return
        }
        contentBinding = ContentAppDetailBinding.bind(nestedScrollView)

        // 初始化appItem
        try {
            // 获取包名 - 尝试两种可能的key（兼容旧代码）
            val packageName = getIntent().getStringExtra(BaseActivity.EXTRA_PACKAGE_NAME)
                ?: getIntent().getStringExtra(EXTRA_PACKAGE_NAME)

            if (packageName == null) {
                android.util.Log.e("AppDetailActivity", "Package name is null in intent")
            } else {
                android.util.Log.d("AppDetailActivity", "Looking for package: $packageName")
                synchronized(Global.app_list) {
                    appItem = Global.getAppItemByPackageNameFromList(
                        Global.app_list,
                        packageName
                    )
                    android.util.Log.d("AppDetailActivity", "Found appItem: ${appItem != null}, list size: ${Global.app_list.size}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppDetailActivity", "Error getting appItem", e)
            e.printStackTrace()
        }

        // 检查appItem是否为null
        if (appItem == null) {
            ToastManager.showToast(
                this@AppDetailActivity,
                "(-_-)The AppItem info is null, try to restart this application.",
                Toast.LENGTH_SHORT
            )
            finish()
            return
        }

        setupStatusBar()

        setSupportActionBar(toolbarAppDetail)
        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()!!.setTitle(appItem!!.getAppName())


        val packageInfo = appItem!!.getPackageInfo()

        // 使用binding设置顶部信息
        appDetailName.setText(appItem!!.getAppName())
        appDetailVersionNameTitle.setText(appItem!!.getVersionName())
        appDetailIcon.setImageDrawable(appItem!!.getIcon())

        // 使用contentBinding设置详细信息
        contentBinding.appDetailPackageName.setText(appItem!!.getPackageName())
        contentBinding.appDetailVersionName.setText(appItem!!.getVersionName())
        contentBinding.appDetailVersionCode.setText(appItem!!.getVersionCode().toString())
        contentBinding.appDetailSize.setText(
            Formatter.formatFileSize(this@AppDetailActivity, appItem!!.getSize())
        )
        contentBinding.appDetailInstallTime.setText(
            SimpleDateFormat.getDateTimeInstance().format(Date(packageInfo.firstInstallTime))
        )
        contentBinding.appDetailUpdateTime.setText(
            SimpleDateFormat.getDateTimeInstance().format(Date(packageInfo.lastUpdateTime))
        )
        contentBinding.appDetailMinimumApi.setText(
            packageInfo.applicationInfo!!.minSdkVersion.toString()
        )
        contentBinding.appDetailTargetApi.setText(
            packageInfo.applicationInfo!!.targetSdkVersion.toString()
        )
        contentBinding.appDetailIsSystemApp.setText(
            getResources().getString(if ((appItem!!.getPackageInfo().applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) > 0) R.string.word_yes else R.string.word_no)
        )
        contentBinding.appDetailPathValue.setText(
            appItem!!.getPackageInfo().applicationInfo!!.sourceDir
        )
        contentBinding.appDetailInstallerNameValue.setText(
            appItem!!.getInstallSource()
        )
        contentBinding.appDetailUid.setText(
            appItem!!.getPackageInfo().applicationInfo!!.uid.toString()
        )
        contentBinding.appDetailLauncherValue.setText(
            appItem!!.getLaunchingClass()
        )


        GetPackageInfoViewTask(
            this@AppDetailActivity,
            appItem!!.getPackageInfo(),
            appItem!!.getStaticReceiversBundle(),
            contentBinding.appDetailAssembly,
            object : GetPackageInfoViewTask.CompletedCallback {
                override fun onViewsCreated() {
                    contentBinding.appDetailCardPg.setVisibility(View.GONE)
                }
            }).start()

        if (SPUtil.getGlobalSharedPreferences(this@AppDetailActivity).getBoolean(
                Constants.PREFERENCE_LOAD_APK_SIGNATURE,
                Constants.PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT
            )
        ) {
            contentBinding.appDetailSignatureAtt.setVisibility(View.VISIBLE)
            contentBinding.appDetailSignatureCard.setVisibility(View.VISIBLE)
            contentBinding.appDetailSignPg.setVisibility(View.VISIBLE)
            GetSignatureInfoTask(
                this@AppDetailActivity,
                appItem!!.getPackageInfo(),
                contentBinding.appDetailSignature,
                object : GetSignatureInfoTask.CompletedCallback {
                    override fun onCompleted() {
                        contentBinding.appDetailSignPg.setVisibility(View.GONE)
                    }
                }).start()
        }

        if (SPUtil.getGlobalSharedPreferences(this@AppDetailActivity).getBoolean(
                Constants.PREFERENCE_LOAD_FILE_HASH,
                Constants.PREFERENCE_LOAD_FILE_HASH_DEFAULT
            )
        ) {
            contentBinding.appDetailHashAtt.setVisibility(View.VISIBLE)
            contentBinding.appDetailHash.setVisibility(View.VISIBLE)
            HashTask(
                appItem!!.getFileItem(),
                HashTask.HashType.MD5,
                object : HashTask.CompletedCallback {
                    override fun onHashCompleted(result: String) {
                        contentBinding.detailHashMd5Pg.setVisibility(View.GONE)
                        contentBinding.detailHashMd5Value.setVisibility(View.VISIBLE)
                        contentBinding.detailHashMd5Value.setText(result)
                    }
                }).start()
            HashTask(
                appItem!!.getFileItem(),
                HashTask.HashType.SHA1,
                object : HashTask.CompletedCallback {
                    override fun onHashCompleted(result: String) {
                        contentBinding.detailHashSha1Pg.setVisibility(View.GONE)
                        contentBinding.detailHashSha1Value.setVisibility(View.VISIBLE)
                        contentBinding.detailHashSha1Value.setText(result)
                    }
                }).start()
            HashTask(
                appItem!!.getFileItem(),
                HashTask.HashType.SHA256,
                object : HashTask.CompletedCallback {
                    override fun onHashCompleted(result: String) {
                        contentBinding.detailHashSha256Pg.setVisibility(View.GONE)
                        contentBinding.detailHashSha256Value.setVisibility(View.VISIBLE)
                        contentBinding.detailHashSha256Value.setText(result)
                    }
                }).start()
            HashTask(
                appItem!!.getFileItem(),
                HashTask.HashType.CRC32,
                object : HashTask.CompletedCallback {
                    override fun onHashCompleted(result: String) {
                        contentBinding.detailHashCrc32Pg.setVisibility(View.GONE)
                        contentBinding.detailHashCrc32Value.setVisibility(View.VISIBLE)
                        contentBinding.detailHashCrc32Value.setText(result)
                    }
                }).start()
        }

        // 注册广播接收器
        try {
            val intentFilter = IntentFilter()
            intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
            intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
            intentFilter.addDataScheme("package")
            registerReceiver(uninstall_receiver, intentFilter)
            isReceiverRegistered = true  // 标记已注册
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 设置状态栏亮色模式
     */
    private fun setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false)
            getWindow().getInsetsController()!!.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = getWindow().getDecorView()
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }
    }

    override fun onClick(v: View) {
        val id = v.getId()
        if (id == R.id.app_detail_run_area) {
            try {
                startActivity(getPackageManager().getLaunchIntentForPackage(appItem!!.getPackageName()))
            } catch (e: Exception) {
                ToastManager.showToast(
                    this@AppDetailActivity,
                    "应用没有界面,无法运行",
                    Toast.LENGTH_SHORT
                )
            }
        } else if (id == R.id.app_detail_export_area) {
            val single_list: MutableList<AppItem> = this.singleItemArrayList
            val item = single_list.get(0)
            Global.checkAndExportCertainAppItemsToSetPathWithoutShare(
                this,
                single_list,
                false,
                object : ExportTaskFinishedListener {
                    override fun onFinished(error_message: String) {
                        if (error_message.trim { it <= ' ' } != "") {
                            AlertDialog.Builder(this@AppDetailActivity)
                                .setTitle(getResources().getString(R.string.exception_title))
                                .setMessage(getResources().getString(R.string.exception_message) + error_message)
                                .setPositiveButton(
                                    getResources().getString(R.string.dialog_button_confirm),
                                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> })
                                .show()
                            return
                        }
                        ToastManager.showToast(
                            this@AppDetailActivity,
                            (getResources().getString(R.string.toast_export_complete) + " "
                                    + SPUtil.getDisplayingExportPath()
                                    + OutputUtil.getWriteFileNameForAppItem(
                                this@AppDetailActivity,
                                single_list.get(0),
                                if (item.exportData || item.exportObb) SPUtil.getCompressingExtensionName(
                                    this@AppDetailActivity
                                ) else "apk",
                                1
                            )),
                            Toast.LENGTH_SHORT
                        )
                    }
                })
        } else if (id == R.id.app_detail_share_area) {
            Global.shareCertainAppsByItems(this, this.singleItemArrayList)
        } else if (id == R.id.app_detail_detail_area) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.setData(Uri.fromParts("package", appItem!!.getPackageName(), null))
            startActivity(intent)
        } else if (id == R.id.app_detail_market_area) {
            try {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + appItem!!.getPackageName())
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                ToastManager.showToast(this@AppDetailActivity, e.toString(), Toast.LENGTH_SHORT)
            }
        } else if (id == R.id.app_detail_package_name_area) {
            clip2ClipboardAndShowSnackbar(
                contentBinding.appDetailPackageName.getText().toString()
            )
        } else if (id == R.id.app_detail_version_name_area) {
            clip2ClipboardAndShowSnackbar(
                contentBinding.appDetailVersionName.getText().toString()
            )
        } else if (id == R.id.app_detail_version_code_area) {
            clip2ClipboardAndShowSnackbar(
                contentBinding.appDetailVersionCode.getText().toString()
            )
        } else if (id == R.id.app_detail_size_area) {
            clip2ClipboardAndShowSnackbar(
                contentBinding.appDetailSize.getText().toString()
            )
        } else if (id == R.id.app_detail_install_time_area) {
            clip2ClipboardAndShowSnackbar(
                contentBinding.appDetailInstallTime.getText().toString()
            )
        } else if (id == R.id.app_detail_update_time_area) {
            clip2ClipboardAndShowSnackbar(
                contentBinding.appDetailUpdateTime.getText().toString()
            )
        } else if (id == R.id.app_detail_minimum_api_area) {
            clip2ClipboardAndShowSnackbar(
                contentBinding.appDetailMinimumApi.getText().toString()
            )
        } else if (id == R.id.app_detail_target_api_area) {
            clip2ClipboardAndShowSnackbar(
                contentBinding.appDetailTargetApi.getText().toString()
            )
        } else if (id == R.id.app_detail_is_system_app_area) {
            clip2ClipboardAndShowSnackbar(
                contentBinding.appDetailIsSystemApp.getText().toString()
            )
        } else if (id == R.id.detail_hash_md5) {
            val value: String? = contentBinding.detailHashMd5Value.getText().toString()
            if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value)
        } else if (id == R.id.detail_hash_sha1) {
            val value: String? = contentBinding.detailHashSha1Value.getText().toString()
            if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value)
        } else if (id == R.id.detail_hash_sha256) {
            val value: String? = contentBinding.detailHashSha256Value.getText().toString()
            if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value)
        } else if (id == R.id.detail_hash_crc32) {
            val value: String? = contentBinding.detailHashCrc32Value.getText().toString()
            if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value)
        } else if (id == R.id.app_detail_path_area) {
            clip2ClipboardAndShowSnackbar(
                contentBinding.appDetailPathValue.getText().toString()
            )
        } else if (id == R.id.app_detail_installer_name_area) {
            clip2ClipboardAndShowSnackbar(
                contentBinding.appDetailInstallerNameValue.getText().toString()
            )
        } else if (id == R.id.app_detail_uid_area) {
            clip2ClipboardAndShowSnackbar(
                contentBinding.appDetailUid.getText().toString()
            )
        } else if (id == R.id.app_detail_launcher_area) {
            clip2ClipboardAndShowSnackbar(
                contentBinding.appDetailLauncherValue.getText().toString()
            )
        } else if (id == R.id.detail_signature_sub) {
            val value: String? = contentBinding.appDetailSignature.tv_sub_value.getText().toString()
            if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value)
        } else if (id == R.id.detail_signature_iss) {
            val value: String? = contentBinding.appDetailSignature.tv_iss_value.getText().toString()
            if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value)
        } else if (id == R.id.detail_signature_serial) {
            val value: String? = contentBinding.appDetailSignature.tv_serial_value.getText().toString()
            if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value)
        } else if (id == R.id.detail_signature_start) {
            val value: String? = contentBinding.appDetailSignature.tv_start.getText().toString()
            if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value)
        } else if (id == R.id.detail_signature_end) {
            val value: String? = contentBinding.appDetailSignature.tv_end.getText().toString()
            if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value)
        } else if (id == R.id.detail_signature_md5) {
            val value: String? = contentBinding.appDetailSignature.tv_md5.getText().toString()
            if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value)
        } else if (id == R.id.detail_signature_sha1) {
            val value: String? = contentBinding.appDetailSignature.tv_sha1.getText().toString()
            if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value)
        } else if (id == R.id.detail_signature_sha256) {
            val value: String? = contentBinding.appDetailSignature.tv_sha256.getText().toString()
            if (!TextUtils.isEmpty(value)) clip2ClipboardAndShowSnackbar(value)
        } else {
            "功能未开放".toast()
        }
    }

    private fun clip2ClipboardAndShowSnackbar(s: String?) {
        try {
            val manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText("message", s))
            Snackbar.make(
                binding.root,
                getResources().getString(R.string.snack_bar_clipboard),
                Snackbar.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val singleItemArrayList: ArrayList<AppItem>
        /**
         * 构造包含单个副本AppItem的ArrayList
         */
        get() {
            val list = java.util.ArrayList<AppItem>()
            val item = AppItem(appItem!!, false, false)

            list.add(item)
            return list
        }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            checkHeightAndFinish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun checkHeightAndFinish() {
        if (Build.VERSION.SDK_INT >= 28) { //根布局项目太多时低版本Android会引发一个底层崩溃。版本号暂定28
            ActivityCompat.finishAfterTransition(this)
        } else {
            if (contentBinding.appDetailAssembly.getIsExpanded()) {
                finish()
            } else {
                ActivityCompat.finishAfterTransition(this)
            }
        }
    }

    override fun finish() {
        super.finish()
        // 只有在已注册的情况下才注销receiver
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(uninstall_receiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                checkHeightAndFinish()
            }

            else -> {}
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 递归查找ViewGroup中的NestedScrollView
     */
    private fun findNestedScrollView(viewGroup: ViewGroup): NestedScrollView? {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is NestedScrollView) {
                return child
            } else if (child is ViewGroup) {
                val result = findNestedScrollView(child)
                if (result != null) {
                    return result
                }
            }
        }
        return null
    }

    companion object {
        // 使用BaseActivity中定义的常量，保持向后兼容
        const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"  // 旧的key，用于兼容
    }
}
