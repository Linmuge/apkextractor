package info.muge.appshare

import android.net.Uri
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.scene.Scene
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import info.muge.appshare.ui.screens.AppChangeScreen
import info.muge.appshare.ui.screens.AppDetailScreen
import info.muge.appshare.ui.screens.ThemeSettingsScreen
import info.muge.appshare.ui.screens.LaunchScreen
import info.muge.appshare.ui.screens.MainScreen
import info.muge.appshare.ui.dialogs.GlobalDialogHost
import info.muge.appshare.ui.theme.AppShareTheme
import info.muge.appshare.utils.SPUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey

@Serializable
data object LaunchRoute : AppRoute

@Serializable
data object MainRoute : AppRoute

@Serializable
data class DetailRoute(val packageName: String) : AppRoute

@Serializable
data class DetailUriRoute(val uri: String) : AppRoute

@Serializable
data class ExternalDetailRoute(
    val packageName: String? = null,
    val uri: String? = null
) : AppRoute

@Serializable
data object AppChangeRoute : AppRoute

@Serializable
data object ThemeSettingsRoute : AppRoute

private class AppNavigator(
    private val context: ComponentActivity?,
    private val backStack: MutableList<NavKey>
) {
    fun replaceRoot(route: AppRoute) {
        backStack.clear()
        backStack.add(route)
    }

    fun navigate(route: AppRoute, singleTop: Boolean = true) {
        if (singleTop && backStack.lastOrNull() == route) return
        backStack.add(route)
    }

    fun navigateToDetail(packageName: String) {
        navigate(DetailRoute(packageName))
    }

    fun navigateToUri(uri: Uri) {
        navigate(DetailUriRoute(uri.toString()))
    }

    fun popOrFinish() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        } else {
            context?.finish()
        }
    }
}

private fun Any?.isDetailSceneKey(): Boolean {
    val text = this?.toString() ?: return false
    return text.startsWith("detail/") ||
        text.startsWith("detail-uri/") ||
        text.startsWith("external-detail/")
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<Scene<NavKey>>.appPushTransitionSpec():
    ContentTransform {
    val from = initialState.key
    val to = targetState.key
    return when {
        from.isDetailSceneKey() && !to.isDetailSceneKey() -> {
            slideInHorizontally(
                initialOffsetX = { -it / 6 },
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 220, delayMillis = 40, easing = LinearOutSlowInEasing)
            ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { it / 5 },
                    animationSpec = tween(durationMillis = 260, easing = FastOutLinearInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
                ) + scaleOut(
                    targetScale = 0.98f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
                )
        }
        !from.isDetailSceneKey() && to.isDetailSceneKey() -> {
            slideInHorizontally(
                initialOffsetX = { it / 3 },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 220, delayMillis = 30, easing = LinearOutSlowInEasing)
            ) + scaleIn(
                initialScale = 0.99f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { -it / 8 },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
                ) + scaleOut(
                    targetScale = 0.985f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutLinearInEasing)
                )
        }
        else -> {
            fadeIn(
                animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
            ) togetherWith fadeOut(
                animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing)
            )
        }
    }
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<Scene<NavKey>>.appPopTransitionSpec():
    ContentTransform {
    val from = initialState.key
    val to = targetState.key
    return when {
        from.isDetailSceneKey() && !to.isDetailSceneKey() -> {
            // 返回主页时：上一页轻微推入，当前详情明显右滑退出，避免“抢镜”
            slideInHorizontally(
                initialOffsetX = { -it / 7 },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 210, delayMillis = 20, easing = LinearOutSlowInEasing)
            ) + scaleIn(
                initialScale = 0.995f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { it / 2 },
                    animationSpec = tween(durationMillis = 280, easing = FastOutLinearInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 170, easing = FastOutLinearInEasing)
                ) + scaleOut(
                    targetScale = 0.97f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutLinearInEasing)
                )
        }
        !from.isDetailSceneKey() && to.isDetailSceneKey() -> {
            slideInHorizontally(
                initialOffsetX = { it / 4 },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing)
            ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { -it / 4 },
                    animationSpec = tween(durationMillis = 260, easing = FastOutLinearInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 170, easing = FastOutLinearInEasing)
                )
        }
        else -> {
            fadeIn(
                animationSpec = tween(durationMillis = 190, easing = LinearOutSlowInEasing)
            ) togetherWith fadeOut(
                animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing)
            )
        }
    }
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<Scene<NavKey>>.appPredictivePopTransitionSpec(
    @NavigationEvent.SwipeEdge swipeEdge: Int
): ContentTransform {
    val from = initialState.key
    val to = targetState.key
    val isRightEdge = swipeEdge == NavigationEvent.EDGE_RIGHT

    // 预测性返回：根据滑动边缘调整方向，让手势跟页面运动方向一致
    val incomingOffset = if (isRightEdge) { -1 } else { 1 }
    val outgoingOffset = if (isRightEdge) { 1 } else { -1 }

    return when {
        from.isDetailSceneKey() && !to.isDetailSceneKey() -> {
            slideInHorizontally(
                initialOffsetX = { it / 8 * incomingOffset },
                animationSpec = tween(durationMillis = 260, easing = LinearOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)
            ) + scaleIn(
                initialScale = 0.996f,
                animationSpec = tween(durationMillis = 260, easing = LinearOutSlowInEasing)
            ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { it / 2 * outgoingOffset },
                    animationSpec = tween(durationMillis = 240, easing = FastOutLinearInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing)
                ) + scaleOut(
                    targetScale = 0.972f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutLinearInEasing)
                )
        }
        else -> {
            slideInHorizontally(
                initialOffsetX = { it / 10 * incomingOffset },
                animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing)
            ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { it / 6 * outgoingOffset },
                    animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 140, easing = FastOutLinearInEasing)
                )
        }
    }
}

