package info.muge.appshare.tasks

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import info.muge.appshare.Constants
import info.muge.appshare.Global
import info.muge.appshare.items.AppItem
import info.muge.appshare.utils.SPUtil
import java.util.Collections

/**
 * 刷新已安装的应用列表
 */
class RefreshInstalledListTask(
    private val context: Context,
    private val listener: RefreshInstalledListTaskCallback?
) : Thread() {

    private val flag_system: Boolean = SPUtil.getGlobalSharedPreferences(context)
        .getBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP, Constants.PREFERENCE_SHOW_SYSTEM_APP_DEFAULT)
    
    private val list_sum = ArrayList<AppItem>()
    
    @Volatile
    private var isInterrupted = false

    override fun run() {
        val manager = context.applicationContext.packageManager
        val settings = SPUtil.getGlobalSharedPreferences(context)
        
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
        // 加锁是在多线程请求已安装列表时PackageManager可能会抛异常 ParceledListSlice: Failure retrieving array;
        synchronized(RefreshInstalledListTask::class.java) {
            list.clear()
            list.addAll(manager.getInstalledPackages(flag))
        }
        
        Global.handler.post {
            listener?.onRefreshProgressStarted(list.size)
        }
        
        for (i in list.indices) {
            if (isInterrupted) return
            
            val info = list[i]
            val info_is_system_app = (info.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) > 0
            val current = i + 1
            
            Global.handler.post {
                listener?.onRefreshProgressUpdated(current, list.size)
            }
            
            if (!flag_system && info_is_system_app) continue
            
            list_sum.add(AppItem(context, info))
        }
        
        if (isInterrupted) return
        
        AppItem.sort_config = settings.getInt(Constants.PREFERENCE_SORT_CONFIG, 0)
        Collections.sort(list_sum)
        
        synchronized(Global.app_list) {
            Global.app_list.clear()
            Global.app_list.addAll(list_sum) // 向全局list保存一个引用
        }

        Global.handler.post {
            listener?.onRefreshCompleted(list_sum)
        }
    }

    fun setInterrupted() {
        this.isInterrupted = true
    }

    /**
     * 刷新已安装列表任务回调
     */
    interface RefreshInstalledListTaskCallback {
        fun onRefreshProgressStarted(total: Int)
        fun onRefreshProgressUpdated(current: Int, total: Int)
        fun onRefreshCompleted(appList: List<AppItem>)
    }
}

