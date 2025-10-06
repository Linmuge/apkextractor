package info.muge.appshare.ui

import android.content.Context
import android.content.SharedPreferences
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import info.muge.appshare.Constants
import info.muge.appshare.R
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.colorSurfaceContainer
import java.util.Calendar

/**
 * 导出规则对话框
 * 编辑导出规则的UI，确定后会保存至SharedPreferences中
 */
class ExportRuleDialog(private val context: Context, style: Int) {

    private val dialog: AlertDialog
    private val builder: MaterialAlertDialogBuilder
    private val edit_apk: TextInputEditText
    private val edit_zip: TextInputEditText
    private val preview: TextView
    private val spinner: MaterialAutoCompleteTextView
    private val settings: SharedPreferences
    private val dialogView: View

    init {
        settings = SPUtil.getGlobalSharedPreferences(context)

        // 使用 MaterialAlertDialogBuilder 创建对话框
        builder = MaterialAlertDialogBuilder(context, style)

        dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rule, null)
        edit_apk = dialogView.findViewById(R.id.filename_apk)
        edit_zip = dialogView.findViewById(R.id.filename_zip)
        preview = dialogView.findViewById(R.id.filename_preview)
        spinner = dialogView.findViewById(R.id.spinner_zip_level)

        dialogView.findViewById<TextView>(R.id.filename_zip_end).text = ".${SPUtil.getCompressingExtensionName(context)}"
        edit_apk.setText(settings.getString(Constants.PREFERENCE_FILENAME_FONT_APK, Constants.PREFERENCE_FILENAME_FONT_DEFAULT))
        edit_zip.setText(settings.getString(Constants.PREFERENCE_FILENAME_FONT_ZIP, Constants.PREFERENCE_FILENAME_FONT_DEFAULT))
        preview.text = getFormatedExportFileName(edit_apk.text.toString(), edit_zip.text.toString())

