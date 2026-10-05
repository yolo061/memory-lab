package com.shiguangji.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

/**
 * 数据库版本策略（对用户承诺「升级不丢数据」）：
 * 1. version 递增时，必须在本文件 MIGRATIONS 中补充对应的 Migration 对象；
 * 2. 刻意不调用 fallbackToDestructiveMigration —— 结构变更失败宁可报错，也不清空用户数据；
 * 3. exportSchema = true：每个版本的建表语句都会导出到 app/schemas/，
 *    供下一版编写迁移时比对与测试。
 */
@Database(
    entities = [
        EntryEntity::class, PhotoEntity::class, CategoryEntity::class,
        WhisperEntity::class, EntryCategoryCrossRef::class,
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao
    abstract fun photoDao(): PhotoDao
    abstract fun categoryDao(): CategoryDao
    abstract fun whisperDao(): WhisperDao
    abstract fun entryCategoryDao(): EntryCategoryDao

    companion object {
        const val NAME = "shiguangji.db"
        const val SCHEMA_VERSION = 2

        /** 历史迁移登记处：每一次结构变更都在这里追加，旧迁移永不删除。 */
        val MIGRATIONS: Array<Migration> = arrayOf(
            /**
             * v1 → v2：栏目从「单选列」升级为「多对多关联表」。
             * 旧记录的单栏目自动转换成一条关联，数据零丢失；
             * entries.categoryId 遗留列原样保留，不重建表。
             */
            object : Migration(1, 2) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `entry_categories` (`entryId` INTEGER NOT NULL, `categoryId` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`entryId`, `categoryId`), " +
                            "FOREIGN KEY(`entryId`) REFERENCES `entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                            "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_entry_categories_entryId` ON `entry_categories` (`entryId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_entry_categories_categoryId` ON `entry_categories` (`categoryId`)")
                    db.execSQL(
                        "INSERT INTO entry_categories (entryId, categoryId) " +
                            "SELECT id, categoryId FROM entries WHERE categoryId IS NOT NULL"
                    )
                }
            }
        )

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
