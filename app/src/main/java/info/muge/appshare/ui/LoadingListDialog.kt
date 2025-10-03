package info.muge.appshare.ui

import android.content.Context
import android.view.View
import info.muge.appshare.R

/**
 * 加载列表对话框
 */
class LoadingListDialog(context: Context) : ProgressDialog(
    context,
    context.resources.getString(R.string.dialog_loading_title)
) {

    init {
        att.visibility = View.GONE
        progressBar.max = 100
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }

    fun setProgress(progress: Int, total: Int) {
        progressBar.max = total
        if (progress > progressBar.max) return
        progressBar.progress = progress
        att_left.text = "$progress/${progressBar.max}"
        att_right.text = "${(progress.toFloat() / total * 100).toInt()}%"
    }
}

