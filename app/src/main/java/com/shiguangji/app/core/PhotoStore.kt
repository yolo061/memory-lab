package com.shiguangji.app.core

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.util.UUID

/**
 * 照片存储规范（自 v1 冻结，后续版本保持不变）：
 *
 *   Android/data/com.shiguangji.app/files/Pictures/photos/<年>/<月>/<uuid>.<ext>
 *
 * - 照片在添加时复制进应用专属目录，与原图生命周期解耦；
 * - 数据库只存相对路径，备份恢复 = 目录整体搬家；
 * - 应用卸载时目录随系统一起清理，不留垃圾。
 */
class PhotoStore(private val context: Context) {

    private val baseDir: File
        get() = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir,
            "photos"
        )

    fun fileOf(relPath: String): File = File(baseDir, relPath.removePrefix("photos/"))

    /** 复制选定图片到归档目录，返回数据库存的相对路径。 */
    fun import(uri: Uri): String {
        val ext = extensionOf(uri)
        val now = java.time.LocalDate.now()
        val dir = File(baseDir, "%04d/%02d".format(now.year, now.monthValue)).apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("无法读取所选图片")
        return "photos/%04d/%02d/%s".format(now.year, now.monthValue, file.name)
    }

    fun delete(relPath: String) {
        runCatching { fileOf(relPath).delete() }
    }

    /** 设置页展示已用空间。 */
    fun totalSizeBytes(): Long = baseDir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    private fun extensionOf(uri: Uri): String {
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        return when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
    }
}
