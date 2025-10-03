package info.muge.appshare.ui

import android.content.Context
import android.text.format.Formatter
import info.muge.appshare.R
import info.muge.appshare.items.AppItem
import java.text.DecimalFormat

/**
 * 导出对话框
 */
class ExportingDialog(context: Context) : ProgressDialog(context, context.resources.getString(R.string.dialog_export_title)) {

    init {
        att.text = context.resources.getString(R.string.dialog_wait)
    }

    fun setProgressOfApp(current: Int, total: Int, item: AppItem, write_path: String) {
        setTitle("${context.resources.getString(R.string.dialog_export_title)}($current/$total):${item.getAppName()}")
        setIcon(item.getIcon())
        att.text = "${context.resources.getString(R.string.dialog_export_msg_apk)}$write_path"
    }

    fun setProgressOfWriteBytes(current: Long, total: Long) {
        if (current < 0) return
        if (current > total) return
        
        progressBar.max = (total / 1024).toInt()
        progressBar.progress = (current / 1024).toInt()
        
        val dm = DecimalFormat("#.00")
        val percent = (dm.format(current.toDouble() / total).toDouble() * 100).toInt()
        att_right.text = "${Formatter.formatFileSize(context, current)}/${Formatter.formatFileSize(context, total)}($percent%)"
    }

    fun setSpeed(bytes: Long) {
        att_left.text = "${Formatter.formatFileSize(context, bytes)}/s"
    }

    fun setProgressOfCurrentZipFile(write_path: String) {
        att.text = "${context.resources.getString(R.string.dialog_export_zip)}$write_path"
    }
}

