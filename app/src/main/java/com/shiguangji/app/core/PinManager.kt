package com.shiguangji.app.core

import com.shiguangji.app.data.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

/** 纯函数便于单元测试：PIN 绝不明文存储，SHA-256 + 随机盐。 */
object PinHash {
    fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun newSalt(): String {
        val bytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * 悄悄话门禁。sessionUnlocked 仅存活于进程内存：
 * App 被杀或重启后自动回到锁定态。
 */
class PinManager(private val settings: SettingsStore) {

    @Volatile
    var sessionUnlocked: Boolean = false

    /** PIN 是否已设置（响应式）。 */
    val isPinSet: Flow<Boolean> = settings.pinHashFlow.map { it != null }

    suspend fun hasPin(): Boolean = isPinSet.first()

    suspend fun setPin(pin: String) {
        val salt = PinHash.newSalt()
        settings.setPin(PinHash.hash(pin, salt), salt)
        sessionUnlocked = true
    }

    suspend fun verify(pin: String): Boolean {
        val (hash, salt) = settings.pinHashAndSalt()
        val ok = hash != null && salt != null && PinHash.hash(pin, salt) == hash
        if (ok) sessionUnlocked = true
        return ok
    }

    suspend fun clearPin() {
        settings.clearPin()
        sessionUnlocked = false
    }
}
