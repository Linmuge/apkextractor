package info.muge.appshare

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import info.muge.appshare.ui.screens.AppDetailScreen
import info.muge.appshare.ui.screens.LaunchScreen
import info.muge.appshare.ui.screens.MainScreen
import info.muge.appshare.ui.dialogs.GlobalDialogHost
import info.muge.appshare.ui.theme.AppShareTheme
import info.muge.appshare.utils.SPUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 全局主题状态
object ThemeState {
    private val _darkModeFlow = MutableStateFlow(false)
    val darkModeFlow: StateFlow<Boolean> = _darkModeFlow.asStateFlow()

    fun updateDarkMode(isDark: Boolean) {
        _darkModeFlow.value = isDark
    }

    fun getDarkModeValue(context: android.content.Context): Boolean {
        val settings = SPUtil.getGlobalSharedPreferences(context)
        val nightMode = settings.getInt(Constants.PREFERENCE_NIGHT_MODE, Constants.PREFERENCE_NIGHT_MODE_DEFAULT)
        return when (nightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> {
                // 跟随系统
                val currentNightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun initDarkMode(context: android.content.Context) {
        _darkModeFlow.value = getDarkModeValue(context)
    }
}

/**
 * 主Activity - Compose版本
 */
class ComposeMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化主题状态
        ThemeState.initDarkMode(this)

        setContent {
            val darkMode by ThemeState.darkModeFlow.collectAsStateWithLifecycle()

            AppShareTheme(darkTheme = darkMode) {
                AppNavigation()
            }
        }
    }
}

/**
 * 应用导航
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // 获取Intent参数
    val packageName = (context as? ComponentActivity)?.intent?.getStringExtra("packageName")
    val apkUri = (context as? ComponentActivity)?.intent?.getStringExtra("apkUri")?.let { Uri.parse(it) }

    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = if (packageName != null || apkUri != null) "detail" else "launch"
        ) {
            composable("launch") {
                LaunchScreen(
                    onNavigateToMain = {
                        navController.navigate("main") {
                            popUpTo("launch") { inclusive = true }
                        }
                    }
                )
            }

            composable("main") {
                MainScreen(
                    onNavigateToAppDetail = { pkg ->
                        navController.navigate("detail/$pkg")
                    },
                    onNavigateToAppDetailWithUri = { uri ->
                        navController.navigate("detailUri/${Uri.encode(uri.toString())}")
                    }
                )
            }

            composable("detail/{packageName}") { backStackEntry ->
                val pkg = backStackEntry.arguments?.getString("packageName") ?: ""
                AppDetailScreen(
                    packageName = pkg,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("detail") {
                AppDetailScreen(
                    packageName = packageName,
                    apkUri = apkUri,
                    onBack = {
                        (context as? ComponentActivity)?.finish()
                    }
                )
            }

            composable("detailUri/{uri}") { backStackEntry ->
                val uriString = backStackEntry.arguments?.getString("uri") ?: ""
                val uri = Uri.parse(Uri.decode(uriString))
                AppDetailScreen(
                    apkUri = uri,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        GlobalDialogHost()
    }
}
