package com.shiguangji.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shiguangji.app.appGraph
import com.shiguangji.app.data.EntryWithPhotos
import com.shiguangji.app.ui.CategoryChip
import com.shiguangji.app.ui.EntryPreview
import com.shiguangji.app.ui.LocalSg
import com.shiguangji.app.ui.PhotoThumb
import com.shiguangji.app.ui.SgCard
import com.shiguangji.app.ui.SectionHeader
import com.shiguangji.app.util.Dates
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onOpenDetail: (Long) -> Unit,
    onOpenEditor: () -> Unit,
    onOpenMemory: () -> Unit,
) {
    val graph = LocalContext.current.appGraph
    val sg = LocalSg.current

    var query by remember { mutableStateOf("") }
    val listFlow = remember(query) {
        if (query.isBlank()) graph.entryRepo.all else graph.entryRepo.search(query)
    }
    val entries by listFlow.collectAsState(initial = emptyList())

    val today = Dates.todayIso()
    val md = remember { Dates.monthDayOf(today) }
    val year = remember { Dates.yearOf(today) }
    val pastCount by graph.entryRepo.countPastOnMonthDay(md, year).collectAsState(initial = 0)
    val pastEntries by graph.entryRepo.pastOnMonthDay(md, year).collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(sg.paper).statusBarsPadding(),
        // 底部留白 = 悬浮玻璃栏高度 + 导航条，让列表末端从栏下滑过
        contentPadding = PaddingValues(bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        item {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            greeting() + " · " + Dates.monthDayZh(today) + " · " + Dates.weekdayZh(today),
                            fontSize = 12.5.sp, color = sg.ink3, letterSpacing = 1.5.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("拾光", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
                                color = sg.ink, letterSpacing = 2.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("SHIGUANG", fontSize = 13.sp, color = sg.brand,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                    IconButton(onClick = onOpenMemory) {
                        Icon(Icons.Rounded.History, "那年今日", tint = sg.ink)
                    }
                }
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp)
            ) {
                // 「写记录」：搜索框左侧的渐变圆钮，写今天的一笔
                Box(
                    Modifier
                        .size(52.dp)
                        .shadow(10.dp, CircleShape, clip = false, spotColor = Color(0x55C4663F))
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(sg.grad1, sg.grad2)))
                        .clickable(onClick = onOpenEditor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "写记录", tint = Color(0xFFFFF6EE), modifier = Modifier.size(25.dp))
                }
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索记录、照片说明…", fontSize = 13.sp, color = sg.ink3) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = sg.ink3, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(999.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = sg.card,
                        focusedContainerColor = sg.card,
                        unfocusedBorderColor = sg.line,
                        focusedBorderColor = sg.brand,
                        cursorColor = sg.brand,
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (query.isBlank() && pastCount > 0 && pastEntries.isNotEmpty()) {
            item {
                val latest = pastEntries.first()
                val latestYear = Dates.yearOf(latest.entry.date)
                val yearsAgo = year - latestYear
                MemoryBanner(
                    title = "$yearsAgo 年前的今天，" + previewOf(latest),
                    sub = latest.entry.date.replace('-', '.') + " · 那年今日",
                    onClick = onOpenMemory,
                )
            }
        }
        if (entries.isEmpty()) {
            item {
                com.shiguangji.app.ui.EmptyState(
                    emoji = if (query.isBlank()) "☀️" else "🔍",
                    text = if (query.isBlank()) "还没有记录\n点搜索框旁的 +，把今天收进拾光" else "没有找到相关记录，换个词试试"
                )
            }
        }
        entries.groupBy { it.entry.date }.forEach { (date, group) ->
            item(key = "h-$date") {
                val rel = Dates.relativeDayZh(date)
                val label = if (rel == "今天" || rel == "昨天" || rel == "前天") "$rel · ${Dates.monthDayZh(date)} ${Dates.weekdayZh(date)}"
                else "${Dates.monthDayZh(date)} ${Dates.weekdayZh(date)}"
                SectionHeader(label)
            }
            items(group, key = { it.entry.id }) { item ->
                EntryCard(
                    item = item,
                    onClick = { onOpenDetail(item.entry.id) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private fun greeting(): String {
    return when (LocalTime.now().hour) {
        in 5..10 -> "早上好"
        in 11..13 -> "中午好"
        in 14..18 -> "下午好"
        else -> "晚上好"
    }
}

private fun previewOf(item: EntryWithPhotos): String =
    item.entry.content.lineFirst().ifBlank { if (item.photos.isNotEmpty()) "拍下了 ${item.photos.size} 张照片" else "记了一笔" }

private fun String.lineFirst(): String = lines().firstOrNull { it.isNotBlank() }?.take(18) ?: ""

@Composable
private fun MemoryBanner(title: String, sub: String, onClick: () -> Unit) {
    val sg = LocalSg.current
    Column(Modifier.padding(horizontal = 22.dp, vertical = 2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(sg.grad1, sg.grad2)))
                .clickable(onClick = onClick)
                .padding(horizontal = 17.dp, vertical = 15.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("那 年 · 今 日", fontSize = 10.5.sp, color = Color.White.copy(alpha = .85f), letterSpacing = 3.sp)
                Spacer(Modifier.height(4.dp))
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF6EE), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(sub, fontSize = 11.sp, color = Color.White.copy(alpha = .8f))
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
                tint = Color.White, modifier = Modifier.size(26.dp).clip(CircleShape).background(Color.White.copy(alpha = .2f)).padding(4.dp)
            )
        }
    }
}

@Composable
fun EntryCard(
    item: EntryWithPhotos,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sg = LocalSg.current
    SgCard(modifier = modifier.clickable(onClick = onClick), radius = 22.dp) {
        Column(Modifier.padding(13.dp)) {
            if (item.photos.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    val show = item.photos.sortedBy { it.sortIndex }.take(3)
                    show.forEach { photo ->
                        PhotoThumb(
                            relPath = photo.relPath,
                            photoStore = LocalContext.current.appGraph.photoStore,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(if (show.size == 1) 1.9f else 1.18f)
                        )
                    }
                }
                Spacer(Modifier.height(11.dp))
            }
            EntryPreview(item.entry.content.ifBlank { "（只有照片的一条记录）" })
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 多栏目：最多展示 2 枚标签，多出折叠成 +N
                item.categories.take(2).forEach { cat ->
                    CategoryChip(cat)
                    Spacer(Modifier.width(6.dp))
                }
                if (item.categories.size > 2) {
                    Text("+${item.categories.size - 2}", fontSize = 11.sp, color = sg.ink3)
                    Spacer(Modifier.width(4.dp))
                }
                if (item.categories.isEmpty()) {
                    Text("未分类", fontSize = 11.sp, color = sg.ink3)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    timeOf(item.entry.createdAt), fontSize = 11.sp, color = sg.ink3
                )
            }
        }
    }
}

private fun timeOf(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
