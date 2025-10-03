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
 * 应用项排序配置对话框
 */
class AppItemSortConfigDialog(context: Context, private val callback: SortConfigDialogCallback?) {

    private val settings: SharedPreferences = SPUtil.getGlobalSharedPreferences(context)
    private val dialog: AlertDialog

    init {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sort, null)

        val sort = settings.getInt(Constants.PREFERENCE_SORT_CONFIG, 0)
        val ra_default = dialogView.findViewById<RadioButton>(R.id.sort_ra_default)
        val ra_name_ascend = dialogView.findViewById<RadioButton>(R.id.sort_ra_ascending_appname)
        val ra_name_descend = dialogView.findViewById<RadioButton>(R.id.sort_ra_descending_appname)
        val ra_size_ascend = dialogView.findViewById<RadioButton>(R.id.sort_ra_ascending_appsize)
        val ra_size_descend = dialogView.findViewById<RadioButton>(R.id.sort_ra_descending_appsize)
        val ra_update_time_ascend = dialogView.findViewById<RadioButton>(R.id.sort_ra_ascending_date)
        val ra_update_time_descend = dialogView.findViewById<RadioButton>(R.id.sort_ra_descending_date)
        val ra_install_time_ascend = dialogView.findViewById<RadioButton>(R.id.sort_ra_ascending_install_date)
        val ra_install_time_descend = dialogView.findViewById<RadioButton>(R.id.sort_ra_descending_install_date)
        val ra_package_name_ascend = dialogView.findViewById<RadioButton>(R.id.sort_ra_ascending_package_name)
        val ra_package_name_descend = dialogView.findViewById<RadioButton>(R.id.sort_ra_descending_package_name)

        ra_default.isChecked = sort == 0
        ra_name_ascend.isChecked = sort == 1
        ra_name_descend.isChecked = sort == 2
        ra_size_ascend.isChecked = sort == 3
        ra_size_descend.isChecked = sort == 4
        ra_update_time_ascend.isChecked = sort == 5
        ra_update_time_descend.isChecked = sort == 6
        ra_install_time_ascend.isChecked = sort == 7
        ra_install_time_descend.isChecked = sort == 8
        ra_package_name_ascend.isChecked = sort == 9
        ra_package_name_descend.isChecked = sort == 10

        val onClickListener: (Int) -> Unit = { sortConfig ->
            val editor = settings.edit()
            editor.putInt(Constants.PREFERENCE_SORT_CONFIG, sortConfig)
            editor.apply()
            dialog.dismiss()
            callback?.onOptionSelected(sortConfig)
        }

        ra_default.setOnClickListener { onClickListener(0) }
        ra_name_ascend.setOnClickListener { onClickListener(1) }
        ra_name_descend.setOnClickListener { onClickListener(2) }
        ra_size_ascend.setOnClickListener { onClickListener(3) }
        ra_size_descend.setOnClickListener { onClickListener(4) }
        ra_update_time_ascend.setOnClickListener { onClickListener(5) }
        ra_update_time_descend.setOnClickListener { onClickListener(6) }
        ra_install_time_ascend.setOnClickListener { onClickListener(7) }
        ra_install_time_descend.setOnClickListener { onClickListener(8) }
        ra_package_name_ascend.setOnClickListener { onClickListener(9) }
        ra_package_name_descend.setOnClickListener { onClickListener(10) }

        // 使用MaterialAlertDialogBuilder创建MD3风格对话框
        val builder = MaterialAlertDialogBuilder(
            context,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog
        )
            .setTitle(context.resources.getString(R.string.dialog_sort_appitem_title))
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

