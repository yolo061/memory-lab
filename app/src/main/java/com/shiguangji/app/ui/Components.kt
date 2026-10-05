package com.shiguangji.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shiguangji.app.core.PhotoStore
import com.shiguangji.app.data.CategoryEntity
import android.widget.Toast

@Composable
fun rememberToast(): (String) -> Unit {
    val ctx = LocalContext.current
    return remember(ctx) {
        { msg: String -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
    }
}

/** 大圆角卡片，圆角与描边跟随主题包。 */
@Composable
fun SgCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    content: @Composable () -> Unit,
) {
    val sg = LocalSg.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(sg.card)
            .border(1.dp, sg.line, RoundedCornerShape(radius))
    ) { content() }
}

/** 栏目彩色标签。 */
@Composable
fun CategoryChip(category: CategoryEntity?, modifier: Modifier = Modifier) {
    val sg = LocalSg.current
    if (category == null) return
    val (bg, on) = sg.category(category.colorKey)
    Text(
        text = "${category.emoji} ${category.name}",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = on,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 11.dp, vertical = 4.dp)
    )
}

/** 日期分组小标题：文字 + 延伸细线，杂志感。 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    val sg = LocalSg.current
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = sg.ink)
        Spacer(Modifier.size(10.dp))
        if (trailing != null) trailing()
        Box(Modifier.weight(1f).height(1.dp).background(sg.line))
    }
}

/** 照片缩略图（占位 loading 底色取 paper2）。 */
@Composable
fun PhotoThumb(
    relPath: String,
    modifier: Modifier = Modifier,
    photoStore: PhotoStore,
) {
    val sg = LocalSg.current
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(sg.paper2)) {
        AsyncImage(
            model = photoStore.fileOf(relPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
    }
}

/** 温暖的空状态插画：渐变圆 + emoji + 一句话。 */
@Composable
fun EmptyState(emoji: String, text: String, modifier: Modifier = Modifier) {
    val sg = LocalSg.current
    Column(modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(sg.brandSoft, sg.brandSoft2))),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 38.sp) }
        Spacer(Modifier.height(14.dp))
        Text(text, fontSize = 13.sp, color = sg.ink3, lineHeight = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** 两行预览正文。 */
@Composable
fun EntryPreview(text: String, modifier: Modifier = Modifier) {
    val sg = LocalSg.current
    Text(
        text, fontSize = 14.sp, color = sg.ink, lineHeight = 23.sp,
        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = modifier
    )
}

@Composable
fun StatColumn(value: String, label: String, modifier: Modifier = Modifier) {
    val sg = LocalSg.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = sg.ink)
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 11.sp, color = sg.ink3, letterSpacing = 1.sp)
    }
}

/** 页面大标题。 */
@Composable
fun PageTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    val sg = LocalSg.current
    Column(modifier.padding(horizontal = 24.dp)) {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = sg.ink, letterSpacing = 2.sp)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 12.5.sp, color = sg.ink3, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun FlowSpacer(height: Dp = 12.dp) {
    Spacer(Modifier.fillMaxWidth().height(height))
}
