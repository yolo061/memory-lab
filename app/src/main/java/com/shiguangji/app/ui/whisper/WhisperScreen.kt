package com.shiguangji.app.ui.whisper

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.shiguangji.app.appGraph
import com.shiguangji.app.core.PinHash
import com.shiguangji.app.data.WhisperEntity
import com.shiguangji.app.ui.LocalSg
import com.shiguangji.app.ui.rememberToast
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val KEY_LETTERS = mapOf('2' to "ABC", '3' to "DEF", '4' to "GHI", '5' to "JKL",
    '6' to "MNO", '7' to "PQRS", '8' to "TUV", '9' to "WXYZ")

/**
 * 悄悄话夜间配色：低饱和的暖夜色 + 香槟金，比原配色更柔和不刺眼。
 * PinShell / WhisperList / PinChangeScreen 共用，保证同一页气质一致。
 */
internal object WhisperNight {
    val bg = Color(0xFF211E27)         // 夜幕底色：比纯黑亮一档、去紫感
    val card = Color(0xFF2C2835)       // 卡片 / 输入框面
    val gold = Color(0xFFD9C096)       // 香槟金：降饱和的主强调色
    val goldSoft = Color(0x26D9C096)   // 金色的半透明底
    val title = Color(0xFFF2EDE5)      // 暖白标题
    val body = Color(0xFFE5DFE0)       // 正文
    val sub = Color(0xFFA8A0B0)        // 次级文字
    val faint = Color(0xFF8A8194)      // 弱提示文字
    val dotBorder = Color(0xFF787089)  // PIN 圆点描边
    val keyBg = Color(0x16FFFFFF)      // 圆键：极淡的白色柔面
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhisperScreen() {
    val graph = LocalContext.current.appGraph
    val sg = LocalSg.current
    val toast = rememberToast()

    // PIN 是否已设置：响应式收集——首次设置完成后自动切到解锁/列表视图
    val hasPin by graph.pinManager.isPinSet.collectAsState(initial = null)

    Box(Modifier.fillMaxSize().background(sg.paper)) {
        var unlocked by remember { mutableStateOf(graph.pinManager.sessionUnlocked) }

        // 自动上锁：离开悄悄话页、或 App 退到后台，立即回到锁定态
        val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        DisposableEffect(owner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE) {
                    graph.pinManager.sessionUnlocked = false
                    unlocked = false
                }
            }
            owner.lifecycle.addObserver(observer)
            onDispose {
                owner.lifecycle.removeObserver(observer)
                graph.pinManager.sessionUnlocked = false
            }
        }

        when (hasPin) {
            null -> {}
            false -> SetupPinView(onDone = { unlocked = true; toast("已设置，悄悄话上锁了 🌙") })
            true -> if (unlocked) {
                WhisperList(onLock = {
                    graph.pinManager.sessionUnlocked = false
                    unlocked = false
                    toast("已上锁 🌙")
                })
            } else LockView(onUnlocked = { unlocked = true })
        }
    }
}

/** 首次进入：设置 PIN（输入两次）。 */
@Composable
private fun SetupPinView(onDone: () -> Unit) {
    val graph = LocalContext.current.appGraph
    val toast = rememberToast()
    val scope = rememberCoroutineScope()
    var first by remember { mutableStateOf<String?>(null) }
    var digits by remember { mutableStateOf("") }

    PinShell(
        emoji = "🌙",
        title = "悄悄话",
        subtitle = if (first == null) "设置 4 位 PIN 码，这一页只说给自己听" else "再输入一遍确认",
        digits = digits,
        onDigit = { d ->
            if (digits.length < 4) {
                digits += d
                if (digits.length == 4) {
                    if (first == null) {
                        first = digits; digits = ""
                    } else if (digits == first) {
                        // 主线程作用域：Toast 与界面状态都必须在主线程更新
                        val pin = digits
                        scope.launch {
                            graph.pinManager.setPin(pin)
                            onDone()
                        }
                    } else {
                        toast("两次输入不一致，重新来")
                        first = null; digits = ""
                    }
                }
            }
        },
        onDelete = { if (digits.isNotEmpty()) digits = digits.dropLast(1) },
    )
}

