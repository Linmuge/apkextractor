package info.muge.appshare.ui

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.LayoutInflater
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import info.muge.appshare.Constants
import info.muge.appshare.R
import info.muge.appshare.utils.SPUtil

/**
 * 导入项排序配置对话框
 */
class ImportItemSortConfigDialog(context: Context, private val callback: SortConfigDialogCallback?) {

    private val settings: SharedPreferences = SPUtil.getGlobalSharedPreferences(context)
    private val dialog: AlertDialog

    init {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sort_import, null)

        val ra_default = dialogView.findViewById<RadioButton>(R.id.sort_ra_default)
        val ra_name_ascending = dialogView.findViewById<RadioButton>(R.id.sort_ra_ascending_filename)
        val ra_name_descending = dialogView.findViewById<RadioButton>(R.id.sort_ra_descending_filename)
        val ra_size_ascending = dialogView.findViewById<RadioButton>(R.id.sort_ra_ascending_filesize)
        val ra_size_descending = dialogView.findViewById<RadioButton>(R.id.sort_ra_descending_filesize)
        val ra_time_ascending = dialogView.findViewById<RadioButton>(R.id.sort_ra_ascending_modified_time)
        val ra_time_descending = dialogView.findViewById<RadioButton>(R.id.sort_ra_descending_modified_time)

        val sort_config = settings.getInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS, 0)
        ra_default.isChecked = sort_config == 0
        ra_name_ascending.isChecked = sort_config == 1
        ra_name_descending.isChecked = sort_config == 2
        ra_size_ascending.isChecked = sort_config == 3
        ra_size_descending.isChecked = sort_config == 4
        ra_time_ascending.isChecked = sort_config == 5
        ra_time_descending.isChecked = sort_config == 6

        val onClickListener: (Int) -> Unit = { sortConfig ->
            val editor = settings.edit()
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS, sortConfig)
            editor.apply()
            dialog.dismiss()
            callback?.onOptionSelected(sortConfig)
        }

        ra_default.setOnClickListener { onClickListener(0) }
        ra_name_ascending.setOnClickListener { onClickListener(1) }
        ra_name_descending.setOnClickListener { onClickListener(2) }
        ra_size_ascending.setOnClickListener { onClickListener(3) }
        ra_size_descending.setOnClickListener { onClickListener(4) }
        ra_time_ascending.setOnClickListener { onClickListener(5) }
        ra_time_descending.setOnClickListener { onClickListener(6) }

        // 使用MaterialAlertDialogBuilder创建MD3风格对话框
        val builder = MaterialAlertDialogBuilder(
            context,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog
        )
            .setTitle(context.resources.getString(R.string.dialog_sort_import_item_title))
            .setView(dialogView)
            .setNegativeButton(context.resources.getString(R.string.dialog_button_cancel)) { _, _ -> }

        dialog = builder.create()

        // 设置圆角和其他MD3样式
        dialog.setOnShowListener {
            val window = dialog.window
            if (window != null) {
                // 应用MD3大圆角
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.setBackgroundDrawableResource(com.google.android.material.R.drawable.mtrl_dialog_background)
                    // 增加圆角大小 - 如果默认圆角不够大
                    val drawable = GradientDrawable()
                    drawable.cornerRadius = 32f // 设置更大的圆角半径
                    drawable.setColor(
                        context.resources.getColor(com.google.android.material.R.color.mtrl_btn_bg_color_selector)
                    )
                    window.setBackgroundDrawable(drawable)
                } else {
                    window.setBackgroundDrawableResource(com.google.android.material.R.drawable.mtrl_dialog_background)
                }
            }
        }
    }

    fun show() {
        dialog.show()
    }

    fun cancel() {
        dialog.dismiss()
    }
}

