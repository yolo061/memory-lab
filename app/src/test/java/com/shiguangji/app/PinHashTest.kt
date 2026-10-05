package com.shiguangji.app

import com.shiguangji.app.core.PinHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHashTest {

    @Test
    fun `同输入同哈希`() {
        assertEquals(PinHash.hash("1234", "salt"), PinHash.hash("1234", "salt"))
    }

    @Test
    fun `不同盐不同哈希`() {
        assertNotEquals(PinHash.hash("1234", "salt-a"), PinHash.hash("1234", "salt-b"))
    }

    @Test
    fun `不同密码不同哈希`() {
        assertNotEquals(PinHash.hash("1234", "s"), PinHash.hash("9876", "s"))
    }

    @Test
    fun `盐是随机的十六位`() {
        val a = PinHash.newSalt()
        val b = PinHash.newSalt()
        assertNotEquals(a, b)
        assertEquals(32, a.length)
        assertTrue(a.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `哈希长度固定64位`() {
        assertEquals(64, PinHash.hash("0000", PinHash.newSalt()).length)
    }
}
