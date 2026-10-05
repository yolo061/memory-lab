package com.shiguangji.app.core

import android.content.Context
import android.net.Uri
import com.shiguangji.app.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 主题包管理：内置主题来自 assets/themes/，导入主题存放 filesDir/themes/。
 * App 每次启动都会重新扫描两个来源 —— 用户新拷进来的主题包文件自动被识别。
 */
class ThemeManager(
    private val context: Context,
    private val settings: SettingsStore,
) {
    private val importedDir: File
        get() = File(context.filesDir, "themes").apply { mkdirs() }

    private val _packs = MutableStateFlow<List<ThemePack>>(emptyList())
    val packs: StateFlow<List<ThemePack>> = _packs

    private val _currentId = MutableStateFlow(DEFAULT_THEME_ID)
    val currentId: StateFlow<String> = _currentId

    private val _currentPack = MutableStateFlow(WARM_PACK)
    val currentPack: StateFlow<ThemePack> = _currentPack

    val current: ThemePack
        get() = _packs.value.find { it.id == _currentId.value } ?: _packs.value.firstOrNull() ?: WARM_PACK

    suspend fun load() {
        val list = mutableListOf<ThemePack>()
        // 1. 内置主题
        withContext(Dispatchers.IO) {
            context.assets.list("themes")?.sorted()?.forEach { file ->
                runCatching {
                    val json = context.assets.open("themes/$file").bufferedReader().use { it.readText() }
                    list += ThemePackParser.parse(json)
                }
            }
            // 2. 用户导入的主题（文件即事实来源，天然可迁移）
            importedDir.listFiles { f -> f.extension.equals("json", true) }
                ?.sortedBy { it.name }
                ?.forEach { f ->
                    runCatching {
                        list += ThemePackParser.parse(f.readText(), fileName = f.name)
                    }
                }
        }
        if (list.isEmpty()) list += WARM_PACK
        _packs.value = list
        val saved = settings.currentThemeId.first()
        _currentId.value = if (list.any { it.id == saved }) saved else list.first().id
        _currentPack.value = current
    }

    suspend fun setCurrent(id: String) {
        _currentId.value = id
        _currentPack.value = current
        settings.setCurrentTheme(id)
    }

    /** 导入主题包：校验通过后落盘，随包文件将来可直接拷走复用。 */
    suspend fun import(uri: Uri): ThemePack = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalArgumentException("读不到文件内容")
        val pack = ThemePackParser.parse(json)
        val file = File(importedDir, "import-${System.currentTimeMillis()}.json")
        file.writeText(json)
        load()
        setCurrent(pack.id)
        _packs.value.first { it.fileName == file.name }
    }

    /** 删除导入的主题包；若正在使用则回到默认。 */
    suspend fun delete(pack: ThemePack) {
        if (pack.fileName == null) return
        File(importedDir, pack.fileName).delete()
        load()
        if (_currentId.value == pack.id) setCurrent(_packs.value.first().id)
    }

    companion object {
        const val DEFAULT_THEME_ID = "t-warm"

        /** 资产加载失败时的兜底主题，保证 App 永远有可用配色。 */
        val WARM_PACK = ThemePack(
            id = DEFAULT_THEME_ID, name = "拾光暖阳", author = "拾光记官方",
            version = 1, isDark = false, colors = ThemeColors.WARM, rawJson = "{}",
        )
    }
}
