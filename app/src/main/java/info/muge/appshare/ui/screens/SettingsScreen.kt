package info.muge.appshare.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.muge.appshare.Constants
import info.muge.appshare.R
import info.muge.appshare.ThemeState
import info.muge.appshare.ui.dialogs.ThemeColorDialog
import info.muge.appshare.ui.theme.isDynamicColorAvailable
import info.muge.appshare.ui.dialogs.AppBottomSheet
import info.muge.appshare.ui.dialogs.AppBottomSheetDualActions
import info.muge.appshare.ui.dialogs.ExportRuleDialog
import info.muge.appshare.ui.theme.AppDimens
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.toast

/**
 * 设置页面 - 完全匹配原 SettingsFragment 布局
 */
@Composable
fun SettingsScreen(
    onNavigateToAppChange: () -> Unit = {},
    onNavigateToThemeSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val settings = SPUtil.getGlobalSharedPreferences(context)

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLoadingOptionsDialog by remember { mutableStateOf(false) }
    var showPackageSeparatorDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showExportRuleDialog by remember { mutableStateOf(false) }
    var showStoragePathDialog by remember { mutableStateOf(false) }
    var showExtensionDialog by remember { mutableStateOf(false) }
    var showDeviceNameDialog by remember { mutableStateOf(false) }

    // 设置值
    var languageValue by remember { mutableIntStateOf(settings.getInt(Constants.PREFERENCE_LANGUAGE, Constants.PREFERENCE_LANGUAGE_DEFAULT)) }
    var showSystemApp by remember {
        mutableStateOf(
            settings.getBoolean(
                Constants.PREFERENCE_SHOW_SYSTEM_APP,
                Constants.PREFERENCE_SHOW_SYSTEM_APP_DEFAULT
            )
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.Space.lg)
            .padding(top = AppDimens.Space.sm, bottom = 100.dp)
    ) {
        // 分组标题：通用设置
        SectionTitle(title = "通用设置")

        Spacer(modifier = Modifier.height(AppDimens.Space.md))

        // 导出路径设置
        SettingItem(
            iconRes = R.drawable.ic_folder,
            title = stringResource(R.string.activity_settings_path),
            value = SPUtil.getDisplayingExportPath(context),
            onClick = {}
        )

        Spacer(modifier = Modifier.height(AppDimens.Space.sm))

        SettingToggleItem(
            iconRes = R.drawable.ic_settings,
            title = stringResource(R.string.main_card_show_system_app),
            value = "开启后显示系统应用",
            checked = showSystemApp,
            onCheckedChange = {
                showSystemApp = it
                settings.edit()
                    .putBoolean(Constants.PREFERENCE_SHOW_SYSTEM_APP, it)
                    .apply()
            }
        )

        Spacer(modifier = Modifier.height(AppDimens.Space.sm))

        // 规则设置
        SettingItem(
            iconRes = R.drawable.ic_rule,
            title = stringResource(R.string.activity_settings_rules),
            value = "配置导出规则",
            onClick = { showExportRuleDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 压缩包扩展名
        SettingItem(
            iconRes = R.drawable.ic_compressed,
            title = stringResource(R.string.activity_settings_extension),
            value = ".${SPUtil.getCompressingExtensionName(context)}",
            isValueHighlighted = true,
            onClick = { showExtensionDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 主题设置
        SettingItem(
            iconRes = R.drawable.ic_dark_mode,
            title = "主题设置",
            value = "夜间模式、主题色、AMOLED 纯黑",
            isValueHighlighted = true,
            onClick = onNavigateToThemeSettings
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 加载选项设置
        SettingItem(
            iconRes = R.drawable.ic_settings,
            title = stringResource(R.string.activity_settings_loading_options),
            value = getLoadingOptionsText(settings, context),
            isValueHighlighted = true,
            onClick = { showLoadingOptionsDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 设备名称
        SettingItem(
            iconRes = R.drawable.ic_device,
            title = stringResource(R.string.activity_settings_device_name),
            value = SPUtil.getDeviceName(context),
            isValueHighlighted = true,
            onClick = { showDeviceNameDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 包名分隔符设置
        SettingItem(
            iconRes = R.drawable.ic_text,
            title = stringResource(R.string.activity_settings_package_name_separator),
            value = settings.getString(Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR, Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR_DEFAULT) ?: "",
            isValueHighlighted = true,
            onClick = { showPackageSeparatorDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 语言设置
        SettingItem(
            iconRes = R.drawable.ic_language,
            title = stringResource(R.string.activity_settings_language),
            value = getLanguageDisplayText(languageValue, context),
            isValueHighlighted = true,
            onClick = { showLanguageDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SectionTitle(title = "数据")

        Spacer(modifier = Modifier.height(12.dp))

        // 应用变更记录
        SettingItem(
            iconRes = R.drawable.ic_info,
            title = "应用变更记录",
            value = "查看安装、更新、卸载记录",
            onClick = onNavigateToAppChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 分组标题：关于
        SectionTitle(title = "关于")

        Spacer(modifier = Modifier.height(12.dp))

        // 意见反馈
        SettingItem(
            iconRes = R.drawable.ic_feedback,
            title = stringResource(R.string.activity_settings_feedback),
            value = "可以将反馈内容发送到邮箱",
            onClick = { showFeedbackDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 关于
        SettingItem(
            iconRes = R.drawable.ic_info,
            title = stringResource(R.string.activity_settings_about),
            value = "应用信息与版本",
            onClick = { showAboutDialog = true }
        )
    }

    // 语言对话框
    if (showLanguageDialog) {
        LanguageDialog(
            currentValue = languageValue,
            onDismiss = { showLanguageDialog = false },
            onConfirm = { value ->
                languageValue = value
                settings.edit().putInt(Constants.PREFERENCE_LANGUAGE, value).apply()
                // 应用语言设置
                EnvironmentUtil.setLanguage(context, value)
                showLanguageDialog = false
            }
        )
    }

    // 加载选项对话框
    if (showLoadingOptionsDialog) {
        LoadingOptionsDialog(
            onDismiss = { showLoadingOptionsDialog = false }
        )
    }

    // 包名分隔符对话框
    if (showPackageSeparatorDialog) {
        PackageSeparatorDialog(
            currentValue = settings.getString(Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR, Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR_DEFAULT) ?: "",
            onDismiss = { showPackageSeparatorDialog = false },
            onConfirm = { value ->
                settings.edit().putString(Constants.PREFERENCE_COPYING_PACKAGE_NAME_SEPARATOR, value).apply()
                showPackageSeparatorDialog = false
            }
        )
    }

    // 关于对话框
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    // 反馈对话框
    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = { showFeedbackDialog = false }
        )
    }

    // 导出规则对话框
    if (showExportRuleDialog) {
        ExportRuleDialog(
            onDismiss = { showExportRuleDialog = false }
        )
    }

    // 压缩包扩展名对话框
    if (showExtensionDialog) {
        ExtensionDialog(
            currentValue = SPUtil.getCompressingExtensionName(context),
            onDismiss = { showExtensionDialog = false },
            onConfirm = { value ->
                settings.edit().putString(Constants.PREFERENCE_COMPRESSING_EXTENSION, value).apply()
                showExtensionDialog = false
            }
        )
    }

    // 设备名称对话框
    if (showDeviceNameDialog) {
        DeviceNameDialog(
            currentValue = SPUtil.getDeviceName(context),
            onDismiss = { showDeviceNameDialog = false },
            onConfirm = { value ->
                settings.edit().putString(Constants.PREFERENCE_DEVICE_NAME, value).apply()
                showDeviceNameDialog = false
            }
        )
    }
}

/**
 * 分组标题
 */
@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = AppDimens.Space.xs)
    )
}

/**
 * 设置项
 */
@Composable
fun SettingItem(
    iconRes: Int,
    title: String,
    value: String,
    isValueHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Radius.lg))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(AppDimens.Radius.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.none)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Space.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(AppDimens.Space.md))

            // 文本区域
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                if (value.isNotEmpty()) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isValueHighlighted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // 箭头
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingToggleItem(
    iconRes: Int,
    title: String,
    value: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Radius.lg)),
        shape = RoundedCornerShape(AppDimens.Radius.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.none)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Space.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(AppDimens.Space.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun NightModeDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(currentValue) }

    AppBottomSheet(
        title = stringResource(R.string.activity_settings_night_mode),
        onDismiss = onDismiss,
        content = {
            Column {
                RadioButtonItem(
                    text = stringResource(R.string.night_mode_enabled),
                    selected = selected == AppCompatDelegate.MODE_NIGHT_YES,
                    onClick = { selected = AppCompatDelegate.MODE_NIGHT_YES }
                )
                RadioButtonItem(
                    text = stringResource(R.string.night_mode_disabled),
                    selected = selected == AppCompatDelegate.MODE_NIGHT_NO,
                    onClick = { selected = AppCompatDelegate.MODE_NIGHT_NO }
                )
                RadioButtonItem(
                    text = stringResource(R.string.night_mode_follow_system),
                    selected = selected == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                    onClick = { selected = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }
                )
            }
        },
        actions = {
            AppBottomSheetDualActions(
                onConfirm = { onConfirm(selected) },
                onDismiss = onDismiss,
                confirmText = stringResource(android.R.string.ok),
                dismissText = stringResource(android.R.string.cancel)
            )
        }
    )
}

@Composable
private fun LanguageDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(currentValue) }

    AppBottomSheet(
        title = stringResource(R.string.activity_settings_language),
        onDismiss = onDismiss,
        content = {
            Column {
                RadioButtonItem(
                    text = stringResource(R.string.language_follow_system),
                    selected = selected == Constants.LANGUAGE_FOLLOW_SYSTEM,
                    onClick = { selected = Constants.LANGUAGE_FOLLOW_SYSTEM }
                )
                RadioButtonItem(
                    text = stringResource(R.string.language_chinese),
                    selected = selected == Constants.LANGUAGE_CHINESE,
                    onClick = { selected = Constants.LANGUAGE_CHINESE }
                )
                RadioButtonItem(
                    text = stringResource(R.string.language_english),
                    selected = selected == Constants.LANGUAGE_ENGLISH,
                    onClick = { selected = Constants.LANGUAGE_ENGLISH }
                )
            }
        },
        actions = {
            AppBottomSheetDualActions(
                onConfirm = { onConfirm(selected) },
                onDismiss = onDismiss,
                confirmText = stringResource(android.R.string.ok),
                dismissText = stringResource(android.R.string.cancel)
            )
        }
    )
}

@Composable
private fun LoadingOptionsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings = SPUtil.getGlobalSharedPreferences(context)

    var loadPermissions by remember { mutableStateOf(settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS, Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT)) }
    var loadActivities by remember { mutableStateOf(settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES, Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT)) }
    var loadServices by remember { mutableStateOf(settings.getBoolean(Constants.PREFERENCE_LOAD_SERVICES, Constants.PREFERENCE_LOAD_SERVICES_DEFAULT)) }
    var loadReceivers by remember { mutableStateOf(settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS, Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT)) }
    var loadProviders by remember { mutableStateOf(settings.getBoolean(Constants.PREFERENCE_LOAD_PROVIDERS, Constants.PREFERENCE_LOAD_PROVIDERS_DEFAULT)) }
    var loadStaticLoaders by remember { mutableStateOf(settings.getBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS, Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT)) }
    var loadSignature by remember { mutableStateOf(settings.getBoolean(Constants.PREFERENCE_LOAD_APK_SIGNATURE, Constants.PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT)) }
    var loadHash by remember { mutableStateOf(settings.getBoolean(Constants.PREFERENCE_LOAD_FILE_HASH, Constants.PREFERENCE_LOAD_FILE_HASH_DEFAULT)) }

    AppBottomSheet(
        title = stringResource(R.string.activity_settings_loading_options),
        onDismiss = onDismiss,
        content = {
            Column {
                CheckboxItem(
                    text = stringResource(R.string.activity_detail_permissions),
                    checked = loadPermissions,
                    onCheckedChange = { loadPermissions = it }
                )
                CheckboxItem(
                    text = stringResource(R.string.activity_detail_activities),
                    checked = loadActivities,
                    onCheckedChange = { loadActivities = it }
                )
                CheckboxItem(
                    text = stringResource(R.string.activity_detail_services),
                    checked = loadServices,
                    onCheckedChange = { loadServices = it }
                )
                CheckboxItem(
                    text = stringResource(R.string.activity_detail_receivers),
                    checked = loadReceivers,
                    onCheckedChange = { loadReceivers = it }
                )
                CheckboxItem(
                    text = stringResource(R.string.activity_detail_providers),
                    checked = loadProviders,
                    onCheckedChange = { loadProviders = it }
                )
                CheckboxItem(
                    text = stringResource(R.string.activity_detail_static_loaders),
                    checked = loadStaticLoaders,
                    onCheckedChange = { loadStaticLoaders = it }
                )
                CheckboxItem(
                    text = stringResource(R.string.dialog_loading_selection_signature),
                    checked = loadSignature,
                    onCheckedChange = { loadSignature = it }
                )
                CheckboxItem(
                    text = stringResource(R.string.dialog_loading_selection_file_hash),
                    checked = loadHash,
                    onCheckedChange = { loadHash = it }
                )
            }
        },
        actions = {
            AppBottomSheetDualActions(
                onConfirm = {
                settings.edit()
                    .putBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS, loadPermissions)
                    .putBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES, loadActivities)
                    .putBoolean(Constants.PREFERENCE_LOAD_SERVICES, loadServices)
                    .putBoolean(Constants.PREFERENCE_LOAD_RECEIVERS, loadReceivers)
                    .putBoolean(Constants.PREFERENCE_LOAD_PROVIDERS, loadProviders)
                    .putBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS, loadStaticLoaders)
                    .putBoolean(Constants.PREFERENCE_LOAD_APK_SIGNATURE, loadSignature)
                    .putBoolean(Constants.PREFERENCE_LOAD_FILE_HASH, loadHash)
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

@Composable
private fun PackageSeparatorDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(currentValue) }

    AppBottomSheet(
        title = stringResource(R.string.activity_settings_package_name_separator),
        onDismiss = onDismiss,
        content = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth()
            )
        },
        actions = {
            AppBottomSheetDualActions(
                onConfirm = { onConfirm(value) },
                onDismiss = onDismiss,
                confirmText = stringResource(R.string.action_confirm),
                dismissText = stringResource(R.string.action_cancel)
            )
        }
    )
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AppBottomSheet(
        title = "${EnvironmentUtil.getAppName(context)}(${EnvironmentUtil.getAppVersionName(context)})",
        onDismiss = onDismiss,
        content = {
            Column {
                Text(
                    text = "AppShare/AppKit 是一款强大的 APK 提取和管理工具。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "功能特点：",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• APK 导出与分享\n• 应用信息查看\n• 签名分析\n• 统计功能",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        actions = {
            AppBottomSheetDualActions(
                onConfirm = onDismiss,
                onDismiss = onDismiss,
                confirmText = stringResource(R.string.dialog_button_confirm),
                dismissText = stringResource(R.string.dialog_button_cancel)
            )
        }
    )
}

@Composable
private fun FeedbackDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val feedbackEmail = "1206083231@qq.com"

    AppBottomSheet(
        title = "意见反馈",
        onDismiss = onDismiss,
        content = {
            Text(
                text = "如您在使用过程中遇到任何问题或有任何建议，欢迎发送邮件至以下邮箱进行反馈：\n\n$feedbackEmail\n\n我们将在15个工作日内回复您的反馈。",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        actions = {
            AppBottomSheetDualActions(
                onConfirm = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("feedback_email", feedbackEmail)
                clipboard.setPrimaryClip(clip)
                "邮箱已复制到剪贴板".toast()
                onDismiss()
                },
                onDismiss = onDismiss,
                confirmText = "复制邮箱",
                dismissText = stringResource(R.string.dialog_button_cancel)
            )
        }
    )
}

@Composable
private fun RadioButtonItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun CheckboxItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private fun getNightModeDisplayText(value: Int, context: Context): String {
    return when (value) {
        AppCompatDelegate.MODE_NIGHT_YES -> context.getString(R.string.night_mode_enabled)
        AppCompatDelegate.MODE_NIGHT_NO -> context.getString(R.string.night_mode_disabled)
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> context.getString(R.string.night_mode_follow_system)
        else -> ""
    }
}

private fun getLanguageDisplayText(value: Int, context: Context): String {
    return when (value) {
        Constants.LANGUAGE_FOLLOW_SYSTEM -> context.getString(R.string.language_follow_system)
        Constants.LANGUAGE_CHINESE -> context.getString(R.string.language_chinese)
        Constants.LANGUAGE_ENGLISH -> context.getString(R.string.language_english)
        else -> ""
    }
}

private fun getLoadingOptionsText(settings: android.content.SharedPreferences, context: Context): String {
    return buildString {
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_PERMISSIONS, Constants.PREFERENCE_LOAD_PERMISSIONS_DEFAULT)) {
            append(context.getString(R.string.activity_detail_permissions))
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_ACTIVITIES, Constants.PREFERENCE_LOAD_ACTIVITIES_DEFAULT)) {
            if (isNotEmpty()) append(",")
            append(context.getString(R.string.activity_detail_activities))
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_SERVICES, Constants.PREFERENCE_LOAD_SERVICES_DEFAULT)) {
            if (isNotEmpty()) append(",")
            append(context.getString(R.string.activity_detail_services))
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_RECEIVERS, Constants.PREFERENCE_LOAD_RECEIVERS_DEFAULT)) {
            if (isNotEmpty()) append(",")
            append(context.getString(R.string.activity_detail_receivers))
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_PROVIDERS, Constants.PREFERENCE_LOAD_PROVIDERS_DEFAULT)) {
            if (isNotEmpty()) append(",")
            append(context.getString(R.string.activity_detail_providers))
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS, Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT)) {
            if (isNotEmpty()) append(",")
            append(context.getString(R.string.activity_detail_static_loaders))
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_APK_SIGNATURE, Constants.PREFERENCE_LOAD_APK_SIGNATURE_DEFAULT)) {
            if (isNotEmpty()) append(",")
            append(context.getString(R.string.dialog_loading_selection_signature))
        }
        if (settings.getBoolean(Constants.PREFERENCE_LOAD_FILE_HASH, Constants.PREFERENCE_LOAD_FILE_HASH_DEFAULT)) {
            if (isNotEmpty()) append(",")
            append(context.getString(R.string.dialog_loading_selection_file_hash))
        }
    }.ifEmpty { context.getString(R.string.word_blank) }
}

