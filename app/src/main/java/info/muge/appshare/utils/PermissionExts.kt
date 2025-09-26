package info.muge.appshare.utils

import androidx.fragment.app.FragmentActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionGroups
import com.hjq.permissions.permission.PermissionLists
import com.hjq.permissions.permission.base.IPermission

object PermissionExts {
    /*读取应用列表权限*/
    fun requestreadInstallApps(context: FragmentActivity,listener:()->Unit){
        if (!SPUtil.getGlobalSharedPreferences(context).getBoolean("show_app", false)){
            try {
                XXPermissions.with(context).permission(PermissionLists.getGetInstalledAppsPermission()).request(object : OnPermissionCallback {

                    override fun onResult(
                        grantedList: List<IPermission?>,
                        deniedList: List<IPermission?>
                    ) {
                        if (grantedList.isNotEmpty() && deniedList.isEmpty()){
                            SPUtil.getGlobalSharedPreferences(context).edit().putBoolean("show_app", true).apply()
                            listener()
                        }else{
                            "授权失败，请前往App设置页面手动授权".toast()
                        }
                    }
                })
            }catch (e: Exception){
                "授权失败，请前往App设置页面手动授权".toast()
            }
        }
    }

}