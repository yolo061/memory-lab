package com.shiguangji.app.ui.memory

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.shiguangji.app.appGraph
import com.shiguangji.app.ui.CategoryChip
import com.shiguangji.app.ui.LocalSg
import com.shiguangji.app.ui.PhotoThumb
import com.shiguangji.app.ui.SgCard
import com.shiguangji.app.util.Dates

@Composable
fun MemoryScreen(onBack: () -> Unit) {
    val graph = LocalContext.current.appGraph
    val sg = LocalSg.current
    val today = remember { Dates.todayIso() }
    val md = remember { Dates.monthDayOf(today) }
    val year = remember { Dates.yearOf(today) }
    val past by graph.entryRepo.pastOnMonthDay(md, year).collectAsState(initial = emptyList())
    val grouped = remember(past) {
        past.groupBy { Dates.yearOf(it.entry.date) }.toSortedMap(compareByDescending { it })
    }

    Column(
        Modifier.fillMaxSize().background(sg.paper).verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = sg.ink) }
            Text("那年今日", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = sg.ink, letterSpacing = 1.sp)
        }
        Column(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val d = Dates.parseOrNull(today)
            Row {
                Text("${d?.monthValue ?: ""}", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = sg.ink, letterSpacing = 3.sp)
                Text("月", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = sg.brand)
                Text("${d?.dayOfMonth ?: ""}", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = sg.ink, letterSpacing = 3.sp)
                Text("日", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = sg.brand)
            }
            Spacer(Modifier.height(6.dp))
            Text("农历的日子每年不同 · 重要的是那天的事", fontSize = 12.5.sp, color = sg.ink3, letterSpacing = 2.sp)
        }

        if (grouped.isEmpty()) {
            com.shiguangji.app.ui.EmptyState("🕰️", "历史上的这一天还是空白\n今天的记录，就是明年的那年今日")
        }
        grouped.forEach { (y, list) ->
            Column(Modifier.padding(horizontal = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 11.dp)) {
                    Text(y.toString(), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic, fontFamily = FontFamily.Serif, color = sg.ink)
                    Spacer(Modifier.width(9.dp))
                    Text(Dates.yearsAgoZh(y, year), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                        color = sg.brand, modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(sg.brandSoft)
                            .padding(horizontal = 10.dp, vertical = 3.dp))
                }
                list.forEach { item ->
                    SgCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { },
                        radius = 20.dp
                    ) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (item.photos.isNotEmpty()) {
                                PhotoThumb(
                                    relPath = item.photos.first().relPath,
                                    photoStore = graph.photoStore,
                                    modifier = Modifier.size(76.dp).clip(RoundedCornerShape(16.dp))
                                )
                                Spacer(Modifier.width(13.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.entry.content.lines().firstOrNull { it.isNotBlank() }?.take(14) ?: "那年的一天",
                                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = sg.ink
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    item.entry.content.ifBlank { "只有照片的一页" },
                                    fontSize = 12.5.sp, color = sg.ink2, lineHeight = 19.sp,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    item.categories.firstOrNull()?.let { CategoryChip(it) }
                                    Spacer(Modifier.weight(1f))
                                    Text(Dates.monthDayZh(item.entry.date), fontSize = 11.sp, color = sg.ink3)
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(
            Modifier.fillMaxWidth()
                .padding(top = 26.dp)
                .padding(bottom = 24.dp + WindowInsets.navigationBars
                    .asPaddingValues().calculateBottomPadding())
        ) {
            Text(
                "— 时间会把日子酿成故事 —",
                fontSize = 11.5.sp, color = sg.ink3, letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