/** 锁定页：PIN 键盘 + 指纹。 */
@Composable
private fun LockView(onUnlocked: () -> Unit) {
    val graph = LocalContext.current.appGraph
    val sg = LocalSg.current
    val toast = rememberToast()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val biometricOn by graph.settings.biometricEnabled.collectAsState(initial = true)
    var digits by remember { mutableStateOf("") }
    var trying by remember { mutableStateOf(false) }

    val canBiometric = remember {
        runCatching {
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
        }.getOrDefault(false)
    }

    fun showBiometric() {
        val activity = context as? FragmentActivity ?: run { toast("当前页面不支持指纹"); return }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    graph.pinManager.sessionUnlocked = true
                    onUnlocked()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("解锁悄悄话")
            .setNegativeButtonText("用 PIN 码")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }

    PinShell(
        emoji = "🌙",
        title = "悄悄话",
        subtitle = "输入 PIN 码解锁 · 这一页只说给自己听",
        digits = digits,
        onDigit = { d ->
            if (!trying && digits.length < 4) {
                digits += d
                if (digits.length == 4) {
                    trying = true
                    val pin = digits
                    scope.launch {
                        val ok = graph.pinManager.verify(pin)
                        if (ok) onUnlocked()
                        else { toast("PIN 不对，再试试"); digits = "" }
                        trying = false
                    }
                }
            }
        },
        onDelete = { if (digits.isNotEmpty()) digits = digits.dropLast(1) },
        onBiometric = if (canBiometric && biometricOn) {
            { showBiometric() }
        } else null,
    )
}

