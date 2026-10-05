package com.shiguangji.app.core

import org.json.JSONObject

/**
 * 主题包：一份 JSON 即一套完整配色方案，可随文件导入导出、跨设备移植。
 *
 * 兼容性承诺（这是主题包格式的核心设计）：
 * - 解析只读取已知字段，**未知字段一律忽略** —— 旧版本 App 打开未来格式不报错；
 * - 缺失字段回退到「拾光暖阳」默认值 —— 精简的主题包也能用；
 * - version 字段递增只代表新增能力，v1 的字段名永不改变语义。
 */
data class ThemePack(
    val id: String,
    val name: String,
    val author: String,
    val version: Int,
    val isDark: Boolean,
    val colors: ThemeColors,
    val rawJson: String,
    /** 导入主题包对应的文件名；内置主题为 null，不可删除。 */
    val fileName: String? = null,
)

data class ThemeColors(
    val paper: String, val paper2: String, val card: String,
    val ink: String, val ink2: String, val ink3: String,
    val brand: String, val brand2: String, val brandSoft: String, val brandSoft2: String,
    val line: String, val gesture: String,
    val greenSoft: String, val greenTx: String,
    val amberSoft: String, val amberTx: String,
    val blueSoft: String, val blueTx: String,
    val purpleSoft: String, val purpleTx: String,
    val roseSoft: String, val roseTx: String,
    val green: String, val amber: String, val blue: String, val purple: String, val rose: String,
    val grad1: String, val grad2: String,
) {
    companion object {
        /** 拾光暖阳配色 —— 所有主题包缺失字段的回退基准。 */
        val WARM = ThemeColors(
            paper = "#FFFAF5EE", paper2 = "#FFF3EADD", card = "#FFFFFFFF",
            ink = "#FF33291F", ink2 = "#FF75675A", ink3 = "#FFA6988A",
            brand = "#FFC4663F", brand2 = "#FFD97B54", brandSoft = "#FFF8E7DC", brandSoft2 = "#FFFBF1E9",
            line = "#14503A26", gesture = "#5A3C2338",
            greenSoft = "#FFECF1E4", greenTx = "#FF5C7A50",
            amberSoft = "#FFFAEDDA", amberTx = "#FFB27327",
            blueSoft = "#FFE6EFF4", blueTx = "#FF4F7B95",
            purpleSoft = "#FFEFEAF5", purpleTx = "#FF7A6398",
            roseSoft = "#FFF8E8E5", roseTx = "#FFA85248",
            green = "#FF7A9B6D", amber = "#FFDE9A52", blue = "#FF6D9BB5", purple = "#FF9C87B8", rose = "#FFC4756B",
            grad1 = "#FFE8926A", grad2 = "#FFC4663F",
        )
    }
}

object ThemePackParser {

    const val FORMAT = "shiguangji-theme-pack"

    /** 解析失败抛 IllegalArgumentException（消息面向用户展示）。 */
    fun parse(json: String, fileName: String? = null, fallbackId: String? = null): ThemePack {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("不是有效的主题包文件（JSON 解析失败）")
        }
        val format = root.optString("format", FORMAT)
        if (format != FORMAT) throw IllegalArgumentException("不是拾光记主题包（format 不匹配）")

        val defaults = ThemeColors.WARM
        val colorsJson = root.optJSONObject("colors") ?: JSONObject()
        fun c(key: String, fallback: String): String =
            normalize(colorsJson.optString(key, ""), fallback)

        val colors = ThemeColors(
            paper = c("paper", defaults.paper), paper2 = c("paper2", defaults.paper2), card = c("card", defaults.card),
            ink = c("ink", defaults.ink), ink2 = c("ink2", defaults.ink2), ink3 = c("ink3", defaults.ink3),
            brand = c("brand", defaults.brand), brand2 = c("brand2", defaults.brand2),
            brandSoft = c("brandSoft", defaults.brandSoft), brandSoft2 = c("brandSoft2", defaults.brandSoft2),
            line = c("line", defaults.line), gesture = c("gesture", defaults.gesture),
            greenSoft = c("greenSoft", defaults.greenSoft), greenTx = c("greenTx", defaults.greenTx),
            amberSoft = c("amberSoft", defaults.amberSoft), amberTx = c("amberTx", defaults.amberTx),
            blueSoft = c("blueSoft", defaults.blueSoft), blueTx = c("blueTx", defaults.blueTx),
            purpleSoft = c("purpleSoft", defaults.purpleSoft), purpleTx = c("purpleTx", defaults.purpleTx),
            roseSoft = c("roseSoft", defaults.roseSoft), roseTx = c("roseTx", defaults.roseTx),
            green = c("green", defaults.green), amber = c("amber", defaults.amber),
            blue = c("blue", defaults.blue), purple = c("purple", defaults.purple), rose = c("rose", defaults.rose),
            grad1 = c("grad1", defaults.grad1), grad2 = c("grad2", defaults.grad2),
        )
        val name = root.optString("name", "未命名主题").ifBlank { "未命名主题" }
        val id = root.optString("id", fallbackId ?: ("import-" + name.hashCode().toUInt().toString()))
        return ThemePack(
            id = id,
            name = name,
            author = root.optString("author", "未知作者"),
            version = root.optInt("version", 1),
            isDark = root.optBoolean("dark", isColorDark(colors.paper)),
            colors = colors,
            rawJson = json,
            fileName = fileName,
        )
    }

    fun serialize(pack: ThemePack): String = pack.rawJson

    /** 支持 #RRGGBB / #AARRGGBB / rgba(r,g,b,a)；无法解析或为空时回退默认值。 */
    fun normalize(input: String, fallback: String): String {
        val s = input.trim()
        if (s.isEmpty()) return fallback
        if (s.startsWith("#")) {
            val v = when (s.length) {
                7 -> "#FF" + s.substring(1)
                9 -> s
                else -> return fallback
            }
            return if (isHex(v)) v else fallback
        }
        if (s.startsWith("rgba") || s.startsWith("rgb")) {
            val nums = s.substringAfter('(').substringBefore(')').split(',')
            val rgb = nums.take(3).mapNotNull { it.trim().toIntOrNull() }
            if (rgb.size == 3 && rgb.all { it in 0..255 }) {
                val alpha = if (nums.size >= 4) {
                    (nums[3].trim().toDoubleOrNull()?.coerceIn(0.0, 1.0)?.times(255))?.toInt() ?: 255
                } else 255
                return String.format("#%02X%02X%02X%02X", alpha, rgb[0], rgb[1], rgb[2])
            }
            return fallback
        }
        return fallback
    }

    /** 按亮度判断颜色深浅（用于 dark 标记缺省时推断状态栏图标颜色）。 */
    fun isColorDark(argb: String): Boolean {
        if (!argb.startsWith("#") || argb.length < 9) return false
        return try {
            val r = argb.substring(3, 5).toInt(16)
            val g = argb.substring(5, 7).toInt(16)
            val b = argb.substring(7, 9).toInt(16)
            (0.299 * r + 0.587 * g + 0.114 * b) < 128.0
        } catch (e: Exception) {
            false
        }
    }

    private fun isHex(s: String): Boolean =
        s.length == 9 && s.drop(1).all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }
}
