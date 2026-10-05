package com.shiguangji.app

import com.shiguangji.app.core.ThemeColors
import com.shiguangji.app.core.ThemePackParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 主题包兼容性是产品承诺，这里锁定核心行为：
 * 未知字段忽略、缺失字段回退、rgba() 兼容、坏文件拒绝。
 */
class ThemePackParserTest {

    private fun fullJson(): String {
        val colors = JSONObject()
            .put("paper", "#FAF5EE")
            .put("paper2", "#F3EADD")
            .put("card", "#FFFFFF")
            .put("ink", "#33291F")
            .put("ink2", "#75675A")
            .put("ink3", "#A6988A")
            .put("brand", "#C4663F")
            .put("brand2", "#D97B54")
            .put("brandSoft", "#F8E7DC")
            .put("brandSoft2", "#FBF1E9")
            .put("line", "#14503A26")
            .put("gesture", "#5A3C2338")
            .put("greenSoft", "#ECF1E4").put("greenTx", "#5C7A50")
            .put("amberSoft", "#FAEDDA").put("amberTx", "#B27327")
            .put("blueSoft", "#E6EFF4").put("blueTx", "#4F7B95")
            .put("purpleSoft", "#EFEAF5").put("purpleTx", "#7A6398")
            .put("roseSoft", "#F8E8E5").put("roseTx", "#A85248")
            .put("green", "#7A9B6D").put("amber", "#DE9A52").put("blue", "#6D9BB5")
            .put("purple", "#9C87B8").put("rose", "#C4756B")
            .put("grad1", "#E8926A").put("grad2", "#C4663F")
        return JSONObject()
            .put("format", "shiguangji-theme-pack")
            .put("version", 1)
            .put("id", "t-test")
            .put("name", "测试主题")
            .put("author", "tester")
            .put("colors", colors)
            .toString()
    }

    @Test
    fun `解析完整主题包`() {
        val pack = ThemePackParser.parse(fullJson(), fileName = "test.json")
        assertEquals("测试主题", pack.name)
        assertEquals("t-test", pack.id)
        assertEquals(1, pack.version)
        assertFalse(pack.isDark)
        assertEquals("test.json", pack.fileName)
        assertEquals("#FFFAF5EE", pack.colors.paper)
    }

    @Test
    fun `未知字段被忽略_未来格式向后兼容`() {
        val future = JSONObject(fullJson())
            .put("v2NewFeature", JSONObject().put("anything", true))
            .put("futureColors", "xxx")
        val pack = ThemePackParser.parse(future.toString())
        assertEquals("测试主题", pack.name) // 旧版本 App 依然能解析新格式文件
    }

    @Test
    fun `缺失颜色字段回退默认值`() {
        val minimal = """{"format":"shiguangji-theme-pack","name":"极简主题","colors":{"brand":"#123456"}}"""
        val pack = ThemePackParser.parse(minimal)
        assertEquals("#FF123456", pack.colors.brand)
        assertEquals(ThemeColors.WARM.paper, pack.colors.paper) // 缺失 → 暖阳默认
    }

    @Test
    fun `rgba写法被支持`() {
        val pack = ThemePackParser.parse(
            """{"format":"shiguangji-theme-pack","colors":{"brandSoft":"rgba(217,160,91,0.16)"}}"""
        )
        // 0.16 × 255 = 40.8 → 40 = 0x28
        assertEquals("#28D9A05B", pack.colors.brandSoft)
    }

    @Test
    fun `坏JSON被拒绝`() {
        try {
            ThemePackParser.parse("{not-json")
            throw AssertionError("应当抛出异常")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("JSON"))
        }
    }

    @Test
    fun `格式不匹配被拒绝`() {
        try {
            ThemePackParser.parse("""{"format":"other-app-theme","name":"x"}""")
            throw AssertionError("应当抛出异常")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("format"))
        }
    }

    @Test
    fun `深浅色自动推断`() {
        val dark = ThemePackParser.parse(
            """{"format":"shiguangji-theme-pack","name":"夜","colors":{"paper":"#16131D"}}"""
        )
        assertTrue(dark.isDark)
    }

    @Test
    fun `颜色归一化`() {
        assertEquals("#FFAABBCC", ThemePackParser.normalize("#AABBCC", "#FF000000"))
        assertEquals("#80AABBCC", ThemePackParser.normalize("#80AABBCC", "#FF000000"))
        assertEquals("#FF112233", ThemePackParser.normalize("rgb(17,34,51)", "#FF000000"))
        assertEquals("#FFF00000", ThemePackParser.normalize("rgba(240,0,0,1.0)", "#FF000000"))
        assertEquals("#FF000000", ThemePackParser.normalize("oops", "#FF000000"))
        assertEquals("#FF000000", ThemePackParser.normalize("", "#FF000000"))
    }
}
