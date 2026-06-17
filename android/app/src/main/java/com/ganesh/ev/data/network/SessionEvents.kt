package com.ganesh.ev.data.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide auth events (A4).
 *
 * Emitted when the session can no longer be recovered — i.e. the refresh token
 * is missing/rejected — so the UI layer can clear state and route to login
 * instead of dead-ending on failing calls. Context-free (just a SharedFlow), so
 * [RetrofitClient] can signal it without depending on Android/UI.
 */
object SessionEvents {

    // replay=0 so a late re-subscribe doesn't re-trigger logout; a small buffer
    // so tryEmit never drops the event if the collector is momentarily absent.
    private val _loggedOut = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val loggedOut: SharedFlow<Unit> = _loggedOut.asSharedFlow()

    /** Signals a terminal auth failure. Thread-safe; callable from any thread. */
    fun notifySessionExpired() {
        _loggedOut.tryEmit(Unit)
    }
}
