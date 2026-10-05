package com.shiguangji.app

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shiguangji.app.appGraph
import com.shiguangji.app.ui.LocalSg
import com.shiguangji.app.ui.SgTheme
import com.shiguangji.app.ui.cats.CategoriesScreen
import com.shiguangji.app.ui.detail.DetailScreen
import com.shiguangji.app.ui.editor.EditorScreen
import com.shiguangji.app.ui.home.HomeScreen
import com.shiguangji.app.ui.memory.MemoryScreen
import com.shiguangji.app.ui.settings.SettingsScreen
import com.shiguangji.app.ui.theme.ThemeScreen
import com.shiguangji.app.ui.viewer.ImageViewerScreen
import com.shiguangji.app.ui.whisper.PinChangeScreen
import com.shiguangji.app.ui.whisper.WhisperScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : FragmentActivity() {
    /** 小组件深链路由：widget 点击后经 intent 带入，由 SgApp 消费并导航 */
    private val pendingRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val graph = LocalContext.current.appGraph
            val pack by graph.themeManager.currentPack.collectAsState()
            val dynamic by graph.settings.dynamicColor.collectAsState(initial = false)
            val route by pendingRoute.collectAsState()
            SgTheme(pack = pack, dynamicColor = dynamic) {
                SgApp(pendingRoute = route, onPendingRouteConsumed = { pendingRoute.value = null })
            }
        }
        consumeWidgetRoute(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeWidgetRoute(intent)
    }

    private fun consumeWidgetRoute(intent: Intent?) {
        intent?.getStringExtra(EXTRA_WIDGET_ROUTE)?.let { pendingRoute.value = it }
    }

    companion object {
        const val EXTRA_WIDGET_ROUTE = "widget_route"
    }
}

object Routes {
    const val HOME = "home"
    const val CATS = "cats"
    const val WHISPER = "whisper"
    const val SETTINGS = "settings"
    const val THEME = "theme"
    const val MEMORY = "memory"
    const val PIN_CHANGE = "pinchange"
    const val EDITOR = "editor?entryId={entryId}"
    const val DETAIL = "detail/{entryId}"
    const val VIEWER = "viewer/{entryId}/{index}"
}

