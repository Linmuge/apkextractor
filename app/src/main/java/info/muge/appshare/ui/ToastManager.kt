package info.muge.appshare.ui

import android.content.Context
import android.widget.Toast
import info.muge.appshare.Global

/**
 * Toast管理器
 */
object ToastManager {

    private var toast: Toast? = null

    fun showToast(context: Context, content: String, length: Int) {
        Global.handler.post {
            toast?.cancel()
            toast = null
            toast = Toast.makeText(context, content, length)
            toast?.show()
        }
    }
}

