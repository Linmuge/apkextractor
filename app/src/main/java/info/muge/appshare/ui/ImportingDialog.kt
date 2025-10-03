package info.muge.appshare.ui

import android.content.Context
import android.text.format.Formatter
import info.muge.appshare.R
import java.text.DecimalFormat

/**
 * 导入对话框
 */
class ImportingDialog(context: Context, private val total: Long) : ProgressDialog(
    context,
    context.resources.getString(R.string.dialog_import_title)
) {

    init {
        progressBar.max = (total / 1024).toInt()
        setCancelable(false)
    }

    fun setCurrentWritingName(filename: String) {
        att.text = "${context.resources.getString(R.string.dialog_import_msg)}$filename"
    }

    fun setProgress(progress: Long) {
        progressBar.progress = (progress / 1024).toInt()
        val dm = DecimalFormat("#.00")
        val percent = (dm.format(progress.toDouble() / total).toDouble() * 100).toInt()
        att_right.text = "${Formatter.formatFileSize(context, progress)}/${Formatter.formatFileSize(context, total)}($percent%)"
    }

    fun setSpeed(speed: Long) {
        att_left.text = "${Formatter.formatFileSize(context, speed)}/s"
    }
}

