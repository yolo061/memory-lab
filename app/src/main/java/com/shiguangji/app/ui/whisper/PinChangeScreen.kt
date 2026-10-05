package com.shiguangji.app.ui.whisper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shiguangji.app.appGraph
import com.shiguangji.app.ui.rememberToast
import kotlinx.coroutines.launch

private enum class PinChangeStep { VERIFY_OLD, ENTER_NEW, CONFIRM_NEW }

/**
 * 修改悄悄话密码：先验证旧密码 → 输入新密码 → 再确认一遍。
 * 任意一步随时可以返回取消，原密码不受影响。
 */
@Composable
fun PinChangeScreen(onBack: () -> Unit) {
    val graph = LocalContext.current.appGraph
    val toast = rememberToast()
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(PinChangeStep.VERIFY_OLD) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var digits by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(WhisperNight.bg)) {
        when (step) {
            PinChangeStep.VERIFY_OLD -> PinShell(
                emoji = "🔑",
                title = "修改密码",
                subtitle = "先输入现在的 PIN 码验证身份",
                digits = digits,
                onDigit = { d ->
                    if (!busy && digits.length < 4) {
                        digits += d
                        if (digits.length == 4) {
                            busy = true
                            val pin = digits
                            scope.launch {
                                if (graph.pinManager.verify(pin)) {
                                    oldPin = pin
                                    step = PinChangeStep.ENTER_NEW
                                    digits = ""
                                } else {
                                    toast("旧密码不对，再试试")
                                    digits = ""
                                }
                                busy = false
                            }
                        }
                    }
                },
                onDelete = { if (digits.isNotEmpty()) digits = digits.dropLast(1) },
            )
            PinChangeStep.ENTER_NEW -> PinShell(
                emoji = "🔑",
                title = "修改密码",
                subtitle = "设置新的 4 位 PIN 码",
                digits = digits,
                onDigit = { d ->
                    if (digits.length < 4) {
                        digits += d
                        if (digits.length == 4) {
                            newPin = digits
                            step = PinChangeStep.CONFIRM_NEW
                            digits = ""
                        }
                    }
                },
                onDelete = { if (digits.isNotEmpty()) digits = digits.dropLast(1) },
            )
            PinChangeStep.CONFIRM_NEW -> PinShell(
                emoji = "🔑",
                title = "修改密码",
                subtitle = "再输入一遍新密码确认",
                digits = digits,
                onDigit = { d ->
                    if (!busy && digits.length < 4) {
                        digits += d
                        if (digits.length == 4) {
                            if (digits == newPin) {
                                busy = true
                                val pin = digits
                                scope.launch {
                                    graph.pinManager.setPin(pin)
                                    toast("密码已修改 ✓")
                                    onBack()
                                }
                            } else {
                                toast("两次输入不一致，重新设置")
                                step = PinChangeStep.ENTER_NEW
                                digits = ""
                            }
                        }
                    }
                },
                onDelete = { if (digits.isNotEmpty()) digits = digits.dropLast(1) },
            )
        }

        // 顶部返回：修改过程中随时退出，原密码不变
        Row(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .align(Alignment.TopStart)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = WhisperNight.title)
            }
            Spacer(Modifier.size(2.dp))
            Text(
                when (step) {
                    PinChangeStep.VERIFY_OLD -> "第 1 步 · 验证旧密码"
                    PinChangeStep.ENTER_NEW -> "第 2 步 · 输入新密码"
                    PinChangeStep.CONFIRM_NEW -> "第 3 步 · 确认新密码"
                },
                fontSize = 12.sp,
                color = WhisperNight.sub,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}