// 全局主题状态
object ThemeState {
    private val _darkModeFlow = MutableStateFlow(false)
    val darkModeFlow: StateFlow<Boolean> = _darkModeFlow.asStateFlow()

    private val _dynamicColorFlow = MutableStateFlow(true)
    val dynamicColorFlow: StateFlow<Boolean> = _dynamicColorFlow.asStateFlow()

    private val _seedColorFlow = MutableStateFlow(androidx.compose.ui.graphics.Color(0xFF4285F4))
    val seedColorFlow: StateFlow<androidx.compose.ui.graphics.Color> = _seedColorFlow.asStateFlow()

    private val _isAmoledFlow = MutableStateFlow(false)
    val isAmoledFlow: StateFlow<Boolean> = _isAmoledFlow.asStateFlow()

    fun updateDarkMode(isDark: Boolean) {
        _darkModeFlow.value = isDark
    }

    fun updateDynamicColor(isDynamic: Boolean) {
        _dynamicColorFlow.value = isDynamic
    }

    fun updateSeedColor(color: androidx.compose.ui.graphics.Color) {
        _seedColorFlow.value = color
    }

    fun updateAmoled(isAmoled: Boolean) {
        _isAmoledFlow.value = isAmoled
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

    fun initDynamicColor(context: android.content.Context) {
        val settings = SPUtil.getGlobalSharedPreferences(context)
        _dynamicColorFlow.value = settings.getBoolean(Constants.PREFERENCE_DYNAMIC_COLOR, true)
    }

    fun initSeedColor(context: android.content.Context) {
        val settings = SPUtil.getGlobalSharedPreferences(context)
        val colorInt = settings.getInt(Constants.PREFERENCE_THEME_COLOR, Constants.PREFERENCE_THEME_COLOR_DEFAULT)
        _seedColorFlow.value = androidx.compose.ui.graphics.Color(colorInt)
    }

    fun initAmoled(context: android.content.Context) {
        val settings = SPUtil.getGlobalSharedPreferences(context)
        _isAmoledFlow.value = settings.getBoolean(Constants.PREFERENCE_AMOLED, false)
    }
}

/**
 * 主Activity - Compose版本
 */
class ComposeMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // 初始化主题状态
        ThemeState.initDarkMode(this)
        ThemeState.initDynamicColor(this)
        ThemeState.initSeedColor(this)
        ThemeState.initAmoled(this)

