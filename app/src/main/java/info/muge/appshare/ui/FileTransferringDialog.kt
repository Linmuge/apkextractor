package info.muge.appshare.ui

import android.content.Context
import android.text.format.Formatter
import java.text.DecimalFormat

/**
 * 文件传输对话框
 */
class FileTransferringDialog(context: Context, title: String) : ProgressDialog(context, title) {

    init {
        setCancelable(false)
    }

    fun setCurrentFileInfo(info: String) {
        att.text = info
    }

    fun setProgressOfSending(progress: Long, total: Long) {
        if (progress < 0 || total <= 0) return
        if (progress > total) return
        
        progressBar.max = (total / 1024).toInt()
        progressBar.progress = (progress / 1024).toInt()
        
        val dm = DecimalFormat("#.00")
        val percent = (dm.format(progress.toDouble() / total).toDouble() * 100).toInt()
        att_right.text = "${Formatter.formatFileSize(context, progress)}/${Formatter.formatFileSize(context, total)}($percent%)"
    }

    fun setSpeed(speed: Long) {
        att_left.text = "${Formatter.formatFileSize(context, speed)}/s"
    }
}

