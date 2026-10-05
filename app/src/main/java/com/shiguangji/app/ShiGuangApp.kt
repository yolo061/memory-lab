package com.shiguangji.app

import android.app.Application
import android.content.Context
import com.shiguangji.app.core.BackupManager
import com.shiguangji.app.core.PhotoStore
import com.shiguangji.app.core.PinManager
import com.shiguangji.app.core.ThemeManager
import com.shiguangji.app.data.AppDatabase
import com.shiguangji.app.data.CategoryRepository
import com.shiguangji.app.data.EntryRepository
import com.shiguangji.app.data.SettingsStore
import com.shiguangji.app.data.WhisperRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShiGuangApp : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        graph.appScope.launch { graph.init() }
    }
}

/**
 * 手工依赖图：规模小、依赖简单，无需引入 DI 框架。
 * 所有仓库为单例，数据库连接全局唯一。
 */
class AppGraph(context: Context) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = SettingsStore(context)
    val photoStore = PhotoStore(context)
    val themeManager = ThemeManager(context, settings)
    val pinManager = PinManager(settings)

    private val db by lazy { AppDatabase.build(context) }

    val entryRepo by lazy { EntryRepository(db.entryDao(), db.photoDao(), db.entryCategoryDao(), photoStore) }
    val categoryRepo by lazy { CategoryRepository(db.categoryDao(), db.entryDao()) }
    val whisperRepo by lazy { WhisperRepository(db.whisperDao()) }
    val backupManager by lazy { BackupManager(context, db, photoStore) }

    suspend fun init() {
        themeManager.load()
        categoryRepo.ensurePresets()
    }
}

val Context.appGraph: AppGraph
    get() = (applicationContext as ShiGuangApp).graph
