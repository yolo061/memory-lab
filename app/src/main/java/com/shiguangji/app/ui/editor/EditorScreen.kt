package com.shiguangji.app.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shiguangji.app.appGraph
import com.shiguangji.app.core.PhotoStore
import com.shiguangji.app.data.PhotoEntity
import com.shiguangji.app.ui.LocalSg
import com.shiguangji.app.ui.rememberToast
import com.shiguangji.app.util.Dates
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

private data class StagedPhoto(
    val relPath: String,
    val caption: String,
    val existingId: Long? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    entryId: Long?,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val graph = LocalContext.current.appGraph
    val sg = LocalSg.current
    val toast = rememberToast()
    val scope = rememberCoroutineScope()

    var loaded by remember { mutableStateOf(entryId == null) }
    var date by remember { mutableStateOf(Dates.todayIso()) }
    var content by remember { mutableStateOf("") }
    val categoryIds = remember { mutableStateListOf<Long>() }
    var showDatePicker by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val staged = remember { mutableStateListOf<StagedPhoto>() }
    val removedIds = remember { mutableStateListOf<Long>() }
    val categories by graph.categoryRepo.all.collectAsState(initial = emptyList())

    LaunchedEffect(entryId) {
        if (entryId != null) {
            graph.entryRepo.get(entryId)?.let { item ->
                date = item.entry.date
                content = item.entry.content
                categoryIds.clear()
                item.categories.forEach { categoryIds.add(it.id) }
                staged.clear()
                item.photos.sortedBy { it.sortIndex }.forEach {
                    staged.add(StagedPhoto(it.relPath, it.caption, it.id))
                }
                loaded = true
            }
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris ->
        uris.forEach { uri ->
            runCatching { staged.add(StagedPhoto(graph.photoStore.import(uri), "")) }
                .onFailure { toast("有一张图片无法读取，已跳过") }
        }
    }

    fun cleanupUnsavedFiles() {
        if (!saved) staged.filter { it.existingId == null }.forEach { graph.photoStore.delete(it.relPath) }
    }

    fun save() {
        if (content.isBlank() && staged.isEmpty()) {
            toast("写点什么，或至少放一张照片吧")
            return
        }
        scope.launch {
            graph.entryRepo.save(
                entryId = entryId,
                date = date,
                content = content.trim(),
                categoryIds = categoryIds.toList(),
                photos = staged.mapIndexed { i, p ->
                    PhotoEntity(id = p.existingId ?: 0L, entryId = entryId ?: 0L, relPath = p.relPath, caption = p.caption, sortIndex = i)
                },
                removedPhotoIds = removedIds.toList(),
            )
            saved = true
            toast("已收进拾光 ✓")
            onSaved()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(sg.paper)
            .statusBarsPadding()
            // 键盘弹出时整体抬升：输入区永远坐在键盘上方（键盘作为一个实体）
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("取消", fontSize = 14.5.sp, color = sg.ink2, modifier = Modifier.clickable {
                cleanupUnsavedFiles(); onClose()
            })
            Spacer(Modifier.weight(1f))
            Text("写记录", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = sg.ink)
            Spacer(Modifier.weight(1f))
            Text("保存", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = sg.brand,
                modifier = Modifier.clickable { save() })
        }
        Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.Bottom) {
            Text("今天想记下什么？", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = sg.ink)
            Spacer(Modifier.width(9.dp))
            Text("都会好好收着", fontSize = 12.sp, color = sg.ink3, modifier = Modifier.padding(bottom = 3.dp))
        }

        // 日期
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 22.dp, top = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(sg.card)
                .border(1.dp, sg.line, RoundedCornerShape(16.dp))
                .clickable { showDatePicker = true }
                .padding(horizontal = 15.dp, vertical = 10.dp)
        ) {
            Icon(Icons.Rounded.CalendarMonth, null, tint = sg.brand, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(Dates.monthDayZh(date), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = sg.ink)
            Spacer(Modifier.width(6.dp))
            Text(Dates.weekdayZh(date), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = sg.ink)
            Spacer(Modifier.width(8.dp))
            Text("点击修改", fontSize = 11.sp, color = sg.ink3)
        }

        // 照片区
        Text(
            "照片 ${staged.size} · 每张都能写说明",
            fontSize = 12.sp, letterSpacing = 2.sp, color = sg.ink3,
            modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 10.dp)
        )
        staged.chunked(2).forEach { rowPhotos ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                rowPhotos.forEach { p ->
                    Column(Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)).background(sg.paper2)) {
                            AsyncImage(
                                model = graph.photoStore.fileOf(p.relPath),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(7.dp)
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = .45f))
                                    .clickable {
                                        if (p.existingId != null) removedIds.add(p.existingId)
                                        staged.remove(p)
                                        if (p.existingId != null) toast("照片已移除")
                                        else graph.photoStore.delete(p.relPath)
                                    },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Rounded.Close, "移除", tint = Color.White, modifier = Modifier.size(13.dp)) }
                        }
                        Spacer(Modifier.height(8.dp))
                        BasicTextField(
                            value = p.caption,
                            onValueChange = { nv ->
                                val idx = staged.indexOfFirst { it === p }
                                if (idx >= 0) staged[idx] = p.copy(caption = nv)
                            },
                            textStyle = TextStyle(fontSize = 12.sp, color = sg.ink2),
                            cursorBrush = SolidColor(sg.brand),
                            decorationBox = { inner ->
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(sg.paper2)
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    if (p.caption.isEmpty()) Text("给这张照片写点什么…", fontSize = 12.sp, color = sg.ink3)
                                    inner()
                                }
                            }
                        )
                    }
                }
                if (rowPhotos.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Box(
            Modifier
                .padding(horizontal = 22.dp, vertical = 6.dp)
                .fillMaxWidth()
                .aspectRatio(2.06f)
                .clip(RoundedCornerShape(18.dp))
                .background(sg.brandSoft2)
                .border(1.8.dp, sg.brandSoft, RoundedCornerShape(18.dp))
                .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Add, "添加照片", tint = sg.brand, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(7.dp))
                Text("添加照片", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = sg.brand)
            }
        }

        // 栏目（可多选）
        Text(
            if (categoryIds.isEmpty()) "放进哪些栏目 · 可多选" else "已选 ${categoryIds.size} 个栏目 · 可多选",
            fontSize = 12.sp, letterSpacing = 2.sp, color = sg.ink3,
            modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 10.dp)
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            categories.forEach { cat ->
                val (bg, on) = sg.category(cat.colorKey)
                val active = cat.id in categoryIds
                Text(
                    "${cat.emoji} ${cat.name}",
                    fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                    color = if (active) on else sg.ink2,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) bg else sg.card)
                        .border(1.5.dp, if (active) bg else sg.line, RoundedCornerShape(999.dp))
                        .clickable {
                            if (active) categoryIds.remove(cat.id) else categoryIds.add(cat.id)
                        }
                        .padding(horizontal = 15.dp, vertical = 9.dp)
                )
            }
        }

        // 正文
        Text("正文", fontSize = 12.sp, letterSpacing = 2.sp, color = sg.ink3,
            modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 8.dp))
        BasicTextField(
            value = content,
            onValueChange = { content = it },
            textStyle = TextStyle(fontSize = 15.sp, color = sg.ink, lineHeight = 28.sp),
            cursorBrush = SolidColor(sg.brand),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(150.dp),
            decorationBox = { inner ->
                Box {
                    if (content.isEmpty()) Text("记下今天的心情与故事，也可以什么都不写…", fontSize = 15.sp, color = sg.ink3, lineHeight = 28.sp)
                    inner()
                }
            }
        )

        // 保存大按钮
        Box(
            Modifier
                .padding(horizontal = 22.dp, vertical = 26.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(sg.grad1, sg.grad2)))
                .clickable { save() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("收 进 拾 光", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFFFFF6EE), letterSpacing = 4.sp)
        }
        // 无底栏页面：自行避开系统手势条
        androidx.compose.foundation.layout.Spacer(
            Modifier.fillMaxWidth().height(4.dp).navigationBarsPadding()
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = Dates.parseOrNull(date)
                ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) { DatePicker(state = pickerState) }
    }
}
