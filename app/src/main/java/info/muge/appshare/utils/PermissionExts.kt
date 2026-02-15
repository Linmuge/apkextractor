package info.muge.appshare.utils

import android.app.Activity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.hjq.permissions.permission.base.IPermission

object PermissionExts {
    /*读取应用列表权限*/
    fun requestreadInstallApps(activity: Activity, listener: () -> Unit) {
        if (!SPUtil.getGlobalSharedPreferences(activity).getBoolean("show_app", false)) {
            try {
                XXPermissions.with(activity).permission(PermissionLists.getGetInstalledAppsPermission()).request(object : OnPermissionCallback {

                    override fun onResult(
                        grantedList: List<IPermission?>,
                        deniedList: List<IPermission?>
                    ) {
                        if (grantedList.isNotEmpty() && deniedList.isEmpty()) {
                            SPUtil.getGlobalSharedPreferences(activity).edit().putBoolean("show_app", true).apply()
                            listener()
                        } else {
                            "授权失败，请前往App设置页面手动授权".toast()
                        }
                    }
                })
            } catch (e: Exception) {
                "授权失败，请前往App设置页面手动授权".toast()
            }
        }
    }

}