        setContent {
            val darkMode by ThemeState.darkModeFlow.collectAsStateWithLifecycle()
            val dynamicColor by ThemeState.dynamicColorFlow.collectAsStateWithLifecycle()
            val seedColor by ThemeState.seedColorFlow.collectAsStateWithLifecycle()
            val isAmoled by ThemeState.isAmoledFlow.collectAsStateWithLifecycle()

            AppShareTheme(
                darkTheme = darkMode,
                dynamicColor = dynamicColor,
                seedColor = seedColor,
                isAmoled = isAmoled
            ) {
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
    val context = LocalContext.current

    // 获取Intent参数
    val packageName = (context as? ComponentActivity)?.intent?.getStringExtra("packageName")
    val apkUriString = (context as? ComponentActivity)?.intent?.getStringExtra("apkUri")
    val startRoute = remember(packageName, apkUriString) {
        if (packageName != null || apkUriString != null) {
            ExternalDetailRoute(packageName = packageName, uri = apkUriString)
        } else {
            LaunchRoute
        }
    }
    val backStack = rememberNavBackStack(startRoute)
    val navigator = remember(context, backStack) {
        AppNavigator(context as? ComponentActivity, backStack)
    }

    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { navigator.popOrFinish() },
            transitionSpec = { appPushTransitionSpec() },
            popTransitionSpec = { appPopTransitionSpec() },
            predictivePopTransitionSpec = { swipeEdge ->
                appPredictivePopTransitionSpec(swipeEdge)
            },
            entryProvider = entryProvider {
                entry<LaunchRoute>(clazzContentKey = { "launch" }) {
                    LaunchScreen(
                        onNavigateToMain = {
                            navigator.replaceRoot(MainRoute)
                        }
                    )
                }

                entry<MainRoute>(clazzContentKey = { "main" }) {
                    MainScreen(
                        onNavigateToAppDetail = { pkg ->
                            navigator.navigateToDetail(pkg)
                        },
                        onNavigateToAppDetailWithUri = { uri ->
                            navigator.navigateToUri(uri)
                        },
                        onNavigateToAppChange = {
                            navigator.navigate(AppChangeRoute)
                        },
                        onNavigateToThemeSettings = {
                            navigator.navigate(ThemeSettingsRoute)
                        }
                    )
                }

                entry<DetailRoute>(clazzContentKey = { "detail/${it.packageName}" }) { route ->
                    AppDetailScreen(
                        packageName = route.packageName,
                        onBack = { navigator.popOrFinish() }
                    )
                }

                entry<DetailUriRoute>(clazzContentKey = { "detail-uri/${it.uri}" }) { route ->
                    val uri = Uri.parse(route.uri)
                    AppDetailScreen(
                        apkUri = uri,
                        onBack = { navigator.popOrFinish() }
                    )
                }

                entry<ExternalDetailRoute>(
                    clazzContentKey = {
                        "external-detail/${it.packageName.orEmpty()}/${it.uri.orEmpty()}"
                    }
                ) { route ->
                    AppDetailScreen(
                        packageName = route.packageName,
                        apkUri = route.uri?.let { Uri.parse(it) },
                        onBack = { navigator.popOrFinish() }
                    )
                }

                entry<AppChangeRoute>(clazzContentKey = { "app-change" }) {
                    AppChangeScreen(
                        onBack = { navigator.popOrFinish() }
                    )
                }

                entry<ThemeSettingsRoute>(clazzContentKey = { "theme-settings" }) {
                    ThemeSettingsScreen(
                        onBack = { navigator.popOrFinish() }
                    )
                }
            }
        )

        GlobalDialogHost()
    }
}
