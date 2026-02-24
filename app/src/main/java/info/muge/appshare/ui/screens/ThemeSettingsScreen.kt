package info.muge.appshare.ui.screens

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.muge.appshare.Constants
import info.muge.appshare.R
import info.muge.appshare.ThemeState
import info.muge.appshare.ui.dialogs.AppBottomSheet
import info.muge.appshare.ui.dialogs.AppBottomSheetDualActions
import info.muge.appshare.ui.dialogs.ThemeColorDialog
import info.muge.appshare.ui.theme.AppDimens
import info.muge.appshare.ui.theme.isDynamicColorAvailable
import info.muge.appshare.utils.SPUtil

/**
 * 主题设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings = SPUtil.getGlobalSharedPreferences(context)

    var showNightModeDialog by remember { mutableStateOf(false) }
    var showThemeColorDialog by remember { mutableStateOf(false) }

    var nightModeValue by remember {
        mutableIntStateOf(
            settings.getInt(Constants.PREFERENCE_NIGHT_MODE, Constants.PREFERENCE_NIGHT_MODE_DEFAULT)
        )
    }
    var isDynamicColor by remember {
        mutableStateOf(
            settings.getBoolean(Constants.PREFERENCE_DYNAMIC_COLOR, true)
        )
    }
    var seedColor by remember {
        mutableStateOf(
            androidx.compose.ui.graphics.Color(
                settings.getInt(Constants.PREFERENCE_THEME_COLOR, Constants.PREFERENCE_THEME_COLOR_DEFAULT)
            )
        )
    }
    var isAmoled by remember {
        mutableStateOf(
            settings.getBoolean(Constants.PREFERENCE_AMOLED, false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "主题设置",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = AppDimens.Space.lg)
                .padding(bottom = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.Space.sm))

            // 夜间模式
            SettingItem(
                iconRes = R.drawable.ic_dark_mode,
                title = stringResource(R.string.activity_settings_night_mode),
                value = getNightModeDisplayText(nightModeValue, context),
                isValueHighlighted = true,
                onClick = { showNightModeDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 动态取色 (Material You) - 仅 Android 12+ 显示
            if (isDynamicColorAvailable()) {
                SettingToggleItem(
                    iconRes = R.drawable.ic_dark_mode,
                    title = stringResource(R.string.setting_dynamic_color),
                    value = stringResource(R.string.setting_dynamic_color_desc),
                    checked = isDynamicColor,
                    onCheckedChange = {
                        isDynamicColor = it
                        settings.edit()
                            .putBoolean(Constants.PREFERENCE_DYNAMIC_COLOR, it)
                            .apply()
                        ThemeState.updateDynamicColor(it)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 主题色选择 (关闭动态取色时显示)
            if (!isDynamicColor) {
                SettingItem(
                    iconRes = R.drawable.ic_dark_mode,
                    title = "主题色",
                    value = "自定义应用主题颜色",
                    isValueHighlighted = true,
                    onClick = { showThemeColorDialog = true }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // AMOLED 纯黑模式
            SettingToggleItem(
                iconRes = R.drawable.ic_dark_mode,
                title = "AMOLED 纯黑",
                value = "深色模式下使用纯黑背景，更省电",
                checked = isAmoled,
                onCheckedChange = {
                    isAmoled = it
                    settings.edit()
                        .putBoolean(Constants.PREFERENCE_AMOLED, it)
                        .apply()
                    ThemeState.updateAmoled(it)
                }
            )
        }
    }

    // 夜间模式对话框
    if (showNightModeDialog) {
        ThemeNightModeDialog(
            currentValue = nightModeValue,
            onDismiss = { showNightModeDialog = false },
            onConfirm = { value ->
                nightModeValue = value
                settings.edit().putInt(Constants.PREFERENCE_NIGHT_MODE, value).apply()
                AppCompatDelegate.setDefaultNightMode(value)
                ThemeState.updateDarkMode(ThemeState.getDarkModeValue(context))
                showNightModeDialog = false
            }
        )
    }

    // 主题颜色选择对话框
    if (showThemeColorDialog) {
        ThemeColorDialog(
            currentColor = seedColor,
            onDismiss = { showThemeColorDialog = false },
            onConfirm = { color ->
                seedColor = color
                settings.edit()
                    .putInt(Constants.PREFERENCE_THEME_COLOR, color.toArgb())
                    .apply()
                ThemeState.updateSeedColor(color)
                showThemeColorDialog = false
            }
        )
    }
}

/**
 * 夜间模式对话框
 */
@Composable
private fun ThemeNightModeDialog(
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
                ThemeRadioButtonItem(
                    text = stringResource(R.string.night_mode_enabled),
                    selected = selected == AppCompatDelegate.MODE_NIGHT_YES,
                    onClick = { selected = AppCompatDelegate.MODE_NIGHT_YES }
                )
                ThemeRadioButtonItem(
                    text = stringResource(R.string.night_mode_disabled),
                    selected = selected == AppCompatDelegate.MODE_NIGHT_NO,
                    onClick = { selected = AppCompatDelegate.MODE_NIGHT_NO }
                )
                ThemeRadioButtonItem(
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
private fun ThemeRadioButtonItem(
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

private fun getNightModeDisplayText(value: Int, context: Context): String {
    return when (value) {
        AppCompatDelegate.MODE_NIGHT_YES -> context.getString(R.string.night_mode_enabled)
        AppCompatDelegate.MODE_NIGHT_NO -> context.getString(R.string.night_mode_disabled)
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> context.getString(R.string.night_mode_follow_system)
        else -> ""
    }
}
