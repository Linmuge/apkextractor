package info.muge.appshare.tasks

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.view.View
import com.google.android.material.snackbar.Snackbar
import info.muge.appshare.Global
import info.muge.appshare.R
import info.muge.appshare.ui.SignatureView
import info.muge.appshare.utils.EnvironmentUtil

/**
 * 获取签名信息任务
 */
class GetSignatureInfoTask(
    private val activity: Activity,
    private val packageInfo: PackageInfo,
    private val signatureView: SignatureView,
    private val callback: CompletedCallback
) : Thread() {

    companion object {
        private val sign_infos_cache = HashMap<String, Array<String>>()
        private val md5_cache = HashMap<PackageInfo, String>()
        private val sha1_cache = HashMap<PackageInfo, String>()
        private val sha256_cache = HashMap<PackageInfo, String>()

        @JvmStatic
        @Synchronized
        fun clearCache() {
            sign_infos_cache.clear()
            md5_cache.clear()
            sha1_cache.clear()
            sha256_cache.clear()
        }
    }

    override fun run() {
        super.run()
        
        val sign_infos1: Array<String>
        val md5_1: String
        val sha1_1: String
        val sha256_1: String

        synchronized(sign_infos_cache) {
            val sourceDir = packageInfo.applicationInfo?.sourceDir ?: ""
            sign_infos1 = if (sign_infos_cache[sourceDir] != null) {
                sign_infos_cache[sourceDir]!!
            } else {
                val infos = EnvironmentUtil.getAPKSignInfo(sourceDir)
                sign_infos_cache[sourceDir] = infos
                infos
            }
        }

        synchronized(md5_cache) {
            md5_1 = if (md5_cache[packageInfo] != null) {
                md5_cache[packageInfo]!!
            } else {
                val md5 = EnvironmentUtil.getSignatureMD5StringOfPackageInfo(packageInfo)
                md5_cache[packageInfo] = md5
                md5
            }
        }

        synchronized(sha1_cache) {
            sha1_1 = if (sha1_cache[packageInfo] != null) {
                sha1_cache[packageInfo]!!
            } else {
                val sha1 = EnvironmentUtil.getSignatureSHA1OfPackageInfo(packageInfo)
                sha1_cache[packageInfo] = sha1
                sha1
            }
        }

        synchronized(sha256_cache) {
            sha256_1 = if (sha256_cache[packageInfo] != null) {
                sha256_cache[packageInfo]!!
            } else {
                val sha256 = EnvironmentUtil.getSignatureSHA256OfPackageInfo(packageInfo)
                sha256_cache[packageInfo] = sha256
                sha256
            }
        }

        val sign_infos = sign_infos1
        val md5 = md5_1
        val sha1 = sha1_1
        val sha256 = sha256_1

        Global.handler.post {
            signatureView.tv_sub_value.text = sign_infos[0]
            signatureView.tv_iss_value.text = sign_infos[1]
            signatureView.tv_serial_value.text = sign_infos[2]
            signatureView.tv_start.text = sign_infos[3]
            signatureView.tv_end.text = sign_infos[4]
            signatureView.tv_md5.text = md5
            signatureView.tv_sha1.text = sha1
            signatureView.tv_sha256.text = sha256

            signatureView.linearLayout_sub.setOnClickListener {
                clip2ClipboardAndShowSnackbar(signatureView.tv_sub_value.text.toString())
            }
            signatureView.linearLayout_iss.setOnClickListener {
                clip2ClipboardAndShowSnackbar(signatureView.tv_iss_value.text.toString())
            }
            signatureView.linearLayout_serial.setOnClickListener {
                clip2ClipboardAndShowSnackbar(signatureView.tv_serial_value.text.toString())
            }
            signatureView.linearLayout_start.setOnClickListener {
                clip2ClipboardAndShowSnackbar(signatureView.tv_start.text.toString())
            }
            signatureView.linearLayout_end.setOnClickListener {
                clip2ClipboardAndShowSnackbar(signatureView.tv_end.text.toString())
            }
            signatureView.linearLayout_md5.setOnClickListener {
                clip2ClipboardAndShowSnackbar(signatureView.tv_md5.text.toString())
            }
            signatureView.linearLayout_sha1.setOnClickListener {
                clip2ClipboardAndShowSnackbar(signatureView.tv_sha1.text.toString())
            }
            signatureView.linearLayout_sha256.setOnClickListener {
                clip2ClipboardAndShowSnackbar(signatureView.tv_sha256.text.toString())
            }
            
            signatureView.root.visibility = View.VISIBLE
            callback.onCompleted()
        }
    }

    private fun clip2ClipboardAndShowSnackbar(s: String) {
        try {
            val manager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText("message", s))
            Snackbar.make(
                activity.findViewById(android.R.id.content),
                activity.resources.getString(R.string.snack_bar_clipboard),
                Snackbar.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    interface CompletedCallback {
        fun onCompleted()
    }
}

