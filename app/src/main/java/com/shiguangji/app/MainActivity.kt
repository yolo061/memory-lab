package com.shiguangji.app

import android.graphics.Color as AndroidColor
import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

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
    const val TABS = "tabs"
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
    val route = backStack?.destination?.route ?: Routes.TABS
    val scope = rememberCoroutineScope()

    // 四个主页面由 HorizontalPager 承载：滑页与底栏高亮共用同一个连续位置
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val tabPos = when (route) {
        Routes.THEME -> 3f     // 主题页挂在「我的」下
        Routes.MEMORY -> 0f    // 那年今日挂在「拾光」下
        else -> pagerState.currentPage + pagerState.currentPageOffsetFraction
    }
    val showBar = route == Routes.TABS || route == Routes.THEME || route == Routes.MEMORY
    // 滑到悄悄话两侧中间时就按深色表面处理；图片查看器是纯黑页
    val onDarkSurface = route == Routes.VIEWER ||
        (route == Routes.TABS && tabPos > 1.5f && tabPos < 2.5f)
    val sg = LocalSg.current
    val hazeState = remember { HazeState() }
    val activity = LocalContext.current as? androidx.activity.ComponentActivity

    // 系统栏图标亮暗只在跨越阈值时切换一次（不能跟随滑动手势逐帧重设）
    DisposableEffect(onDarkSurface, sg.isDark) {
        val darkIcons = !onDarkSurface && !sg.isDark
        activity?.enableEdgeToEdge(
            statusBarStyle = if (darkIcons) SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            else SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { sg.isDark || onDarkSurface }
        )
        onDispose { }
    }

    Box(Modifier.fillMaxSize().background(sg.paper)) {
        NavHost(
            navController = navController,
            startDestination = Routes.TABS,
            // 整个页面内容是毛玻璃的取景源：底栏悬浮时能看到内容从玻璃后穿过
            modifier = Modifier.fillMaxSize().hazeSource(hazeState)
        ) {
            composable(Routes.TABS) {
                TabsPager(
                    pagerState = pagerState,
                    onOpenDetail = { navController.navigate("detail/$it") },
                    onOpenEditor = { navController.navigate(Routes.EDITOR) },
                    onOpenMemory = { navController.navigate(Routes.MEMORY) },
                    onOpenTheme = { navController.navigate(Routes.THEME) },
                    onOpenPinChange = { navController.navigate(Routes.PIN_CHANGE) },
                )
            }
            composable(Routes.THEME) { ThemeScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.MEMORY) { MemoryScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.PIN_CHANGE) { PinChangeScreen(onBack = { navController.popBackStack() }) }
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
                dark = onDarkSurface,
                tabPos = tabPos,
                onSelect = { index ->
                    if (route == Routes.TABS) {
                        // 点选项：高亮块和页面一起从当前位置平滑滑过去
                        scope.launch { pagerState.animateScrollToPage(index) }
                    } else {
                        // 主题 / 那年今日等子页面：先定位，再退回主页面
                        scope.launch {
                            pagerState.scrollToPage(index)
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            )
        }
    }
}

/**
 * 四个主页面：拾光 / 栏目 / 悄悄话 / 我的。
 * 左右滑动即可切换，底栏高亮块随滑动连续移动；四页常驻，来回滑不重新加载。
 */
@Composable
private fun TabsPager(
    pagerState: PagerState,
    onOpenDetail: (Long) -> Unit,
    onOpenEditor: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenPinChange: () -> Unit,
) {
    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 3,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> HomeScreen(
                onOpenDetail = onOpenDetail,
                onOpenEditor = onOpenEditor,
                onOpenMemory = onOpenMemory,
            )
            1 -> CategoriesScreen()
            2 -> WhisperScreen(pageFocused = pagerState.currentPage == 2)
            3 -> SettingsScreen(onOpenTheme = onOpenTheme, onOpenPinChange = onOpenPinChange)
        }
    }
}

/**
 * iOS 流体玻璃风格的悬浮底栏：
 * 胶囊造型 + 毛玻璃模糊（Haze，Android 12+ 真实模糊，旧设备自动降级为半透明），
 * 内容从玻璃后穿过；高亮块是一块圆角方砖，随 Pager 的连续位置平滑滑动。
 */
@Composable
private fun FloatingGlassBar(
    hazeState: HazeState,
    dark: Boolean,
    tabPos: Float,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sg = LocalSg.current
    val pillShape = RoundedCornerShape(34.dp)
    // 玻璃的基色：只负责“提亮/压暗”，必须保持半透明，内容才透得出来
    val glassTint = if (dark) Color(0xFF2A2433) else Color.White
    val borderColor = if (dark) Color.White.copy(alpha = .14f) else Color.White.copy(alpha = .55f)
    val glassStyle = HazeStyle(
        backgroundColor = Color.Transparent,
        tints = listOf(HazeTint(glassTint.copy(alpha = if (dark) .60f else .58f))),
        blurRadius = 26.dp,
        noiseFactor = .05f,
        // 无真实模糊的旧设备：用更实的半透明底兜底，保证文字可读
        fallbackTint = HazeTint(glassTint.copy(alpha = .92f)),
    )
    val pos = tabPos.coerceIn(0f, 3f)

    Box(modifier) {
        Box(
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
                .border(1.dp, borderColor, pillShape)
        ) {
            BoxWithConstraints(Modifier.matchParentSize()) {
                val itemW = maxWidth / 4
                // 高亮方砖：滑动时跟手，点击选项时随 Pager 动画滑行；颜色跨明暗页渐变
                val tileColor by animateColorAsState(
                    targetValue = if (dark) Color(0x2ED9C096) else sg.brandSoft,
                    animationSpec = tween(180), label = "tabTile"
                )
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = itemW * pos + 12.dp)
                        .width(itemW - 24.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(tileColor)
                )
            }
            Row(Modifier.matchParentSize(), verticalAlignment = Alignment.CenterVertically) {
                BarItem(Icons.Rounded.Home, "拾光", 0, pos, dark, Modifier.weight(1f)) { onSelect(0) }
                BarItem(Icons.Rounded.GridView, "栏目", 1, pos, dark, Modifier.weight(1f)) { onSelect(1) }
                BarItem(Icons.Rounded.Nightlight, "悄悄话", 2, pos, dark, Modifier.weight(1f)) { onSelect(2) }
                BarItem(Icons.Rounded.Person, "我的", 3, pos, dark, Modifier.weight(1f)) { onSelect(3) }
            }
        }
    }
}

/**
 * 底栏单选项：只负责「图标 + 文字」，选中底色由滑动的高亮方砖承担；
 * 图标与文字严格居中，颜色在高亮块滑过中点时切换。
 */
@Composable
private fun BarItem(
    icon: ImageVector,
    label: String,
    index: Int,
    tabPos: Float,
    dark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val sg = LocalSg.current
    val active = tabPos.roundToInt() == index
    // 深色页（悄悄话）的选中金与 WhisperNight 香槟金同源
    val activeTint = if (dark) Color(0xFFE3C89B) else sg.brand
    val idleTint = if (dark) Color(0xFF9A90AC) else sg.ink3
    val tint = if (active) activeTint else idleTint
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
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
