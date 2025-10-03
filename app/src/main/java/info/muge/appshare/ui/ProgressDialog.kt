package info.muge.appshare.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckBox
import info.muge.appshare.R

/**
 * 进度对话框基类
 */
abstract class ProgressDialog(context: Context, title: String) : AlertDialog(context) {

    @JvmField
    protected val progressBar: ProgressBar
    @JvmField
    protected val att: TextView
    @JvmField
    protected val att_left: TextView
    @JvmField
    protected val att_right: TextView

    init {
        val dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_with_progress, null)
        setView(dialog_view)
        progressBar = dialog_view.findViewById(R.id.dialog_progress_bar)
        att = dialog_view.findViewById(R.id.dialog_att)
        att_left = dialog_view.findViewById(R.id.dialog_att_left)
        att_right = dialog_view.findViewById(R.id.dialog_att_right)
        
        dialog_view.findViewById<AppCompatCheckBox>(R.id.dialog_progress_keep_on)
            .setOnCheckedChangeListener { _, isChecked ->
                try {
                    if (isChecked) {
                        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        setTitle(title)
    }

    override fun show() {
        super.show()
        try {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