        val zipLevels = arrayOf(
            context.resources.getString(R.string.zip_level_default),
            context.resources.getString(R.string.zip_level_stored),
            context.resources.getString(R.string.zip_level_low),
            context.resources.getString(R.string.zip_level_normal),
            context.resources.getString(R.string.zip_level_high)
        )

        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, zipLevels)
        spinner.setAdapter(adapter)
        spinner.inputType = InputType.TYPE_NULL
        spinner.isFocusable = true
        spinner.isFocusableInTouchMode = false
        spinner.isClickable = true

        val level_set = settings.getInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT)
        try {
            when (level_set) {
                Constants.ZIP_LEVEL_STORED -> spinner.setText(zipLevels[1], false)
                Constants.ZIP_LEVEL_LOW -> spinner.setText(zipLevels[2], false)
                Constants.ZIP_LEVEL_NORMAL -> spinner.setText(zipLevels[3], false)
                Constants.ZIP_LEVEL_HIGH -> spinner.setText(zipLevels[4], false)
                else -> spinner.setText(zipLevels[0], false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 检查APK文件名警告
        if (!edit_apk.text.toString().contains(Constants.FONT_APP_NAME) &&
            !edit_apk.text.toString().contains(Constants.FONT_APP_PACKAGE_NAME) &&
            !edit_apk.text.toString().contains(Constants.FONT_APP_VERSIONCODE) &&
            !edit_apk.text.toString().contains(Constants.FONT_APP_VERSIONNAME)
        ) {
            dialogView.findViewById<View>(R.id.filename_apk_warn).visibility = View.VISIBLE
        } else {
            dialogView.findViewById<View>(R.id.filename_apk_warn).visibility = View.GONE
        }

        // 检查ZIP文件名警告
        if (!edit_zip.text.toString().contains(Constants.FONT_APP_NAME) &&
            !edit_zip.text.toString().contains(Constants.FONT_APP_PACKAGE_NAME) &&
            !edit_zip.text.toString().contains(Constants.FONT_APP_VERSIONCODE) &&
            !edit_zip.text.toString().contains(Constants.FONT_APP_VERSIONNAME)
        ) {
            dialogView.findViewById<View>(R.id.filename_zip_warn).visibility = View.VISIBLE
        } else {
            dialogView.findViewById<View>(R.id.filename_zip_warn).visibility = View.GONE
        }

        // 配置对话框
        builder.setTitle(context.resources.getString(R.string.dialog_filename_title))
        builder.setView(dialogView)
        builder.setPositiveButton(context.resources.getString(R.string.dialog_button_confirm), null)
        builder.setNegativeButton(context.resources.getString(R.string.dialog_button_cancel), null)

        // APK文件名文本监听器
        edit_apk.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                preview.text = getFormatedExportFileName(edit_apk.text.toString(), edit_zip.text.toString())
                if (!edit_apk.text.toString().contains(Constants.FONT_APP_NAME) &&
                    !edit_apk.text.toString().contains(Constants.FONT_APP_PACKAGE_NAME) &&
                    !edit_apk.text.toString().contains(Constants.FONT_AUTO_SEQUENCE_NUMBER)
                ) {
                    dialogView.findViewById<View>(R.id.filename_apk_warn).visibility = View.VISIBLE
                } else {
                    dialogView.findViewById<View>(R.id.filename_apk_warn).visibility = View.GONE
                }
            }
        })

        // ZIP文件名文本监听器
        edit_zip.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                preview.text = getFormatedExportFileName(edit_apk.text.toString(), edit_zip.text.toString())
                if (!edit_zip.text.toString().contains(Constants.FONT_APP_NAME) &&
                    !edit_zip.text.toString().contains(Constants.FONT_APP_PACKAGE_NAME) &&
                    !edit_zip.text.toString().contains(Constants.FONT_AUTO_SEQUENCE_NUMBER)
                ) {
                    dialogView.findViewById<View>(R.id.filename_zip_warn).visibility = View.VISIBLE
                } else {
                    dialogView.findViewById<View>(R.id.filename_zip_warn).visibility = View.GONE
                }
            }
        })

        // 设置Chip点击监听器
        setupChipClickListener(dialogView, R.id.filename_appname, Constants.FONT_APP_NAME)
        setupChipClickListener(dialogView, R.id.filename_packagename, Constants.FONT_APP_PACKAGE_NAME)
        setupChipClickListener(dialogView, R.id.filename_version, Constants.FONT_APP_VERSIONNAME)
        setupChipClickListener(dialogView, R.id.filename_versioncode, Constants.FONT_APP_VERSIONCODE)
        setupChipClickListener(dialogView, R.id.filename_connector, "-")
        setupChipClickListener(dialogView, R.id.filename_underline, "_")
        setupChipClickListener(dialogView, R.id.filename_year, Constants.FONT_YEAR)
        setupChipClickListener(dialogView, R.id.filename_month, Constants.FONT_MONTH)
        setupChipClickListener(dialogView, R.id.filename_day_of_month, Constants.FONT_DAY_OF_MONTH)
        setupChipClickListener(dialogView, R.id.filename_hour_of_day, Constants.FONT_HOUR_OF_DAY)
        setupChipClickListener(dialogView, R.id.filename_minute, Constants.FONT_MINUTE)
        setupChipClickListener(dialogView, R.id.filename_second, Constants.FONT_SECOND)
        setupChipClickListener(dialogView, R.id.filename_sequence_number, Constants.FONT_AUTO_SEQUENCE_NUMBER)

        // 创建对话框，但不立即显示
        dialog = builder.create()

        // 在显示前设置窗口属性，避免闪烁问题
        dialog.window?.let { window ->
            // 设置对话框宽度
            val params = window.attributes
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = params

            // 创建圆角背景并立即应用
            val cornerSize = context.resources.getDimension(R.dimen.md3_card_corner_radius_large)
            val shapeAppearanceModel = ShapeAppearanceModel.Builder()
                .setAllCorners(CornerFamily.ROUNDED, cornerSize)
                .build()

            val shapeDrawable = MaterialShapeDrawable(shapeAppearanceModel)
            // 使用主题颜色属性替代硬编码白色
            shapeDrawable.fillColor = android.content.res.ColorStateList.valueOf(
                context.colorSurfaceContainer
            )

            window.setBackgroundDrawable(shapeDrawable)
        }
    }

    private fun setupChipClickListener(rootView: View, chipId: Int, insertText: String) {
        val chip = rootView.findViewById<Chip>(chipId)
        chip.setOnClickListener {
            if (edit_apk.hasFocus()) {
                val position = edit_apk.selectionStart
                edit_apk.text?.insert(position, insertText)
            }
            if (edit_zip.hasFocus()) {
                val position = edit_zip.selectionStart
                edit_zip.text?.insert(position, insertText)
            }
        }
    }

    fun show() {
        dialog.show()

        // 在对话框显示后设置按钮点击监听器
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (edit_apk.text.toString().trim().isEmpty() || edit_zip.text.toString().trim().isEmpty()) {
                ToastManager.showToast(context, context.resources.getString(R.string.dialog_filename_toast_blank), Toast.LENGTH_SHORT)
                return@setOnClickListener
            }

            val apk_replaced_variables = EnvironmentUtil.getEmptyVariableString(edit_apk.text.toString())
            val zip_replaced_variables = EnvironmentUtil.getEmptyVariableString(edit_zip.text.toString())
            if (!EnvironmentUtil.isALegalFileName(apk_replaced_variables) || !EnvironmentUtil.isALegalFileName(zip_replaced_variables)) {
                ToastManager.showToast(context, context.resources.getString(R.string.file_invalid_name), Toast.LENGTH_SHORT)
                return@setOnClickListener
            }

            val editor = settings.edit()
            editor.putString(Constants.PREFERENCE_FILENAME_FONT_APK, edit_apk.text.toString())
            editor.putString(Constants.PREFERENCE_FILENAME_FONT_ZIP, edit_zip.text.toString())

            val selectedZipLevel = spinner.text.toString()
            when (selectedZipLevel) {
                context.resources.getString(R.string.zip_level_default) -> {
                    editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT)
                }
                context.resources.getString(R.string.zip_level_stored) -> {
                    editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.ZIP_LEVEL_STORED)
                }
                context.resources.getString(R.string.zip_level_low) -> {
                    editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.ZIP_LEVEL_LOW)
                }
                context.resources.getString(R.string.zip_level_normal) -> {
                    editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.ZIP_LEVEL_NORMAL)
                }
                context.resources.getString(R.string.zip_level_high) -> {
                    editor.putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.ZIP_LEVEL_HIGH)
                }
            }

            editor.apply()
            dialog.dismiss()
        }
    }

    fun cancel() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    private fun getFormatedExportFileName(apk: String, zip: String): String {
        return "${context.resources.getString(R.string.word_preview)}\n\nAPK:  ${getReplacedString(apk)}.apk\n\n" +
                "${context.resources.getString(R.string.word_compressed)}:  ${getReplacedString(zip)}." +
                SPUtil.getCompressingExtensionName(context)
    }

    private fun getReplacedString(value: String): String {
        val PREVIEW_APPNAME = context.resources.getString(R.string.dialog_filename_preview_appname)
        val PREVIEW_PACKAGENAME = context.resources.getString(R.string.dialog_filename_preview_packagename)
        val PREVIEW_VERSION = context.resources.getString(R.string.dialog_filename_preview_version)
        val PREVIEW_VERSIONCODE = context.resources.getString(R.string.dialog_filename_preview_versioncode)
        
        var result = value
        result = result.replace(Constants.FONT_APP_NAME, PREVIEW_APPNAME)
        result = result.replace(Constants.FONT_APP_PACKAGE_NAME, PREVIEW_PACKAGENAME)
        result = result.replace(Constants.FONT_APP_VERSIONNAME, PREVIEW_VERSION)
        result = result.replace(Constants.FONT_APP_VERSIONCODE, PREVIEW_VERSIONCODE)
        result = result.replace(Constants.FONT_YEAR, EnvironmentUtil.getCurrentTimeValue(Calendar.YEAR))
        result = result.replace(Constants.FONT_MONTH, EnvironmentUtil.getCurrentTimeValue(Calendar.MONTH))
        result = result.replace(Constants.FONT_DAY_OF_MONTH, EnvironmentUtil.getCurrentTimeValue(Calendar.DAY_OF_MONTH))
        result = result.replace(Constants.FONT_HOUR_OF_DAY, EnvironmentUtil.getCurrentTimeValue(Calendar.HOUR_OF_DAY))
        result = result.replace(Constants.FONT_MINUTE, EnvironmentUtil.getCurrentTimeValue(Calendar.MINUTE))
        result = result.replace(Constants.FONT_SECOND, EnvironmentUtil.getCurrentTimeValue(Calendar.SECOND))
        result = result.replace(Constants.FONT_AUTO_SEQUENCE_NUMBER, "2")
        return result
    }
}

