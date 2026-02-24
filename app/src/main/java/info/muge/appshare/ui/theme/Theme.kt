package info.muge.appshare.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.materialkolor.rememberDynamicColorScheme

/**
 * 检查设备是否支持动态取色
 */
fun isDynamicColorAvailable(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

/**
 * AppShare 主题
 *
 * @param darkTheme 是否使用深色主题
 * @param dynamicColor 是否使用系统动态取色（Material You）
 * @param seedColor 自定义主题种子色（materialKolor 生成调色板）
 * @param isAmoled AMOLED 纯黑模式（仅深色主题时生效）
 * @param content 内容
 */
@Composable
fun AppShareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColor: Color = Color(0xFF4285F4),
    isAmoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // 系统动态取色 (Android 12+)
        dynamicColor && isDynamicColorAvailable() -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // 使用 materialKolor 从 seedColor 生成完整 M3 调色板
        else -> rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = darkTheme
        )
    }

    // AMOLED 纯黑模式：将 surface 系列颜色覆盖为纯黑/接近纯黑
    val finalScheme = if (isAmoled && darkTheme) {
        colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainer = Color(0xFF0A0A0A),
            surfaceContainerLow = Color(0xFF050505),
            surfaceContainerHigh = Color(0xFF111111),
            surfaceContainerHighest = Color(0xFF1A1A1A),
            surfaceContainerLowest = Color.Black,
            surfaceDim = Color.Black,
            surfaceBright = Color(0xFF1A1A1A)
        )
    } else {
        colorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
