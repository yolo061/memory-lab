package com.shiguangji.app.ui.cats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shiguangji.app.appGraph
import com.shiguangji.app.data.CategoryEntity
import com.shiguangji.app.data.EntryWithPhotos
import com.shiguangji.app.ui.home.EntryCard
import com.shiguangji.app.ui.LocalSg
import com.shiguangji.app.ui.PageTitle
import com.shiguangji.app.ui.rememberToast
import kotlinx.coroutines.launch

private val COLOR_KEYS = listOf("green", "amber", "blue", "purple", "rose", "brand")
private val EMOJIS = listOf("🌿", "🍜", "🧳", "☁️", "📚", "🐱", "🌸", "☕", "🍎", "🎸", "🏠", "🌧️")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen() {
    val graph = LocalContext.current.appGraph
    val sg = LocalSg.current
    val toast = rememberToast()
    val scope = rememberCoroutineScope()
    val categories by graph.categoryRepo.all.collectAsState(initial = emptyList())
    val allEntries by graph.entryRepo.all.collectAsState(initial = emptyList())

    var sheetFor by remember { mutableStateOf<CategoryEntity?>(null) }
    var sheetOpen by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<CategoryEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    val counts = remember(allEntries) {
        val m = mutableMapOf<Long, Int>()
        allEntries.forEach { e ->
            e.categories.forEach { c -> m[c.id] = (m[c.id] ?: 0) + 1 }
        }
        m
    }

    if (viewing != null) {
        val cat = viewing!!
        val catEntries: List<EntryWithPhotos> = allEntries.filter { e ->
            e.categories.any { it.id == cat.id }
        }
        Column(Modifier.fillMaxSize().background(sg.paper).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewing = null }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = sg.ink)
                }
                Text("${cat.emoji} ${cat.name}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = sg.ink)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { sheetFor = cat; sheetOpen = true }) {
                    Icon(Icons.Rounded.Edit, "编辑栏目", tint = sg.ink2)
                }
                IconButton(onClick = { confirmDelete = cat }) {
                    Icon(Icons.Rounded.Delete, "删除栏目", tint = sg.roseTx)
                }
            }
            if (catEntries.isEmpty()) {
                com.shiguangji.app.ui.EmptyState("🌿", "这个栏目还没有记录\n写记录时选它就好")
            } else {
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    bottom = 110.dp + WindowInsets.navigationBars
                        .asPaddingValues().calculateBottomPadding()
                )) {
                    listItems(catEntries, key = { it.entry.id }) { item ->
                        EntryCard(
                            item = item,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                            onClick = {},
                        )
                    }
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().background(sg.paper).statusBarsPadding()) {
            Column(Modifier.padding(top = 14.dp)) {
                PageTitle("栏目", "把日子分门别类地收好")
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 22.dp, end = 22.dp, top = 22.dp,
                    bottom = 24.dp + WindowInsets.navigationBars
                        .asPaddingValues().calculateBottomPadding()
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categories, key = { it.id }) { cat ->
                    val (bg, on) = sg.category(cat.colorKey)
                    Column(
                        Modifier
                            .aspectRatio(1.25f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(bg)
                            .clickable { viewing = cat }
                            .padding(18.dp)
                    ) {
                        Text(cat.emoji, fontSize = 32.sp)
                        Spacer(Modifier.weight(1f))
                        Text(cat.name, fontSize = 16.5.sp, fontWeight = FontWeight.Bold, color = on)
                        Spacer(Modifier.height(3.dp))
                        Text("${counts[cat.id] ?: 0} 篇", fontSize = 11.5.sp, color = on.copy(alpha = .75f))
                    }
                }
                item {
                    Column(
                        Modifier
                            .aspectRatio(1.25f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(sg.card)
                            .border(1.8.dp, sg.brandSoft, RoundedCornerShape(24.dp))
                            .clickable { sheetFor = null; sheetOpen = true },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.Add, "新建栏目", tint = sg.brand, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("新建栏目", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = sg.brand)
                    }
                }
            }
            // 说明文字独立于网格，整行居中放在页面底部，不再和「新建栏目」并排成两个框
            Text(
                "删除栏目时，里面的记录不会被删掉，只是回到「未分类」。",
                fontSize = 11.5.sp, color = sg.ink3, lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                    .padding(
                        start = 40.dp, end = 40.dp,
                        bottom = 18.dp + WindowInsets.navigationBars
                            .asPaddingValues().calculateBottomPadding()
                    )
            )
        }
    }

    if (sheetOpen) {
        val editing = sheetFor
        var name by remember(editing?.id) { mutableStateOf(editing?.name ?: "") }
        var emoji by remember(editing?.id) { mutableStateOf(editing?.emoji ?: "🌿") }
        var colorKey by remember(editing?.id) { mutableStateOf(editing?.colorKey ?: "green") }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            containerColor = sg.paper,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 22.dp)
                    // 键盘弹出时内容整体上浮；键盘收起时避让手势条
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (editing == null) "新建栏目" else "编辑栏目", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sg.ink)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        if (name.isBlank()) { toast("先给栏目起个名字"); return@TextButton }
                        scope.launch {
                            graph.categoryRepo.save(editing?.id, name.trim(), emoji, colorKey)
                            sheetOpen = false
                            toast("已保存 ✓")
                        }
                    }) { Text("完成", color = sg.brand, fontWeight = FontWeight.Bold) }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    placeholder = { Text("栏目名字，比如「运动」「观影」", fontSize = 14.sp, color = sg.ink3) },
                    singleLine = true, shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = sg.card, unfocusedContainerColor = sg.card,
                        focusedBorderColor = sg.brand, unfocusedBorderColor = sg.line, cursorColor = sg.brand,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("选个图标", fontSize = 11.5.sp, letterSpacing = 2.sp, color = sg.ink3,
                    modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
                EMOJIS.chunked(6).forEach { rowEmojis ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowEmojis.forEach { e ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(if (emoji == e) sg.brandSoft else sg.card)
                                    .border(1.5.dp, if (emoji == e) sg.brand else sg.line, RoundedCornerShape(13.dp))
                                    .clickable { emoji = e },
                                contentAlignment = Alignment.Center
                            ) { Text(e, fontSize = 21.sp) }
                        }
                        repeat(6 - rowEmojis.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Text("选个颜色", fontSize = 11.5.sp, letterSpacing = 2.sp, color = sg.ink3,
                    modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    COLOR_KEYS.forEach { key ->
                        val (bg, on) = sg.category(key)
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (key == "brand") sg.brand else bg)
                                .border(
                                    if (colorKey == key) 3.dp else 0.dp,
                                    if (colorKey == key) on else androidx.compose.ui.graphics.Color.Transparent,
                                    CircleShape
                                )
                                .clickable { colorKey = key }
                        )
                    }
                }
            }
        }
    }

    confirmDelete?.let { cat ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = sg.card,
            title = { Text("删除「${cat.name}」？", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = { Text("栏目里的 ${counts[cat.id] ?: 0} 篇记录不会被删除，它们会回到「未分类」。", fontSize = 13.5.sp, color = sg.ink2, lineHeight = 21.sp) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        graph.categoryRepo.delete(cat)
                        confirmDelete = null
                        viewing = null
                        toast("栏目已删除，记录完好")
                    }
                }) { Text("删除栏目", color = sg.roseTx) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } }
        )
    }
}
