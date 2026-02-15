package info.muge.appshare.ui.dialogs

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.muge.appshare.Constants
import info.muge.appshare.R
import info.muge.appshare.ui.ToastManager
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.SPUtil
import java.util.Calendar

/**
 * 导出规则对话框 - 与原 ExportRuleDialog 完全一致
 */
@Composable
fun ExportRuleDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings = SPUtil.getGlobalSharedPreferences(context)

    var apkFileName by remember {
        mutableStateOf(
            settings.getString(Constants.PREFERENCE_FILENAME_FONT_APK, Constants.PREFERENCE_FILENAME_FONT_DEFAULT) ?: ""
        )
    }
    var zipFileName by remember {
        mutableStateOf(
            settings.getString(Constants.PREFERENCE_FILENAME_FONT_ZIP, Constants.PREFERENCE_FILENAME_FONT_DEFAULT) ?: ""
        )
    }
    var selectedZipLevel by remember {
        mutableIntStateOf(
            settings.getInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT)
        )
    }
    var focusedField by remember { mutableStateOf("apk") }

    val zipLevels = listOf(
        context.getString(R.string.zip_level_default) to Constants.PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT,
        context.getString(R.string.zip_level_stored) to Constants.ZIP_LEVEL_STORED,
        context.getString(R.string.zip_level_low) to Constants.ZIP_LEVEL_LOW,
        context.getString(R.string.zip_level_normal) to Constants.ZIP_LEVEL_NORMAL,
        context.getString(R.string.zip_level_high) to Constants.ZIP_LEVEL_HIGH
    )

    // 预览文本
    val previewText = getFormatedExportFileName(context, apkFileName, zipFileName)

    // 检查警告
    val apkWarn = !apkFileName.contains(Constants.FONT_APP_NAME) &&
            !apkFileName.contains(Constants.FONT_APP_PACKAGE_NAME) &&
            !apkFileName.contains(Constants.FONT_AUTO_SEQUENCE_NUMBER)

    val zipWarn = !zipFileName.contains(Constants.FONT_APP_NAME) &&
            !zipFileName.contains(Constants.FONT_APP_PACKAGE_NAME) &&
            !zipFileName.contains(Constants.FONT_AUTO_SEQUENCE_NUMBER)

    // 可插入的变量
    val variables = listOf(
        Pair(Constants.FONT_APP_NAME, stringResource(R.string.dialog_filename_button_appname)),
        Pair(Constants.FONT_APP_PACKAGE_NAME, stringResource(R.string.dialog_filename_button_packagename)),
        Pair(Constants.FONT_APP_VERSIONNAME, stringResource(R.string.dialog_filename_button_version)),
        Pair(Constants.FONT_APP_VERSIONCODE, stringResource(R.string.dialog_filename_button_versioncode)),
        Pair("-", "-"),
        Pair("_", "_"),
        Pair(Constants.FONT_YEAR, stringResource(R.string.dialog_filename_button_year)),
        Pair(Constants.FONT_MONTH, stringResource(R.string.dialog_filename_button_month)),
        Pair(Constants.FONT_DAY_OF_MONTH, stringResource(R.string.dialog_filename_button_day_of_month)),
        Pair(Constants.FONT_HOUR_OF_DAY, stringResource(R.string.dialog_filename_button_hour_of_day)),
        Pair(Constants.FONT_MINUTE, stringResource(R.string.dialog_filename_button_minute)),
        Pair(Constants.FONT_SECOND, stringResource(R.string.dialog_filename_button_second)),
        Pair(Constants.FONT_AUTO_SEQUENCE_NUMBER, stringResource(R.string.dialog_filename_button_sequence_number))
    )

    AppBottomSheet(
        title = stringResource(R.string.dialog_filename_title),
        onDismiss = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // APK 文件名输入框
                OutlinedTextField(
                    value = apkFileName,
                    onValueChange = { apkFileName = it },
                    label = { Text(stringResource(R.string.dialog_filename_apk_att)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { focusedField = "apk" },
                    singleLine = true
                )

                if (apkWarn) {
                    Text(
                        text = stringResource(R.string.dialog_filename_warn_no_variables),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ZIP 文件名输入框
                val extensionName = SPUtil.getCompressingExtensionName(context)
                OutlinedTextField(
                    value = zipFileName,
                    onValueChange = { zipFileName = it },
                    label = { Text(stringResource(R.string.dialog_filename_zip_att)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { focusedField = "zip" },
                    singleLine = true,
                    suffix = { Text(".$extensionName") }
                )

                if (zipWarn) {
                    Text(
                        text = stringResource(R.string.dialog_filename_warn_no_variables),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 变量选择
                Text(
                    text = stringResource(R.string.dialog_filename_variables),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    variables.forEach { (value, label) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                if (focusedField == "apk") {
                                    apkFileName += value
                                } else {
                                    zipFileName += value
                                }
                            },
                            label = { Text(label, fontSize = MaterialTheme.typography.labelSmall.fontSize) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ZIP 压缩级别
                Text(
                    text = "压缩级别:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    zipLevels.forEach { (label, level) ->
                        FilterChip(
                            selected = selectedZipLevel == level,
                            onClick = { selectedZipLevel = level },
                            label = { Text(label, fontSize = MaterialTheme.typography.labelSmall.fontSize) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 预览
                Text(
                    text = stringResource(R.string.word_preview),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            AppBottomSheetDualActions(
                onConfirm = {
                if (apkFileName.trim().isEmpty() || zipFileName.trim().isEmpty()) {
                    ToastManager.showToast(context, context.getString(R.string.dialog_filename_toast_blank), Toast.LENGTH_SHORT)
                    return@AppBottomSheetDualActions
                }

                val apkReplaced = EnvironmentUtil.getEmptyVariableString(apkFileName)
                val zipReplaced = EnvironmentUtil.getEmptyVariableString(zipFileName)

                if (!EnvironmentUtil.isALegalFileName(apkReplaced) || !EnvironmentUtil.isALegalFileName(zipReplaced)) {
                    ToastManager.showToast(context, context.getString(R.string.file_invalid_name), Toast.LENGTH_SHORT)
                    return@AppBottomSheetDualActions
                }

                settings.edit()
                    .putString(Constants.PREFERENCE_FILENAME_FONT_APK, apkFileName)
                    .putString(Constants.PREFERENCE_FILENAME_FONT_ZIP, zipFileName)
                    .putInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, selectedZipLevel)
                    .apply()

                onDismiss()
                },
                onDismiss = onDismiss,
                confirmText = stringResource(R.string.dialog_button_confirm),
                dismissText = stringResource(R.string.dialog_button_cancel)
            )
        }
    )
}

private fun getFormatedExportFileName(context: Context, apk: String, zip: String): String {
    val extensionName = SPUtil.getCompressingExtensionName(context)
    return "${context.getString(R.string.word_preview)}\n\nAPK:  ${getReplacedString(context, apk)}.apk\n\n" +
            "${context.getString(R.string.word_compressed)}:  ${getReplacedString(context, zip)}.$extensionName"
}

private fun getReplacedString(context: Context, value: String): String {
    val previewAppName = context.getString(R.string.dialog_filename_preview_appname)
    val previewPackageName = context.getString(R.string.dialog_filename_preview_packagename)
    val previewVersion = context.getString(R.string.dialog_filename_preview_version)
    val previewVersionCode = context.getString(R.string.dialog_filename_preview_versioncode)

    var result = value
    result = result.replace(Constants.FONT_APP_NAME, previewAppName)
    result = result.replace(Constants.FONT_APP_PACKAGE_NAME, previewPackageName)
    result = result.replace(Constants.FONT_APP_VERSIONNAME, previewVersion)
    result = result.replace(Constants.FONT_APP_VERSIONCODE, previewVersionCode)
    result = result.replace(Constants.FONT_YEAR, EnvironmentUtil.getCurrentTimeValue(Calendar.YEAR))
    result = result.replace(Constants.FONT_MONTH, EnvironmentUtil.getCurrentTimeValue(Calendar.MONTH))
    result = result.replace(Constants.FONT_DAY_OF_MONTH, EnvironmentUtil.getCurrentTimeValue(Calendar.DAY_OF_MONTH))
    result = result.replace(Constants.FONT_HOUR_OF_DAY, EnvironmentUtil.getCurrentTimeValue(Calendar.HOUR_OF_DAY))
    result = result.replace(Constants.FONT_MINUTE, EnvironmentUtil.getCurrentTimeValue(Calendar.MINUTE))
    result = result.replace(Constants.FONT_SECOND, EnvironmentUtil.getCurrentTimeValue(Calendar.SECOND))
    result = result.replace(Constants.FONT_AUTO_SEQUENCE_NUMBER, "2")
    return result
}
