package com.shiguangji.app.data

import com.shiguangji.app.core.PhotoStore
import kotlinx.coroutines.flow.Flow

class EntryRepository(
    private val entryDao: EntryDao,
    private val photoDao: PhotoDao,
    private val linkDao: EntryCategoryDao,
    private val photoStore: PhotoStore,
) {
    val all: Flow<List<EntryWithPhotos>> = entryDao.observeAll()
    fun byCategory(categoryId: Long): Flow<List<EntryWithPhotos>> = entryDao.observeByCategory(categoryId)
    fun byId(id: Long): Flow<EntryWithPhotos?> = entryDao.observeById(id)
    fun search(q: String): Flow<List<EntryWithPhotos>> = entryDao.search(q.trim())
    fun pastOnMonthDay(monthDay: String, beforeYear: Int): Flow<List<EntryWithPhotos>> =
        entryDao.pastOnMonthDay(monthDay, beforeYear)
    fun countPastOnMonthDay(monthDay: String, beforeYear: Int): Flow<Int> =
        entryDao.countPastOnMonthDay(monthDay, beforeYear)
    val count: Flow<Int> = entryDao.count()
    val photoCount: Flow<Int> = photoDao.count()

    /**
     * 保存记录（可挂多个栏目）与照片列表；被移除的照片会连同文件一起清理。
     * 遗留的 entries.categoryId 列写入第一个选中栏目，保持旧数据可读。
     */
    suspend fun save(
        entryId: Long?,
        date: String,
        content: String,
        categoryIds: List<Long>,
        photos: List<PhotoEntity>,
        removedPhotoIds: List<Long>,
    ): Long {
        val now = System.currentTimeMillis()
        val id: Long
        if (entryId == null) {
            id = entryDao.insert(
                EntryEntity(
                    date = date, content = content,
                    categoryId = categoryIds.firstOrNull(),
                    createdAt = now, updatedAt = now,
                )
            )
        } else {
            val old = entryDao.getById(entryId)
            entryDao.update(
                (old?.entry ?: EntryEntity(id = entryId, date = date, content = "")).copy(
                    date = date, content = content,
                    categoryId = categoryIds.firstOrNull(),
                    updatedAt = now,
                )
            )
            id = entryId
        }
        // 重建多栏目关联
        linkDao.deleteForEntry(id)
        if (categoryIds.isNotEmpty()) {
            linkDao.insertAll(categoryIds.map { EntryCategoryCrossRef(entryId = id, categoryId = it) })
        }
        removedPhotoIds.forEach { pid ->
            photoDao.deleteById(pid)
        }
        photoDao.insertAll(photos.map { it.copy(entryId = id) })
        return id
    }

    /** 删除记录并清理其照片文件。 */
    suspend fun delete(entry: EntryWithPhotos) {
        entry.photos.forEach { photoStore.delete(it.relPath) }
        entryDao.deleteById(entry.entry.id)
    }

    suspend fun get(id: Long): EntryWithPhotos? = entryDao.getById(id)

    /** 仅删除照片（编辑页移除时调用），同时清理文件。 */
    suspend fun deletePhoto(photo: PhotoEntity) {
        photoDao.deleteById(photo.id)
        photoStore.delete(photo.relPath)
    }
}

class CategoryRepository(private val dao: CategoryDao, private val entryDao: EntryDao) {
    val all: Flow<List<CategoryEntity>> = dao.observeAll()

    suspend fun ensurePresets() {
        if (dao.count() > 0) return
        listOf(
            Triple("生活", "🌿", "green"),
            Triple("美食", "🍜", "amber"),
            Triple("旅行", "🧳", "blue"),
            Triple("心情", "☁️", "purple"),
            Triple("工作", "📚", "rose"),
        ).forEachIndexed { index, (name, emoji, color) ->
            dao.insert(CategoryEntity(name = name, emoji = emoji, colorKey = color, sortOrder = index))
        }
    }

    suspend fun save(id: Long?, name: String, emoji: String, colorKey: String, sortOrder: Int = 0): Long =
        if (id == null) dao.insert(CategoryEntity(name = name, emoji = emoji, colorKey = colorKey, sortOrder = sortOrder))
        else {
            dao.update(CategoryEntity(id = id, name = name, emoji = emoji, colorKey = colorKey, sortOrder = sortOrder))
            id
        }

    /** 删除栏目只摘除关联，不删除任何记录 —— 记录回到「未分类」。 */
    suspend fun delete(category: CategoryEntity) {
        entryDao.clearCategory(category.id)   // 清掉遗留单选列
        dao.delete(category)                  // 关联表行由外键级联删除
    }
}

class WhisperRepository(private val dao: WhisperDao) {
    val all: Flow<List<WhisperEntity>> = dao.observeAll()
    val count: Flow<Int> = dao.count()
    suspend fun add(content: String) = dao.insert(WhisperEntity(content = content))
    suspend fun delete(id: Long) = dao.deleteById(id)
}
