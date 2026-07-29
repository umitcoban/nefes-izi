package com.umityasincoban.nefesizi.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockSessionTest {
    @Test
    fun `new process starts unauthenticated`() {
        assertFalse(AppLockSession().isAuthenticated)
    }

    @Test
    fun `foreground within timeout keeps authentication`() {
        val session = AppLockSession(lockAfterMillis = 30_000)
        session.markAuthenticated()
        session.markBackgrounded(elapsedRealtime = 10_000)

        val expired = session.onForegrounded(elapsedRealtime = 39_999)

        assertFalse(expired)
        assertTrue(session.isAuthenticated)
    }

    @Test
    fun `foreground at timeout clears authentication`() {
        val session = AppLockSession(lockAfterMillis = 30_000)
        session.markAuthenticated()
        session.markBackgrounded(elapsedRealtime = 10_000)

        val expired = session.onForegrounded(elapsedRealtime = 40_000)

        assertTrue(expired)
        assertFalse(session.isAuthenticated)
    }

    @Test
    fun `explicit clear removes authentication immediately`() {
        val session = AppLockSession()
        session.markAuthenticated()

        session.clearAuthentication()

        assertFalse(session.isAuthenticated)
    }
}
