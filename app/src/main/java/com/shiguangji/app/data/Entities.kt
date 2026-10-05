package com.shiguangji.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * 一条日常记录。date 使用 ISO 文本 "yyyy-MM-dd" 存储：
 * - 那年今日查询可直接用 substr 匹配月-日；
 * - 文本格式对人和备份文件都稳定可读，利于长期兼容。
 *
 * categoryId 为 v1 的遗留字段（单栏目），v2 起多栏目走 entry_categories 关联表；
 * 保留此列并同步写入第一个选中栏目，保证旧版本备份仍可读。
 */
@Entity(
    tableName = "entries",
    indices = [Index("date"), Index("categoryId")]
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val content: String,
    val categoryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** 记录 ↔ 栏目 多对多关联（v2 新增，支持一条记录挂多个栏目）。 */
@Entity(
    tableName = "entry_categories",
    primaryKeys = ["entryId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [Index("entryId"), Index("categoryId")]
)
data class EntryCategoryCrossRef(
    val entryId: Long,
    val categoryId: Long,
)

/**
 * 照片：只存相对路径（如 "photos/2026/10/uuid.jpg"），
 * 与应用专属目录 Android/data/<pkg>/files/Pictures/ 拼接成绝对路径。
 * 换机 / 备份恢复时只要目录整体迁移即可，数据库无需改动 —— 存储格式自 v1 起冻结。
 */
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId")]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val entryId: Long,
    val relPath: String,
    val caption: String = "",
    val sortIndex: Int = 0,
)

data class EntryWithPhotos(
    @Embedded val entry: EntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val photos: List<PhotoEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryCategoryCrossRef::class,
            parentColumn = "entryId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<CategoryEntity> = emptyList(),
)

/** 栏目。colorKey 取 "green|amber|blue|purple|rose|brand"，映射到当前主题的配色。 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val emoji: String,
    val colorKey: String = "green",
    val sortOrder: Int = 0,
)

/** 悄悄话：独立表，与普通记录完全隔离，配合 PIN/生物识别门禁。 */
@Entity(tableName = "whispers")
data class WhisperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)
