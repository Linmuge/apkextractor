package info.muge.appshare.utils

import androidx.fragment.app.FragmentActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions

object PermissionExts {
    /*读取应用列表权限*/
    fun requestreadInstallApps(context: FragmentActivity,listener:()->Unit){
        if (!SPUtil.getGlobalSharedPreferences(context).getBoolean("show_app", false)){
            try {
                XXPermissions.with(context).permission(Permission.GET_INSTALLED_APPS).request(object : OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                        SPUtil.getGlobalSharedPreferences(context).edit().putBoolean("show_app", true).apply()
                        listener()
                    }
                    override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                        "授权失败，请前往App设置页面手动授权".toast()
                    }
                })
            }catch (e: Exception){
                "授权失败，请前往App设置页面手动授权".toast()
            }
        }
    }

}