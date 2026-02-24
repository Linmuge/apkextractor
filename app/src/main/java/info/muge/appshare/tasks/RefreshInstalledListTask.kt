package info.muge.appshare.tasks

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import info.muge.appshare.Constants
import info.muge.appshare.Global
import info.muge.appshare.items.AppItem
import info.muge.appshare.utils.SPUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * 刷新已安装的应用列表（协程版）
 */
class RefreshInstalledListTask(
    private val context: Context
) {
    /**
     * 执行刷新（挂起函数，在 IO 线程执行）
     * @param onProgressStarted 进度开始回调（主线程，非阻塞）
     * @param onProgressUpdated 进度更新回调（主线程，非阻塞）
     * @return 排序后的应用列表
     */
    suspend fun execute(
        onProgressStarted: ((total: Int) -> Unit)? = null,
        onProgressUpdated: ((current: Int, total: Int) -> Unit)? = null
    ): List<AppItem> = withContext(Dispatchers.IO) {
        val mainScope = CoroutineScope(Dispatchers.Main)
        val settings = SPUtil.getGlobalSharedPreferences(context)
        val flagSystem = settings.getBoolean(
            Constants.PREFERENCE_SHOW_SYSTEM_APP,
            Constants.PREFERENCE_SHOW_SYSTEM_APP_DEFAULT
        )

        val manager = context.applicationContext.packageManager

        var flag = 0
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS, Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT)) {
            flag = flag or PackageManager.GET_PERMISSIONS
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES, Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT)) {
            flag = flag or PackageManager.GET_ACTIVITIES
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS, Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT)) {
            flag = flag or PackageManager.GET_RECEIVERS
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_APK_SIGNATURE, Constants.PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT)) {
            flag = flag or PackageManager.GET_SIGNATURES
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_SERVICES, Constants.PREFERENCE_LOAD_SERVICES_DEFAULT)) {
            flag = flag or PackageManager.GET_SERVICES
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_PROVIDERS, Constants.PREFERENCE_LOAD_PROVIDERS_DEFAULT)) {
            flag = flag or PackageManager.GET_PROVIDERS
        }

        val list = ArrayList<android.content.pm.PackageInfo>()
        synchronized(RefreshInstalledListTask::class.java) {
            list.clear()
            list.addAll(manager.getInstalledPackages(flag))
        }

        // 非阻塞 fire-and-forget，匹配原 Global.handler.post 语义
        mainScope.launch {
            onProgressStarted?.invoke(list.size)
        }

        val listSum = ArrayList<AppItem>()
        for (i in list.indices) {
            ensureActive()

            val info = list[i]
            val isSystemApp = (info.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) > 0
            val current = i + 1

            // 非阻塞 fire-and-forget，不挂起 IO 协程
            mainScope.launch {
                onProgressUpdated?.invoke(current, list.size)
            }

            if (!flagSystem && isSystemApp) continue

            listSum.add(AppItem(context, info))
        }

        ensureActive()

        AppItem.sort_config = settings.getInt(Constants.PREFERENCE_SORT_CONFIG, 0)
        Collections.sort(listSum)

        synchronized(Global.app_list) {
            Global.app_list.clear()
            Global.app_list.addAll(listSum)
        }

        listSum
    }

    /**
     * 刷新已安装列表任务回调（保留向后兼容）
     */
    interface RefreshInstalledListTaskCallback {
        fun onRefreshProgressStarted(total: Int)
        fun onRefreshProgressUpdated(current: Int, total: Int)
        fun onRefreshCompleted(appList: List<AppItem>)
    }
}
