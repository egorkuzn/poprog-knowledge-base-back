package com.example.poprogknowledgebaseback

import com.example.poprogknowledgebaseback.application.importer.isTelegramUrl
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Lab19ImportServiceTelegramHostTest {
    @Test
    fun `returns true for known telegram hosts`() {
        assertTrue(isTelegramUrl("https://t.me/poprog"))
        assertTrue(isTelegramUrl("https://telegram.me/poprog"))
        assertTrue(isTelegramUrl("https://telegram.org/blog"))
        assertTrue(isTelegramUrl("https://subdomain.t.me/channel"))
    }

    @Test
    fun `returns false for non telegram hosts and malformed urls`() {
        assertFalse(isTelegramUrl("https://example.org/page"))
        assertFalse(isTelegramUrl("https://www.iae.nsk.su/ru/laboratory-sites/lab-19"))
        assertFalse(isTelegramUrl("not-a-url"))
    }
}
