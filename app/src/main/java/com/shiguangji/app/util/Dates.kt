package com.shiguangji.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 日期工具。记录内部统一使用 ISO "yyyy-MM-dd" 文本，展示时才转中文。 */
object Dates {

    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayIso(): String = LocalDate.now().toString()

    fun parseOrNull(iso: String): LocalDate? = runCatching { LocalDate.parse(iso, ISO) }.getOrNull()

    fun isoOf(date: LocalDate): String = date.format(ISO)

    fun yearOf(iso: String): Int = iso.substring(0, 4).toInt()

    /** 月-日片段（MM-dd），那年今日的匹配键。 */
    fun monthDayOf(iso: String): String = iso.substring(5, 10)

    fun weekdayZh(iso: String): String {
        val d = parseOrNull(iso) ?: return ""
        return when (d.dayOfWeek.value) {
            1 -> "星期一"; 2 -> "星期二"; 3 -> "星期三"
            4 -> "星期四"; 5 -> "星期五"; 6 -> "星期六"
            else -> "星期日"
        }
    }

    fun monthDayZh(iso: String): String {
        val d = parseOrNull(iso) ?: return iso
        return "${d.monthValue}月${d.dayOfMonth}日"
    }

    /** 与今天相比的相对日期描述。 */
    fun relativeDayZh(iso: String): String {
        val d = parseOrNull(iso) ?: return iso
        val today = LocalDate.now()
        return when (d) {
            today -> "今天"
            today.minusDays(1) -> "昨天"
            today.minusDays(2) -> "前天"
            else -> "${d.monthValue}月${d.dayOfMonth}日"
        }
    }

    /** 那年今日的年份标签：3 -> 「3 年前」。 */
    fun yearsAgoZh(year: Int, currentYear: Int): String {
        val diff = currentYear - year
        return when {
            diff <= 0 -> "今年"
            diff == 1 -> "去年"
            else -> "$diff 年前"
        }
    }
}
