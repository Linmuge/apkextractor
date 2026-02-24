package info.muge.appshare.ui.screens.appdetail

import android.content.pm.ApplicationInfo
import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import info.muge.appshare.R
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.theme.AppDimens
import java.text.SimpleDateFormat
import java.util.Date

/**
 * 应用信息内容 - 与原 fragment_app_info.xml 完全一致
 */
@Composable
fun AppInfoContent(appItem: AppItem) {
    val context = LocalContext.current
    val packageInfo = appItem.getPackageInfo()
    val appInfo = packageInfo.applicationInfo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppDimens.Space.lg)
    ) {
        DetailCard {
            // 包名
            InfoItemVertical(
                label = stringResource(R.string.activity_detail_package_name),
                value = appItem.getPackageName(),
                onClick = { copyToClipboard(context, appItem.getPackageName()) }
            )
            InfoDivider()

            // 版本名
            InfoItemHorizontal(
                label = stringResource(R.string.activity_detail_version_name),
                value = appItem.getVersionName(),
                onClick = { copyToClipboard(context, appItem.getVersionName()) }
            )

            // 版本号
            InfoItemHorizontal(
                label = stringResource(R.string.activity_detail_version_code),
                value = appItem.getVersionCode().toString(),
                onClick = { copyToClipboard(context, appItem.getVersionCode().toString()) }
            )

            // 大小
            InfoItemHorizontal(
                label = stringResource(R.string.activity_detail_package_size),
                value = Formatter.formatFileSize(context, appItem.getSize()),
                onClick = { copyToClipboard(context, Formatter.formatFileSize(context, appItem.getSize())) }
            )

            InfoDivider()

            // 首次安装时间
            val installTime = SimpleDateFormat.getDateTimeInstance().format(Date(packageInfo.firstInstallTime))
            InfoItemHorizontal(
                label = stringResource(R.string.activity_detail_first_install_time),
                value = installTime,
                onClick = { copyToClipboard(context, installTime) }
            )

            // 上次更新时间
            val updateTime = SimpleDateFormat.getDateTimeInstance().format(Date(packageInfo.lastUpdateTime))
            InfoItemHorizontal(
                label = stringResource(R.string.activity_detail_last_update_time),
                value = updateTime,
                onClick = { copyToClipboard(context, updateTime) }
            )

            // 安装来源
            InfoItemHorizontal(
                label = stringResource(R.string.activity_detail_installer_name),
                value = appItem.getInstallSource(),
                onClick = { copyToClipboard(context, appItem.getInstallSource()) }
            )

            InfoDivider()

            // 最低API
            InfoItemHorizontal(
                label = stringResource(R.string.activity_detail_minimum_api),
                value = appInfo?.minSdkVersion?.toString() ?: "-",
                onClick = { copyToClipboard(context, appInfo?.minSdkVersion?.toString()) }
            )

            // 目标API
            InfoItemHorizontal(
                label = stringResource(R.string.activity_detail_target_api),
                value = appInfo?.targetSdkVersion?.toString() ?: "-",
                onClick = { copyToClipboard(context, appInfo?.targetSdkVersion?.toString()) }
            )

            // 系统应用
            val isSystemApp = ((appInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM) > 0
            val systemAppText = if (isSystemApp) stringResource(R.string.word_yes) else stringResource(R.string.word_no)
            InfoItemHorizontal(
                label = stringResource(R.string.activity_detail_is_system_app),
                value = systemAppText,
                onClick = { copyToClipboard(context, systemAppText) }
            )

            // UID
            InfoItemHorizontal(
                label = stringResource(R.string.activity_detail_uid),
                value = appInfo?.uid?.toString() ?: "-",
                onClick = { copyToClipboard(context, appInfo?.uid?.toString()) }
            )

            InfoDivider()

            // 路径
            InfoItemVertical(
                label = stringResource(R.string.activity_detail_path),
                value = appInfo?.sourceDir ?: "-",
                onClick = { copyToClipboard(context, appInfo?.sourceDir) }
            )

            // 主启动类
            InfoItemVertical(
                label = stringResource(R.string.activity_detail_launch_intent),
                value = appItem.getLaunchingClass() ?: "-",
                onClick = { copyToClipboard(context, appItem.getLaunchingClass()) }
            )

            InfoDivider()

            // 数据目录
            InfoItemVertical(
                label = stringResource(R.string.activity_detail_data_dir),
                value = appInfo?.dataDir ?: "-",
                onClick = { copyToClipboard(context, appInfo?.dataDir) }
            )

            // Native库目录
            InfoItemVertical(
                label = stringResource(R.string.activity_detail_native_lib_dir),
                value = appInfo?.nativeLibraryDir ?: "-",
                onClick = { copyToClipboard(context, appInfo?.nativeLibraryDir) }
            )

            // 进程名
            InfoItemVertical(
                label = stringResource(R.string.activity_detail_process_name),
                value = appInfo?.processName ?: "-",
                onClick = { copyToClipboard(context, appInfo?.processName) }
            )

            InfoDivider()

            // Flags
            val flagsString = getFlagsString(appInfo?.flags ?: 0)
            InfoItemVertical(
                label = stringResource(R.string.activity_detail_flags),
                value = flagsString,
                onClick = { copyToClipboard(context, flagsString) }
            )
        }
    }
}

