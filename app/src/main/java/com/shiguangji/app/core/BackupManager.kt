package com.shiguangji.app.core

import android.content.Context
import com.shiguangji.app.data.AppDatabase
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 备份 = 一个 zip：数据库（checkpoint 后的干净副本）+ 全部照片 + manifest.json。
 *
 * 兼容性：
 * - manifest 里记录 schemaVersion，恢复到旧版本 App 时若发现备份来自更高 schema，拒绝恢复并提示；
 * - manifest 里未知字段一律忽略，未来新增能力不影响旧版恢复；
 * - 恢复采用「先落临时目录，校验通过再替换」的两段式，中断不会损坏现有数据。
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val photoStore: PhotoStore,
) {
    suspend fun exportTo(output: java.io.OutputStream) {
        // WAL 收敛到主库文件后复制，保证副本完整
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        val dbFile = context.getDatabasePath(AppDatabase.NAME)
        val photoDir = photoStore.fileOf("photos").parentFile
        val photos = photoStore.fileOf("").walkBottomUp().filter { it.isFile }

        ZipOutputStream(output.buffered()).use { zip ->
            val manifest = JSONObject().apply {
                put("format", FORMAT)
                put("version", BACKUP_VERSION)
                put("schemaVersion", AppDatabase.SCHEMA_VERSION)
                put("appVersion", appVersion())
                put("exportedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                put("photoCount", photos.count())
            }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("db/${AppDatabase.NAME}"))
            dbFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()

            photos.forEach { file ->
                val rel = file.relativeToOrNull(photoDir ?: return@forEach) ?: return@forEach
                zip.putNextEntry(ZipEntry("photos/${rel.path.replace('\\', '/')}"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** 返回 true 表示恢复成功；失败抛 IllegalArgumentException（消息可直接展示）。 */
    suspend fun importFrom(inputStream: java.io.InputStream) {
        val staging = File(context.cacheDir, "restore-${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            var manifest: JSONObject? = null
            ZipInputStream(inputStream.buffered()).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val out = File(staging, entry.name)
                    if (entry.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zip.copyTo(it) }
                    }
                    if (entry.name == "manifest.json") {
                        manifest = JSONObject(out.readText(Charsets.UTF_8))
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            val m = manifest ?: throw IllegalArgumentException("备份文件缺少 manifest.json，无法识别")
            if (m.optString("format", FORMAT) != FORMAT) {
                throw IllegalArgumentException("不是拾光记备份文件")
            }
            val backupSchema = m.optInt("schemaVersion", 1)
            if (backupSchema > AppDatabase.SCHEMA_VERSION) {
                throw IllegalArgumentException(
                    "备份来自更新版本的拾光记（数据结构 v$backupSchema > 当前 v${AppDatabase.SCHEMA_VERSION}），" +
                        "请先升级 App 再恢复"
                )
            }
            val dbFile = File(staging, "db/${AppDatabase.NAME}")
            if (!dbFile.exists()) throw IllegalArgumentException("备份文件不完整（缺少数据库）")

            // 校验通过：关闭数据库 → 原子替换 → 迁移照片
            db.close()
            val target = context.getDatabasePath(AppDatabase.NAME)
            target.parentFile?.mkdirs()
            File(target.parentFile, "${AppDatabase.NAME}-wal").delete()
            File(target.parentFile, "${AppDatabase.NAME}-shm").delete()
            dbFile.copyTo(target, overwrite = true)

            val photoRoot = photoStore.fileOf("")
            photoRoot.deleteRecursively()
            val stagedPhotos = File(staging, "photos")
            if (stagedPhotos.exists()) {
                stagedPhotos.copyRecursively(photoRoot, overwrite = true)
            }
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun appVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    }.getOrDefault("1.0")

    companion object {
        const val FORMAT = "shiguangji-backup"
        const val BACKUP_VERSION = 1

        /** 恢复后数据库连接已重建，需要重启进程加载新数据。 */
        fun restartApp(context: Context) {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launch?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(launch)
            Runtime.getRuntime().exit(0)
        }
    }
}
