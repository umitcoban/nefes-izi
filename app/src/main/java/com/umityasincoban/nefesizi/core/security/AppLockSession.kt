package com.umityasincoban.nefesizi.core.security

class AppLockSession(
    private val lockAfterMillis: Long = 30_000L,
) {
    var isAuthenticated: Boolean = false
        private set

    private var backgroundAtElapsed: Long? = null

    fun markAuthenticated() {
        isAuthenticated = true
        backgroundAtElapsed = null
    }

    fun markBackgrounded(elapsedRealtime: Long) {
        backgroundAtElapsed = elapsedRealtime
    }

    fun onForegrounded(elapsedRealtime: Long): Boolean {
        val expired = backgroundAtElapsed?.let {
            elapsedRealtime - it >= lockAfterMillis
        } ?: false
        backgroundAtElapsed = null
        if (expired) isAuthenticated = false
        return expired
    }

    fun clearAuthentication() {
        isAuthenticated = false
        backgroundAtElapsed = null
    }
}
