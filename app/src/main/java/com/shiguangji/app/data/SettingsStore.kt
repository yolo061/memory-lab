package com.shiguangji.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 轻量设置存储。兼容策略：
 * - 只增不改：新版本只允许新增 key，禁止改动已有 key 的语义；
 * - 读取全部带默认值，旧版本备份/新版本数据互相可读。
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val CURRENT_THEME = stringPreferencesKey("current_theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
    }

    val currentThemeId: Flow<String> = context.dataStore.data.map { it[Keys.CURRENT_THEME] ?: "t-warm" }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: false }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.BIOMETRIC_ENABLED] ?: true }

    /** PIN 是否已设置——响应式：setPin / clearPin 后立即发出新值。 */
    val pinHashFlow: Flow<String?> = context.dataStore.data.map { it[Keys.PIN_HASH] }

    suspend fun setCurrentTheme(id: String) = context.dataStore.edit { it[Keys.CURRENT_THEME] = id }
    suspend fun setDynamicColor(on: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = on }
    suspend fun setBiometricEnabled(on: Boolean) = context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = on }

    suspend fun setPin(hash: String, salt: String) = context.dataStore.edit {
        it[Keys.PIN_HASH] = hash
        it[Keys.PIN_SALT] = salt
    }

    suspend fun pinHashAndSalt(): Pair<String?, String?> =
        context.dataStore.data.map { prefs -> prefs[Keys.PIN_HASH] to prefs[Keys.PIN_SALT] }.first()

    suspend fun clearPin() = context.dataStore.edit {
        it.remove(Keys.PIN_HASH)
        it.remove(Keys.PIN_SALT)
    }
}
