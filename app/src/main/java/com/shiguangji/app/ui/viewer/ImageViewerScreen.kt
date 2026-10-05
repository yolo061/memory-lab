package com.shiguangji.app.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shiguangji.app.appGraph

/**
 * 全屏照片查看器：黑底沉入、左右翻页、双指缩放 + 单指拖动、双击放大/还原。
 * 从详情页任意一张照片点进来，翻到哪张看哪张。
 */
@Composable
fun ImageViewerScreen(entryId: Long, initialIndex: Int, onBack: () -> Unit) {
    val graph = LocalContext.current.appGraph
    val item by graph.entryRepo.byId(entryId).collectAsState(initial = null)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val photos = item?.photos?.sortedBy { it.sortIndex }
        if (photos != null) {
            if (photos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("这条记录没有照片", fontSize = 13.sp, color = Color(0xFF9C948A))
                }
            } else {
                ViewerPager(
                    relPaths = photos.map { it.relPath },
                    captions = photos.map { it.caption },
                    initialIndex = initialIndex.coerceIn(0, photos.lastIndex),
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun ViewerPager(
    relPaths: List<String>,
    captions: List<String>,
    initialIndex: Int,
    onBack: () -> Unit,
) {
    val graph = LocalContext.current.appGraph
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { relPaths.size })
    var page by remember { mutableIntStateOf(initialIndex) }

    // 缩放/位移状态：翻页时归位
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var canvas by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(pagerState.currentPage) {
        page = pagerState.currentPage
        scale = 1f
        offset = Offset.Zero
    }

    fun clampOffset(o: Offset, s: Float): Offset {
        val maxX = canvas.width * (s - 1f) / 2f
        val maxY = canvas.height * (s - 1f) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            // 放大后关闭整页滑动，把横向手势让给拖动看图
            userScrollEnabled = scale <= 1.01f,
            modifier = Modifier.fillMaxSize()
        ) { p ->
            AsyncImage(
                model = graph.photoStore.fileOf(relPaths[p]),
                contentDescription = "照片 ${p + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .onSizeChanged { canvas = it }
                    .graphicsLayer(
                        scaleX = scale, scaleY = scale,
                        translationX = offset.x, translationY = offset.y
                    )
                    // 自定义手势：双指捏合缩放；放大后单指拖动；
                    // 未放大时单指不消费事件，横向滑动交还给翻页
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break
                                if (pressed.size > 1 || scale > 1.01f) {
                                    event.changes.forEach {
                                        if (it.positionChanged()) it.consume()
                                    }
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    val ns = (scale * zoomChange).coerceIn(1f, 5f)
                                    offset = if (ns > 1f) clampOffset(offset + panChange, ns) else Offset.Zero
                                    scale = ns
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { pos ->
                            val target = if (scale > 1.5f) 1f else 2.5f
                            if (target == 1f) {
                                offset = Offset.Zero
                            } else {
                                val c = Offset(canvas.width / 2f, canvas.height / 2f)
                                offset = clampOffset(offset + (pos - c) * (scale - target), target)
                            }
                            scale = target
                        })
                    }
            )
        }

        // 顶部：返回 + 页码
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = Color.White, modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.weight(1f))
            if (relPaths.size > 1) {
                Text(
                    "${page + 1} / ${relPaths.size}",
                    fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                )
            }
            Spacer(Modifier.width(38.dp))
        }

        // 底部：照片说明（当前页有才显示）
        val caption = captions.getOrNull(page)?.takeIf { it.isNotBlank() }
        if (caption != null) {
            Text(
                caption,
                fontSize = 13.sp, color = Color(0xFFEDE6DC), lineHeight = 20.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }
    }
}
