package com.shiguangji.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shiguangji.app.appGraph
import com.shiguangji.app.core.BackupManager
import com.shiguangji.app.ui.LocalSg
import com.shiguangji.app.ui.PageTitle
import com.shiguangji.app.ui.SgCard
import com.shiguangji.app.ui.StatColumn
import com.shiguangji.app.ui.rememberToast
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onOpenTheme: () -> Unit, onOpenPinChange: () -> Unit = {}) {
    val graph = LocalContext.current.appGraph
    val sg = LocalSg.current
    val toast = rememberToast()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    val entries by graph.entryRepo.all.collectAsState(initial = emptyList())
    val photoCount by graph.entryRepo.photoCount.collectAsState(initial = 0)
    val catCount by graph.categoryRepo.all.collectAsState(initial = emptyList())
    val storageBytes by produceState(0L) { value = graph.photoStore.totalSizeBytes() }
    var lastBackupAt by remember { mutableStateOf<String?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    val days = remember(entries) { entries.map { it.entry.date }.distinct().size }
    val hasPin by graph.pinManager.isPinSet.collectAsState(initial = null)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { graph.backupManager.exportTo(it) }
                    ?: throw IllegalStateException("无法写入文件")
            }.onSuccess {
                lastBackupAt = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("M月d日 HH:mm"))
                toast("备份完成 ✓ 建议把 zip 拷到电脑或网盘")
            }.onFailure { toast("备份失败：${it.message}") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { graph.backupManager.importFrom(it) }
                    ?: throw IllegalStateException("无法读取文件")
            }.onSuccess {
                toast("恢复成功，正在重新打开拾光记…")
                kotlinx.coroutines.delay(600)
                BackupManager.restartApp(ctx)
            }.onFailure { toast("恢复失败：${it.message}") }
        }
    }

    Column(
        Modifier.fillMaxSize().background(sg.paper).verticalScroll(rememberScrollState())
    ) {
        Column(Modifier.statusBarsPadding().padding(top = 14.dp)) {
            PageTitle("我的")
        }
        SgCard(Modifier.padding(22.dp)) {
            Row(Modifier.padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(sg.brandSoft, sg.brandSoft2))),
                    contentAlignment = Alignment.Center
                ) { Text("🌻", fontSize = 28.sp) }
                Spacer(Modifier.size(15.dp))
                Column {
                    Text("亲爱的记录者", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = sg.ink)
                    Spacer(Modifier.height(4.dp))
                    Text("坚持记录 $days 天 · 感谢每一个认真生活的日子", fontSize = 12.sp, color = sg.ink2)
                }
            }
        }
        SgCard(Modifier.padding(horizontal = 22.dp).padding(bottom = 14.dp)) {
            Row(Modifier.padding(vertical = 15.dp)) {
                StatColumn("$days", "记录天数", Modifier.weight(1f))
                StatColumn("$photoCount", "照片", Modifier.weight(1f))
                StatColumn("${catCount.size}", "栏目", Modifier.weight(1f))
            }
        }

        Column(Modifier.padding(horizontal = 22.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            SetItem(emoji = "🎨", bg = sg.brandSoft, title = "主题外观",
                sub = "主题包可换装、导入导出", onClick = onOpenTheme)
            if (hasPin == true) {
                SetItem(emoji = "🔑", bg = sg.roseSoft, title = "修改悄悄话密码",
                    sub = "需先验证旧密码，再设置新密码", onClick = onOpenPinChange)
            }
            SetItem(emoji = "💾", bg = sg.blueSoft, title = "备份与恢复",
                sub = if (lastBackupAt == null) "全部数据都在本机 · 建议定期备份" else "上次备份：$lastBackupAt",
                onClick = { showBackupDialog = true })
            SetItem(emoji = "📁", bg = sg.greenSoft, title = "存储管理",
                sub = "照片已用 %.1f MB · 按 年/月 规范归档".format(storageBytes / 1024.0 / 1024.0),
                onClick = { toast("照片存放在应用专属目录，卸载自动清理") })
            SetItem(emoji = "📖", bg = sg.purpleSoft, title = "关于拾光记",
                sub = "v1.0 · 纯离线 · 无任何网络权限", onClick = { showAbout = true })
        }
        Text(
            "拾光记 · 把每个普通的日子，都收进时光里 ☀️",
            fontSize = 11.sp, color = sg.ink3, letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth()
                .padding(top = 26.dp)
                .padding(bottom = 100.dp + WindowInsets.navigationBars
                    .asPaddingValues().calculateBottomPadding())
                .then(Modifier),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            containerColor = sg.card,
            title = { Text("备份与恢复", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = { Text("备份 = 数据库 + 全部照片打包成一个 zip，可以存到电脑、网盘或新手机。", fontSize = 13.5.sp, color = sg.ink2, lineHeight = 22.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showBackupDialog = false
                    exportLauncher.launch("拾光记备份-${System.currentTimeMillis()}.zip")
                }) { Text("导出备份", color = sg.brand) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupDialog = false
                    showRestoreConfirm = true
                }) { Text("从备份恢复", color = sg.ink2) }
            }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            containerColor = sg.card,
            title = { Text("从备份恢复？", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = { Text("恢复会用备份文件覆盖当前所有记录和照片，现有数据将被替换。\n\n旧版本备份可以恢复到新版本；来自更高版本的备份会提示先升级。", fontSize = 13.5.sp, color = sg.ink2, lineHeight = 22.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                }) { Text("选择备份文件", color = sg.brand) }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false }) { Text("取消") } }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = sg.card,
            title = { Text("拾光记 v1.0", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "一个完全离线的日常记录应用。\n\n" +
                        "☀️ 无网络权限，数据永不离开设备\n" +
                        "🎨 主题包 = 一份 JSON 配色，可导入导出\n" +
                        "💾 数据库 + 照片规范存储，支持一键备份\n" +
                        "🔄 版本升级保证兼容旧数据（数据库迁移 + 格式回退）\n\n" +
                        "把每个普通的日子，都收进时光里。",
                    fontSize = 13.5.sp, color = sg.ink2, lineHeight = 22.sp
                )
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("好的") } }
        )
    }
}

@Composable
private fun SetItem(emoji: String, bg: androidx.compose.ui.graphics.Color, title: String, sub: String, onClick: () -> Unit) {
    val sg = LocalSg.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(19.dp))
            .background(sg.card)
            .clickable(onClick = onClick)
            .padding(15.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(bg),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 19.sp) }
        Spacer(Modifier.size(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = sg.ink)
            Spacer(Modifier.height(2.dp))
            Text(sub, fontSize = 11.sp, color = sg.ink3)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = sg.ink3, modifier = Modifier.size(16.dp))
    }
}