/** PIN 页公共骨架：月亮徽标 + 圆点 + 键盘（修改密码等场景复用）。 */
@Composable
internal fun PinShell(
    emoji: String,
    title: String,
    subtitle: String,
    digits: String,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onBiometric: (() -> Unit)? = null,
) {
    val night = WhisperNight
    // letterSpacing 会让居中文本整体偏左（尾部多出一段空隙），按字数补回来
    val density = LocalDensity.current
    val trackingFix = with(density) { (2.sp * title.length).toDp() }
    Column(
        Modifier.fillMaxSize().background(night.bg).statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(70.dp))
        Box(
            Modifier.size(74.dp).clip(RoundedCornerShape(26.dp))
                .background(night.goldSoft),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 34.sp) }
        Spacer(Modifier.height(18.dp))
        Text(
            title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = night.title,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = trackingFix)
        )
        Spacer(Modifier.height(8.dp))
        Text(subtitle, fontSize = 12.sp, color = night.sub, textAlign = TextAlign.Center, lineHeight = 19.sp)
        Spacer(Modifier.height(34.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.size(11.dp).clip(CircleShape)
                        .background(if (i < digits.length) night.gold else Color.Transparent)
                        .border(1.5.dp, night.dotBorder, CircleShape)
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(17.dp)) {
            listOf(listOf('1', '2', '3'), listOf('4', '5', '6'), listOf('7', '8', '9')).forEach { rowKeys ->
                Row(horizontalArrangement = Arrangement.spacedBy(17.dp)) {
                    rowKeys.forEach { k -> KeyButton(k) { onDigit(k) } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(17.dp)) {
                if (onBiometric != null) {
                    GhostKey(onBiometric) {
                        Icon(Icons.Rounded.Fingerprint, "指纹解锁",
                            tint = night.gold, modifier = Modifier.size(27.dp))
                    }
                } else {
                    GhostKey(null)
                }
                KeyButton('0') { onDigit('0') }
                GhostKey(onDelete) {
                    Icon(Icons.Rounded.Backspace, "删除", tint = night.gold, modifier = Modifier.size(24.dp))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("PIN + 生物识别加密 · 不上传任何服务器", fontSize = 10.5.sp, color = night.faint)
        // 悬浮玻璃栏留位：键盘整体坐在栏上方，不再被压住
        Spacer(
            Modifier.height(
                12.dp + androidx.compose.foundation.layout.WindowInsets.navigationBars
                    .asPaddingValues().calculateBottomPadding() + 96.dp
            )
        )
    }
}

private const val KEY_LETTERS_EXTRA = "unused"

@Composable
private fun KeyButton(k: Char, onClick: () -> Unit) {
    val night = WhisperNight
    val letters = KEY_LETTERS[k]
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(night.keyBg)
            .clickable(onClick = onClick)
    ) {
        Text(k.toString(), fontSize = 23.sp, color = night.title, textAlign = TextAlign.Center)
        Text(letters ?: " ", fontSize = 8.sp, color = night.faint, textAlign = TextAlign.Center)
    }
}

/** 空占位或带内容的圆键（指纹 / 删除）。 */
@Composable
private fun GhostKey(onClick: (() -> Unit)?, content: @Composable () -> Unit = {}) {
    Box(
        Modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        if (onClick != null) {
            Box(
                Modifier.fillMaxSize().clip(CircleShape).clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) { content() }
        }
    }
}

/** 已解锁：悄悄话列表 + 写一句。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhisperList(onLock: () -> Unit) {
    val graph = LocalContext.current.appGraph
    val night = WhisperNight
    val toast = rememberToast()
    val scope = rememberCoroutineScope()
    val whispers by graph.whisperRepo.all.collectAsState(initial = emptyList())
    var sheetOpen by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        Modifier.fillMaxSize().background(night.bg).statusBarsPadding()
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("悄悄话 🌙", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                    color = night.title, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                // 手动上锁：立刻回到 PIN/指纹锁定态
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(night.goldSoft)
                        .clickable(onClick = onLock)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.Lock, "上锁", tint = night.gold, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("上锁", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = night.gold)
                }
                Spacer(Modifier.width(10.dp))
                Text("共 ${whispers.size} 条", fontSize = 12.sp, color = night.faint)
            }
            Spacer(Modifier.height(6.dp))
            Text("这些话，只说给自己听 · 加密存放在这台手机里", fontSize = 12.sp, color = night.sub, letterSpacing = 1.sp)
        }
        if (whispers.isEmpty()) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {}
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("还没有悄悄话\n第一句，写给自己 🌙", fontSize = 13.sp, color = night.faint,
                    textAlign = TextAlign.Center, lineHeight = 22.sp)
            }
            Spacer(Modifier.height(30.dp))
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                whispers.forEach { w -> WhisperCard(w, onDelete = { scope.launch { graph.whisperRepo.delete(w.id) } }) }
                Spacer(Modifier.height(14.dp))
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(night.goldSoft)
                    .clickable { sheetOpen = true }
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text("＋", fontSize = 15.sp, color = night.gold, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("写一句悄悄话", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = night.gold)
            }
        }
        // 悬浮玻璃栏留位
        Spacer(
            Modifier.height(
                96.dp + WindowInsets.navigationBars
                    .asPaddingValues().calculateBottomPadding()
            )
        )
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            containerColor = night.card,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 22.dp)
                    // 键盘弹出时内容整体上浮；键盘收起时避让手势条
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("悄悄话 🌙", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = night.title)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        if (draft.isBlank()) return@TextButton
                        val text = draft
                        draft = ""
                        sheetOpen = false
                        scope.launch { graph.whisperRepo.add(text); toast("已封存 ✓") }
                    }) { Text("封存这句话", color = night.gold, fontWeight = FontWeight.Bold) }
                }
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it },
                    placeholder = { Text("有些话不必给任何人看，写在这里就好…", fontSize = 13.5.sp, color = night.sub) },
                    minLines = 4,
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x12FFFFFF), unfocusedContainerColor = Color(0x12FFFFFF),
                        focusedBorderColor = night.gold, unfocusedBorderColor = Color(0x1AFFFFFF),
                        cursorColor = night.gold,
                        focusedTextColor = night.title, unfocusedTextColor = night.title,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("🔒 加密保存在本机 · 不上传任何服务器", fontSize = 11.sp, color = night.sub, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun WhisperCard(w: WhisperEntity, onDelete: () -> Unit) {
    val night = WhisperNight
    val time = remember(w.createdAt) {
        Instant.ofEpochMilli(w.createdAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("M月d日 · HH:mm"))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(night.card)
            .clickable(onClick = onDelete)
            .padding(start = 17.dp, end = 17.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Row {
            Box(
                Modifier
                    .width(3.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(night.gold)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(time, fontSize = 10.5.sp, color = night.faint, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                Text(w.content, fontSize = 13.5.sp, color = night.body, lineHeight = 24.sp)
            }
        }
    }
}