private fun getFlagsString(flags: Int): String {
    val flagList = mutableListOf<String>()
    if (flags and ApplicationInfo.FLAG_SYSTEM != 0) flagList.add("SYSTEM")
    if (flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) flagList.add("DEBUGGABLE")
    if (flags and ApplicationInfo.FLAG_HAS_CODE != 0) flagList.add("HAS_CODE")
    if (flags and ApplicationInfo.FLAG_PERSISTENT != 0) flagList.add("PERSISTENT")
    if (flags and ApplicationInfo.FLAG_FACTORY_TEST != 0) flagList.add("FACTORY_TEST")
    if (flags and ApplicationInfo.FLAG_ALLOW_TASK_REPARENTING != 0) flagList.add("ALLOW_TASK_REPARENTING")
    if (flags and ApplicationInfo.FLAG_ALLOW_CLEAR_USER_DATA != 0) flagList.add("ALLOW_CLEAR_USER_DATA")
    if (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) flagList.add("UPDATED_SYSTEM_APP")
    if (flags and ApplicationInfo.FLAG_TEST_ONLY != 0) flagList.add("TEST_ONLY")
    if (flags and ApplicationInfo.FLAG_VM_SAFE_MODE != 0) flagList.add("VM_SAFE_MODE")
    if (flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0) flagList.add("ALLOW_BACKUP")
    if (flags and ApplicationInfo.FLAG_KILL_AFTER_RESTORE != 0) flagList.add("KILL_AFTER_RESTORE")
    if (flags and ApplicationInfo.FLAG_RESTORE_ANY_VERSION != 0) flagList.add("RESTORE_ANY_VERSION")
    if (flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE != 0) flagList.add("EXTERNAL_STORAGE")
    if (flags and ApplicationInfo.FLAG_LARGE_HEAP != 0) flagList.add("LARGE_HEAP")
    if (flags and ApplicationInfo.FLAG_STOPPED != 0) flagList.add("STOPPED")
    if (flags and ApplicationInfo.FLAG_SUPPORTS_RTL != 0) flagList.add("SUPPORTS_RTL")
    if (flags and ApplicationInfo.FLAG_INSTALLED != 0) flagList.add("INSTALLED")
    if (flags and ApplicationInfo.FLAG_IS_DATA_ONLY != 0) flagList.add("IS_DATA_ONLY")
    return if (flagList.isEmpty()) "-" else flagList.joinToString(", ")
}
