package com.shiguangji.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shiguangji.app.appGraph
import com.shiguangji.app.ui.CategoryChip
import com.shiguangji.app.ui.LocalSg
import com.shiguangji.app.ui.rememberToast
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(entryId: Long, onBack: () -> Unit, onEdit: (Long) -> Unit, onOpenViewer: (Int) -> Unit) {
    val graph = LocalContext.current.appGraph
    val sg = LocalSg.current
    val toast = rememberToast()
    val scope = rememberCoroutineScope()
    val item by graph.entryRepo.byId(entryId).collectAsState(initial = null)
    var confirmDelete by remember { mutableStateOf(false) }

    val current = item ?: return
    val photos = current.photos.sortedBy { it.sortIndex }
    val pagerState = rememberPagerState(pageCount = { photos.size })

    Box(Modifier.fillMaxSize().background(sg.paper)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(340.dp)) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val photo = photos[page]
                    AsyncImage(
                        model = graph.photoStore.fileOf(photo.relPath),
                        contentDescription = "照片 ${page + 1}，点按看大图",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clickable { onOpenViewer(page) }
                    )
                }
                // 说明浮层
                if (photos.getOrNull(pagerState.currentPage)?.caption?.isNotBlank() == true) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 15.dp, vertical = 11.dp)
                    ) {
                        Text("“", fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontSize = 24.sp, color = Color.White.copy(alpha = .7f))
                        Text(
                            photos[pagerState.currentPage].caption,
                            fontSize = 13.sp, color = Color(0xFFFFF6EE), lineHeight = 21.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                if (photos.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (photos.getOrNull(pagerState.currentPage)?.caption?.isBlank() != false) 14.dp else 90.dp)
                    ) {
                        photos.indices.forEach { i ->
                            Box(
                                Modifier
                                    .size(if (i == pagerState.currentPage) 17.dp else 7.dp, 7.dp)
                                    .clip(CircleShape)
                                    .background(if (i == pagerState.currentPage) Color.White else Color.White.copy(alpha = .45f))
                            )
                        }
                    }
                }
            }

            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Dates4Detail(current.entry.date), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = sg.ink
                    )
                    Spacer(Modifier.size(8.dp))
                    Box(Modifier.size(3.dp).clip(CircleShape).background(sg.ink3))
                    Spacer(Modifier.size(8.dp))
                    Text(timeOf(current.entry.createdAt), fontSize = 12.sp, color = sg.ink3)
                }
                if (current.categories.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    // 多栏目标签
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        current.categories.forEach { CategoryChip(it) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                if (current.entry.content.isNotBlank()) {
                    Text(current.entry.content, fontSize = 15.sp, color = sg.ink, lineHeight = 29.sp)
                }
            }
        }

        // 返回
        Box(
            Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(14.dp)
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(sg.card.copy(alpha = .92f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = sg.ink, modifier = Modifier.size(19.dp)) }

        // 底部操作
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(sg.card)
                    .clickable { onEdit(entryId) }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Edit, null, tint = sg.ink2, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.size(7.dp))
                    Text("编辑", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = sg.ink2)
                }
            }
            Box(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(sg.card)
                    .clickable { confirmDelete = true }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Delete, null, tint = sg.roseTx, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.size(7.dp))
                    Text("删除", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = sg.roseTx)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = sg.card,
            title = { Text("删除这条记录？", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = { Text("记录和它的照片会一起删除，无法找回。", fontSize = 13.5.sp, color = sg.ink2, lineHeight = 21.sp) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        graph.entryRepo.delete(current)
                        toast("已删除")
                        onBack()
                    }
                }) { Text("删除", color = sg.roseTx) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("先留着") } }
        )
    }
}

private fun Dates4Detail(iso: String): String =
    com.shiguangji.app.util.Dates.monthDayZh(iso) + " " + com.shiguangji.app.util.Dates.weekdayZh(iso)

private fun timeOf(epochMillis: Long): String =
    java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
