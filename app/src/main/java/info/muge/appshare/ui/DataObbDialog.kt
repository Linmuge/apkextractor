package info.muge.appshare.ui

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import info.muge.appshare.Global
import info.muge.appshare.R
import info.muge.appshare.items.AppItem
import info.muge.appshare.utils.FileUtil
import info.muge.appshare.utils.StorageUtil
import java.io.File

/**
 * Data和Obb选择对话框
 */
class DataObbDialog(
    context: Context,
    export_list: List<AppItem>,
    private val callback: DialogDataObbConfirmedCallback?
) : AlertDialog(context), View.OnClickListener {

    private val view: View
    private val list = ArrayList<AppItem>()
    private val list_data_controllable = ArrayList<AppItem>()
    private val list_obb_controllable = ArrayList<AppItem>()
    private lateinit var cb_data: CheckBox
    private lateinit var cb_obb: CheckBox

    init {
        for (appItem in export_list) {
            list.add(AppItem(appItem, false, false))
        }
        
        view = LayoutInflater.from(context).inflate(R.layout.dialog_data_obb, null)
        cb_data = view.findViewById(R.id.dialog_checkbox_data)
        cb_obb = view.findViewById(R.id.dialog_checkbox_obb)
        val tv_att = view.findViewById<TextView>(R.id.data_obb_att)
        tv_att.text = context.resources.getString(R.string.dialog_data_obb_message)
        
        setView(view)
        setTitle(context.resources.getString(R.string.dialog_data_obb_title))
        
        setButton(BUTTON_POSITIVE, context.resources.getString(R.string.dialog_button_confirm)) { _, _ -> }
        setButton(BUTTON_NEGATIVE, context.resources.getString(R.string.dialog_button_cancel)) { _, _ -> }
    }

    override fun show() {
        super.show()
        getButton(BUTTON_POSITIVE).setOnClickListener(null)
        
        Thread {
            synchronized(this@DataObbDialog) {
                var data = 0L
                var obb = 0L
                
                for (item in list) {
                    val data_item = FileUtil.getFileOrFolderSize(
                        File("${StorageUtil.getMainExternalStoragePath()}/android/data/${item.getPackageName()}")
                    )
                    val obb_item = FileUtil.getFileOrFolderSize(
                        File("${StorageUtil.getMainExternalStoragePath()}/android/obb/${item.getPackageName()}")
                    )
                    data += data_item
                    obb += obb_item
                    
                    if (data_item > 0) list_data_controllable.add(item)
                    if (obb_item > 0) list_obb_controllable.add(item)
                }
                
                val data_total = data
                val obb_total = obb
                
                Global.handler.post {
                    if (data_total == 0L && obb_total == 0L) {
                        cancel()
                        callback?.onDialogDataObbConfirmed(list)
                        return@post
                    }
                    
                    view.findViewById<View>(R.id.dialog_data_obb_wait_area).visibility = View.GONE
                    view.findViewById<View>(R.id.dialog_data_obb_show_area).visibility = View.VISIBLE
                    cb_data.isEnabled = data_total > 0
                    cb_obb.isEnabled = obb_total > 0
                    cb_data.text = "Data(${Formatter.formatFileSize(context, data_total)})"
                    cb_obb.text = "Obb(${Formatter.formatFileSize(context, obb_total)})"
                    getButton(BUTTON_POSITIVE).setOnClickListener(this@DataObbDialog)
                }
            }
        }.start()
    }

    override fun onClick(v: View) {
        if (v == getButton(BUTTON_POSITIVE)) {
            if (cb_data.isChecked) {
                for (item in list_data_controllable) {
                    item.exportData = true
                }
            }
            if (cb_obb.isChecked) {
                for (item in list_obb_controllable) {
                    item.exportObb = true
                }
            }
            callback?.onDialogDataObbConfirmed(list)
            cancel()
        }
    }

    /**
     * Data和Obb确认回调
     */
    interface DialogDataObbConfirmedCallback {
        fun onDialogDataObbConfirmed(export_list: List<AppItem>)
    }
}

