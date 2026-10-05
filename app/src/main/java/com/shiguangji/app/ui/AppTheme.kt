package com.shiguangji.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.shiguangji.app.core.ThemePack

/** 主题包在 Compose 里的投影：页面从 LocalSg 取语义色，与原型一一对应。 */
data class SgColors(
    val pack: ThemePack,
    val isDark: Boolean,
    val paper: Color, val paper2: Color, val card: Color,
    val ink: Color, val ink2: Color, val ink3: Color,
    val brand: Color, val brand2: Color, val brandSoft: Color, val brandSoft2: Color,
    val line: Color, val gesture: Color,
    val greenSoft: Color, val greenTx: Color,
    val amberSoft: Color, val amberTx: Color,
    val blueSoft: Color, val blueTx: Color,
    val purpleSoft: Color, val purpleTx: Color,
    val roseSoft: Color, val roseTx: Color,
    val green: Color, val amber: Color, val blue: Color, val purple: Color, val rose: Color,
    val grad1: Color, val grad2: Color,
) {
    /** 栏目 colorKey -> (底色, 文字色)。未知 key 回到品牌色。 */
    fun category(key: String): Pair<Color, Color> = when (key) {
        "green" -> greenSoft to greenTx
        "amber" -> amberSoft to amberTx
        "blue" -> blueSoft to blueTx
        "purple" -> purpleSoft to purpleTx
        "rose" -> roseSoft to roseTx
        else -> brandSoft to brand
    }

    companion object {
        fun from(pack: ThemePack): SgColors {
            val c = pack.colors
            return SgColors(
                pack = pack, isDark = pack.isDark,
                paper = c(c.paper), paper2 = c(c.paper2), card = c(c.card),
                ink = c(c.ink), ink2 = c(c.ink2), ink3 = c(c.ink3),
                brand = c(c.brand), brand2 = c(c.brand2),
                brandSoft = c(c.brandSoft), brandSoft2 = c(c.brandSoft2),
                line = c(c.line), gesture = c(c.gesture),
                greenSoft = c(c.greenSoft), greenTx = c(c.greenTx),
                amberSoft = c(c.amberSoft), amberTx = c(c.amberTx),
                blueSoft = c(c.blueSoft), blueTx = c(c.blueTx),
                purpleSoft = c(c.purpleSoft), purpleTx = c(c.purpleTx),
                roseSoft = c(c.roseSoft), roseTx = c(c.roseTx),
                green = c(c.green), amber = c(c.amber), blue = c(c.blue), purple = c(c.purple), rose = c(c.rose),
                grad1 = c(c.grad1), grad2 = c(c.grad2),
            )
        }

        /** "#AARRGGBB" → Compose Color；异常时回退暖纸色，绝不让主题包搞崩 App。 */
        private fun c(argb: String): Color = try {
            Color(argb.removePrefix("#").toLong(16).toInt())
        } catch (e: Exception) {
            Color(0xFFFAF5EE)
        }
    }
}

val LocalSg = staticCompositionLocalOf<SgColors> { error("SgColors not provided") }

@Composable
fun rememberSgColors(pack: ThemePack): SgColors = remember(pack) { SgColors.from(pack) }

/**
 * 主题包 → Material3 配色。
 * 「跟随系统取色」开启时（Android 12+），品牌色由系统壁纸取色接管，
 * 但栏目的彩色语义、渐变等仍来自主题包，保持个性化。
 */
@Composable
fun SgTheme(
    pack: ThemePack,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val sg = rememberSgColors(pack)
    val context = LocalContext.current
    val scheme: ColorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= 31) {
        if (sg.isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val base = if (sg.isDark) darkColorScheme() else lightColorScheme()
        base.copy(
            primary = sg.brand,
            onPrimary = Color.White,
            primaryContainer = sg.brandSoft,
            onPrimaryContainer = sg.brand,
            secondary = sg.brand2,
            background = sg.paper,
            onBackground = sg.ink,
            surface = sg.card,
            onSurface = sg.ink,
            surfaceVariant = sg.paper2,
            onSurfaceVariant = sg.ink2,
            outline = sg.ink3,
            outlineVariant = sg.line,
        )
    }
    CompositionLocalProvider(LocalSg provides sg) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
