package info.muge.appshare.fragments.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import info.muge.appshare.Global
import info.muge.appshare.R
import info.muge.appshare.items.AppItem

/**
 * 应用详情页 Fragment 基类
 * 提供公共功能：获取 AppItem、复制到剪贴板等
 */
abstract class BaseDetailFragment : Fragment() {

    companion object {
        const val ARG_PACKAGE_NAME = "package_name"
    }

    protected var appItem: AppItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 从 arguments 获取包名，然后从全局列表中查找 AppItem
        val packageName = arguments?.getString(ARG_PACKAGE_NAME)
        if (packageName != null) {
            synchronized(Global.app_list) {
                appItem = Global.getAppItemByPackageNameFromList(Global.app_list, packageName)
            }
        }
    }

    /**
     * 复制文本到剪贴板并显示 Snackbar 提示
     */
    protected fun copyToClipboard(text: String?) {
        if (text.isNullOrEmpty()) return
        
        try {
            val context = requireContext()
            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText("message", text))
            
            view?.let { rootView ->
                Snackbar.make(
                    rootView,
                    getString(R.string.snack_bar_clipboard),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 创建带包名参数的 Fragment 实例
     */
    protected fun <T : BaseDetailFragment> T.withPackageName(packageName: String): T {
        arguments = Bundle().apply {
            putString(ARG_PACKAGE_NAME, packageName)
        }
        return this
    }
}