@Composable
fun SgApp(pendingRoute: String? = null, onPendingRouteConsumed: () -> Unit = {}) {
    val navController = rememberNavController()
    // 小组件/深链路由：外部请求打开某个页面（如桌面「写记录」直达编辑器）
    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            when (pendingRoute) {
                "editor" -> navController.navigate(Routes.EDITOR)
            }
            onPendingRouteConsumed()
        }
    }
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: Routes.HOME
    val showBar = route in setOf(
        Routes.HOME, Routes.CATS, Routes.WHISPER, Routes.SETTINGS, Routes.THEME, Routes.MEMORY
    )
    // 悄悄话 / 图片查看器是深色背景：玻璃栏与状态栏图标都按深色表面处理
    val onDarkSurface = route == Routes.WHISPER || route == Routes.VIEWER
    val sg = LocalSg.current
    val hazeState = remember { HazeState() }
    val activity = LocalContext.current as? androidx.activity.ComponentActivity

    SideEffect {
        val darkIcons = if (onDarkSurface) false else !sg.isDark
        activity?.enableEdgeToEdge(
            statusBarStyle = if (darkIcons) SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            else SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { sg.isDark || onDarkSurface }
        )
    }

    Box(Modifier.fillMaxSize().background(sg.paper)) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            // 整个页面内容是毛玻璃的取景源：底栏悬浮时能看到内容从玻璃后穿过
            modifier = Modifier.fillMaxSize().hazeSource(hazeState)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenDetail = { navController.navigate("detail/$it") },
                    onOpenEditor = { navController.navigate(Routes.EDITOR) },
                    onOpenMemory = { navController.navigate(Routes.MEMORY) },
                )
            }
            composable(Routes.CATS) { CategoriesScreen() }
            composable(Routes.WHISPER) { WhisperScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenTheme = { navController.navigate(Routes.THEME) },
                    onOpenPinChange = { navController.navigate(Routes.PIN_CHANGE) },
                )
            }
            composable(Routes.THEME) { ThemeScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.PIN_CHANGE) { PinChangeScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.MEMORY) { MemoryScreen(onBack = { navController.popBackStack() }) }
            composable(
                Routes.EDITOR,
                arguments = listOf(navArgument("entryId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                val id = entry.arguments?.getLong("entryId")?.takeIf { it > 0 }
                EditorScreen(
                    entryId = id,
                    onClose = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(
                Routes.DETAIL,
                arguments = listOf(navArgument("entryId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("entryId") ?: return@composable
                DetailScreen(
                    entryId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("editor?entryId=$it") },
                    onOpenViewer = { index -> navController.navigate("viewer/$id/$index") },
                )
            }
            composable(
                Routes.VIEWER,
                arguments = listOf(
                    navArgument("entryId") { type = NavType.LongType },
                    navArgument("index") { type = NavType.IntType; defaultValue = 0 }
                )
            ) { entry ->
                val id = entry.arguments?.getLong("entryId") ?: return@composable
                val index = entry.arguments?.getInt("index") ?: 0
                ImageViewerScreen(
                    entryId = id,
                    initialIndex = index,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        if (showBar) {
            FloatingGlassBar(
                hazeState = hazeState,
                onDarkSurface = onDarkSurface,
                current = route,
                onSelect = { r -> navController.navigateTopLevel(r) },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            )
        }
    }
}

private fun androidx.navigation.NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * iOS 流体玻璃风格的悬浮底栏：
 * 胶囊造型 + 毛玻璃模糊（Haze，Android 12+ 真实模糊，旧设备自动降级为半透明），
 * 内容从玻璃后穿过；四个入口均分宽度，图标与文字严格居中。
 */
@Composable
private fun FloatingGlassBar(
    hazeState: HazeState,
    onDarkSurface: Boolean,
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(34.dp)
    // 玻璃的基色：只负责“提亮/压暗”，必须保持半透明，内容才透得出来
    val glassTint = if (onDarkSurface) Color(0xFF2A2433) else Color.White
    val borderColor = if (onDarkSurface) Color.White.copy(alpha = .14f) else Color.White.copy(alpha = .55f)
    val glassStyle = HazeStyle(
        backgroundColor = Color.Transparent,
        tints = listOf(HazeTint(glassTint.copy(alpha = if (onDarkSurface) .60f else .58f))),
        blurRadius = 26.dp,
        noiseFactor = .05f,
        // 无真实模糊的旧设备：用更实的半透明底兜底，保证文字可读
        fallbackTint = HazeTint(glassTint.copy(alpha = .92f)),
    )

    Box(modifier) {
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
                .padding(bottom = 10.dp)
                .fillMaxWidth()
                .height(68.dp)
                .shadow(18.dp, pillShape, clip = false, ambientColor = Color(0x2E2A1A10), spotColor = Color(0x382A1A10))
                .clip(pillShape)
                .hazeEffect(hazeState, glassStyle)
                .border(1.dp, borderColor, pillShape),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BarItem(Icons.Rounded.Home, "拾光", current == Routes.HOME || current == Routes.MEMORY, onDarkSurface, Modifier.weight(1f)) { onSelect(Routes.HOME) }
            BarItem(Icons.Rounded.GridView, "栏目", current == Routes.CATS, onDarkSurface, Modifier.weight(1f)) { onSelect(Routes.CATS) }
            BarItem(Icons.Rounded.Nightlight, "悄悄话", current == Routes.WHISPER, onDarkSurface, Modifier.weight(1f)) { onSelect(Routes.WHISPER) }
            BarItem(Icons.Rounded.Person, "我的", current == Routes.SETTINGS || current == Routes.THEME, onDarkSurface, Modifier.weight(1f)) { onSelect(Routes.SETTINGS) }
        }
    }
}

/**
 * 底栏单选项：选中时由一个椭圆胶囊把「图标 + 文字」整个包裹起来，
 * 未选中则无底色；每个选项等宽、内容居中，胶囊随页面明暗切换（悄悄话深色页用半透明金）。
 */
@Composable
private fun BarItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onDarkSurface: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val sg = LocalSg.current
    val activeTint = if (onDarkSurface) Color(0xFFE2B272) else sg.brand
    val idleTint = if (onDarkSurface) Color(0xFF9A90AC) else sg.ink3
    val tint = if (active) activeTint else idleTint
    val pillBg = when {
        active && onDarkSurface -> Color(0x2ED9B36A)   // 半透明金，融进深色玻璃
        active -> sg.brandSoft
        else -> Color.Transparent
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(pillBg)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 7.dp)
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(21.dp))
            Spacer(Modifier.height(3.dp))
            Text(
                label, fontSize = 10.5.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = tint,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
