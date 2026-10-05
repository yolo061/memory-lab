package com.shiguangji.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Transaction
    @Query("SELECT * FROM entries ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<EntryWithPhotos>>

    @Transaction
    @Query("SELECT * FROM entries WHERE content LIKE '%' || :q || '%' ORDER BY date DESC, createdAt DESC")
    fun search(q: String): Flow<List<EntryWithPhotos>>

    @Transaction
    @Query(
        "SELECT DISTINCT entries.* FROM entries " +
            "INNER JOIN entry_categories ec ON ec.entryId = entries.id " +
            "WHERE ec.categoryId = :categoryId " +
            "ORDER BY entries.date DESC, entries.createdAt DESC"
    )
    fun observeByCategory(categoryId: Long): Flow<List<EntryWithPhotos>>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    fun observeById(id: Long): Flow<EntryWithPhotos?>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): EntryWithPhotos?

    /** 那年今日：月-日相同且年份更早的所有记录。 */
    @Transaction
    @Query(
        "SELECT * FROM entries WHERE substr(date, 6, 5) = :monthDay AND substr(date, 1, 4) < :beforeYear " +
            "ORDER BY date DESC"
    )
    fun pastOnMonthDay(monthDay: String, beforeYear: Int): Flow<List<EntryWithPhotos>>

    @Transaction
    @Query(
        "SELECT COUNT(*) FROM entries WHERE substr(date, 6, 5) = :monthDay AND substr(date, 1, 4) < :beforeYear"
    )
    fun countPastOnMonthDay(monthDay: String, beforeYear: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM entries")
    fun count(): Flow<Int>

    @Query("UPDATE entries SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Long)
}

@Dao
interface PhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoEntity>)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM photos WHERE entryId = :entryId ORDER BY sortIndex")
    suspend fun photosOf(entryId: Long): List<PhotoEntity>

    @Query("SELECT COUNT(*) FROM photos")
    fun count(): Flow<Int>
}

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)
}

/** 栏目 + 记录数（栏目页卡片展示用）。 */
data class CategoryWithCount(val category: CategoryEntity, val entryCount: Int)

@Dao
interface EntryCategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<EntryCategoryCrossRef>)

    @Query("DELETE FROM entry_categories WHERE entryId = :entryId")
    suspend fun deleteForEntry(entryId: Long)
}

@Dao
interface WhisperDao {

    @Query("SELECT * FROM whispers ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WhisperEntity>>

    @Insert
    suspend fun insert(whisper: WhisperEntity)

    @Query("DELETE FROM whispers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM whispers")
    fun count(): Flow<Int>
}
