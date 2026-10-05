package com.shiguangji.app.ui.theme

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shiguangji.app.appGraph
import com.shiguangji.app.core.ThemePack
import com.shiguangji.app.ui.LocalSg
import com.shiguangji.app.ui.rememberSgColors
import com.shiguangji.app.ui.rememberToast
import kotlinx.coroutines.launch

@Composable
fun ThemeScreen(onBack: () -> Unit) {
    val graph = LocalContext.current.appGraph
    val sg = LocalSg.current
    val toast = rememberToast()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val packs by graph.themeManager.packs.collectAsState()
    val currentId by graph.themeManager.currentId.collectAsState()
    val dynamic by graph.settings.dynamicColor.collectAsState(initial = false)
    var showJson by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching { graph.themeManager.import(uri) }
                .onSuccess { toast("已换上「${it.name}」✓") }
                .onFailure { toast(it.message ?: "导入失败") }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use {
                    it.write(graph.themeManager.current.rawJson.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("无法写入文件")
            }.onSuccess { toast("主题包已导出 ✓") }
                .onFailure { toast("导出失败：${it.message}") }
        }
    }

    Column(
        Modifier.fillMaxSize().background(sg.paper).verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = sg.ink) }
            Text("主题外观", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = sg.ink, letterSpacing = 1.sp)
        }
        Text(
            "每一套主题就是一个主题包 —— 一份描述配色的 JSON 文件，点一下立刻换装，也可以导出分享、在新手机导入。",
            fontSize = 12.sp, color = sg.ink3, lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
        )
        packs.chunked(2).forEach { rowPacks ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                rowPacks.forEach { pack ->
                    ThemeCard(
                        pack = pack,
                        active = pack.id == currentId,
                        onClick = {
                            scope.launch {
                                graph.themeManager.setCurrent(pack.id)
                                toast("已换上「${pack.name}」")
                            }
                        },
                        onDelete = if (pack.fileName != null) {
                            {
                                scope.launch {
                                    graph.themeManager.delete(pack)
                                    toast("已删除主题包")
                                }
                            }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowPacks.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 22.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(19.dp))
                .background(sg.card)
                .padding(15.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("跟随系统取色", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = sg.ink)
                Spacer(Modifier.height(2.dp))
                Text("Android 12+ 壁纸取色，栏目配色仍来自主题包", fontSize = 11.sp, color = sg.ink3)
            }
            Switch(
                checked = dynamic,
                onCheckedChange = { scope.launch { graph.settings.setDynamicColor(it) } },
                colors = SwitchDefaults.colors(checkedTrackColor = sg.brand)
            )
        }

        Row(Modifier.padding(horizontal = 22.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(sg.brand)
                    .clickable { showJson = true }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) { Text("查看主题包文件", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFF6EE)) }
            Box(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(sg.card)
                    .border(1.dp, sg.line, RoundedCornerShape(15.dp))
                    .clickable { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) { Text("导入主题包", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = sg.ink2) }
        }
        Box(
            Modifier
                .padding(horizontal = 22.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(sg.card)
                .border(1.dp, sg.line, RoundedCornerShape(15.dp))
                .clickable { exportLauncher.launch("${graph.themeManager.current.name}.json") }
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) { Text("导出当前主题包", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = sg.ink2) }

        Text(
            "📦 主题包存放在应用目录 themes/ 下，启动时自动识别；缺失字段回退默认值，未知字段自动忽略 —— 老版本也能打开新格式的主题包。",
            fontSize = 11.sp, color = sg.ink3, lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
        )
        Spacer(
            Modifier.height(
                16.dp + WindowInsets.navigationBars
                    .asPaddingValues().calculateBottomPadding() + 100.dp
            )
        )
    }

    if (showJson) {
        val current = graph.themeManager.current
        AlertDialog(
            onDismissRequest = { showJson = false },
            containerColor = sg.card,
            title = { Text("主题包文件 · ${current.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2B241C))
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        current.rawJson.ifBlank { "{}" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp, lineHeight = 17.sp, color = Color(0xFFF3E5C8),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showJson = false }) { Text("关闭") } }
        )
    }
}

@Composable
private fun ThemeCard(
    pack: ThemePack,
    active: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val p = rememberSgColors(pack)
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(p.card)
            .border(
                if (active) 2.dp else 1.dp,
                if (active) p.brand else p.line,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(p.paper, p.paper2)))
                    .border(1.dp, p.line, RoundedCornerShape(13.dp))
            ) {
                Row(
                    Modifier.align(Alignment.CenterStart).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(13.dp).clip(CircleShape).background(p.grad2))
                    Box(Modifier.size(13.dp).clip(CircleShape).background(p.green))
                    Box(Modifier.size(13.dp).clip(CircleShape).background(p.blue))
                    Box(
                        Modifier
                            .align(Alignment.CenterVertically)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(p.card)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Aa", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.ink)
                    }
                }
                if (active) {
                    Text(
                        "使用中", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFF6EE), letterSpacing = 1.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(p.brand)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (onDelete != null) {
                    Text(
                        "删除", fontSize = 9.5.sp,
                        color = p.ink3,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onDelete)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(pack.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.ink, modifier = Modifier.padding(horizontal = 3.dp))
            Text(
                pack.author + (if (pack.isDark) " · 深色" else ""),
                fontSize = 10.5.sp, color = p.ink3,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
            )
        }
    }
}
