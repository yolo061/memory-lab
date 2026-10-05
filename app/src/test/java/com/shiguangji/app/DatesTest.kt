package com.shiguangji.app

import com.shiguangji.app.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Test

class DatesTest {

    @Test
    fun `月日匹配键`() {
        assertEquals("10-04", Dates.monthDayOf("2023-10-04"))
        assertEquals("02-29", Dates.monthDayOf("2024-02-29"))
    }

    @Test
    fun `年份提取`() {
        assertEquals(2023, Dates.yearOf("2023-10-04"))
    }

    @Test
    fun `年份倒叙标签`() {
        assertEquals("今年", Dates.yearsAgoZh(2026, 2026))
        assertEquals("去年", Dates.yearsAgoZh(2025, 2026))
        assertEquals("3 年前", Dates.yearsAgoZh(2023, 2026))
    }

    @Test
    fun `星期与中文日期`() {
        assertEquals("星期日", Dates.weekdayZh("2026-10-04"))
        assertEquals("10月4日", Dates.monthDayZh("2026-10-04"))
        assertEquals("2月29日", Dates.monthDayZh("2024-02-29"))
    }

    @Test
    fun `相对日期`() {
        assertEquals("今天", Dates.relativeDayZh(Dates.todayIso()))
        assertEquals("昨天", Dates.relativeDayZh(java.time.LocalDate.now().minusDays(1).toString()))
    }

    @Test
    fun `ISO往返一致`() {
        val today = Dates.todayIso()
        assertEquals(today, Dates.isoOf(Dates.parseOrNull(today)!!))
    }
}