@Composable
private fun ExtensionDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selected by remember { mutableStateOf(currentValue) }
    val extensions = listOf("zip", "apks", "xapk")

    AppBottomSheet(
        title = stringResource(R.string.activity_settings_extension),
        onDismiss = onDismiss,
        content = {
            Column {
                extensions.forEach { ext ->
                    RadioButtonItem(
                        text = ".$ext",
                        selected = selected == ext,
                        onClick = { selected = ext }
                    )
                }
            }
        },
        actions = {
            AppBottomSheetDualActions(
                onConfirm = { onConfirm(selected) },
                onDismiss = onDismiss,
                confirmText = stringResource(R.string.dialog_button_confirm),
                dismissText = stringResource(R.string.dialog_button_cancel)
            )
        }
    )
}

@Composable
private fun DeviceNameDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(currentValue) }

    AppBottomSheet(
        title = stringResource(R.string.activity_settings_device_name),
        onDismiss = onDismiss,
        content = {
            Column {
                Text(
                    text = "设置在网络传输时显示的设备名称",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        actions = {
            AppBottomSheetDualActions(
                onConfirm = {
                if (value.trim().isNotEmpty()) {
                    onConfirm(value.trim())
                } else {
                    "设备名称不能为空".toast()
                }
                },
                onDismiss = onDismiss,
                confirmText = stringResource(R.string.dialog_button_confirm),
                dismissText = stringResource(R.string.dialog_button_cancel)
            )
        }
    )
}
