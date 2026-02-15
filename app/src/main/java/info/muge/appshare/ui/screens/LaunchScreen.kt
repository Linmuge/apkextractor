package info.muge.appshare.ui.screens

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import info.muge.appshare.R
import info.muge.appshare.ui.dialogs.PrivacyDialog
import info.muge.appshare.utils.SPUtil

/**
 * 启动页
 *
 * @param onNavigateToMain 导航到主页的回调
 */
@Composable
fun LaunchScreen(
    onNavigateToMain: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // 设置状态栏样式 - 与原实现保持一致
    SideEffect {
        val window = (context as? Activity)?.window
        window?.let {
            it.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            it.statusBarColor = Color.TRANSPARENT
            it.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            it.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    // 检查是否首次启动
    LaunchedEffect(Unit) {
        val isFirstLaunch = SPUtil.getGlobalSharedPreferences(context).getBoolean("start", true)

        if (isFirstLaunch) {
            showPrivacyDialog = true
        } else {
            onNavigateToMain()
        }
    }

    // 启动页UI - 与原布局保持一致
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .alpha(if (showPrivacyDialog) 0f else 1f), // 对话框显示时隐藏内容
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图标位置 - 垂直偏移35%，与原布局 constraintVerticalBias="0.35" 一致
            Spacer(modifier = Modifier.fillMaxSize(0.35f))

            // 应用图标
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground_launch),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 应用名称
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // 隐私政策对话框
    if (showPrivacyDialog) {
        PrivacyDialog(
            onAgree = {
                SPUtil.getGlobalSharedPreferences(context)
                    .edit()
                    .putBoolean("start", false)
                    .apply()
                showPrivacyDialog = false
                onNavigateToMain()
            },
            onDisagree = {
                (context as? Activity)?.finish()
            }
        )
    }
}
