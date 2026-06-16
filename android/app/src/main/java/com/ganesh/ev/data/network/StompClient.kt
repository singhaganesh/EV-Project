package com.ganesh.ev.data.network

import android.util.Log
import okhttp3.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Minimal STOMP-over-WebSocket client.
 *
 * - Sends the JWT in the CONNECT frame (required by the backend channel
 *   interceptor) so subscriptions are authorized to the current user.
 * - Automatically reconnects with exponential backoff and re-subscribes all
 *   active topics, so losing the connection mid-charge resumes telemetry.
 * - disconnect() stops reconnection and tears down the socket.
 */
class StompClient(private val url: String) {

    // STOMP frames are terminated by the NULL byte. Built at runtime so the
    // source file stays free of embedded control characters.
    private val nullTerminator: String = 0.toChar().toString()

    private var webSocket: WebSocket? = null
    // pingInterval makes OkHttp send WebSocket ping frames and surface a dead
    // peer as onFailure, which triggers our reconnect — without it a half-open
    // connection can silently freeze telemetry (CV-6).
    private val client = OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    private val listeners = ConcurrentHashMap<String, (String) -> Unit>()

    @Volatile private var isConnected = false
    @Volatile private var userClosed = false
    @Volatile private var reconnectAttempts = 0

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    fun connect() {
        userClosed = false
        openSocket()
    }

    private fun openSocket() {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("STOMP", "WebSocket opened; sending CONNECT")
                val token = RetrofitClient.getAuthToken()
                val connectFrame = buildString {
                    append("CONNECT\n")
                    append("accept-version:1.2\n")
                    append("heart-beat:0,0\n")
                    if (token.isNotEmpty()) append("Authorization:Bearer $token\n")
                    append("\n")
                    append(nullTerminator)
                }
                webSocket.send(connectFrame)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                when {
                    text.startsWith("CONNECTED") -> {
                        isConnected = true
                        reconnectAttempts = 0
                        Log.d("STOMP", "STOMP connected; re-subscribing ${listeners.size} topic(s)")
                        listeners.keys.forEach { sendSubscribeFrame(it) }
                    }
                    text.startsWith("MESSAGE") -> parseMessage(text)
                    text.startsWith("ERROR") -> Log.e("STOMP", "STOMP error frame: $text")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("STOMP", "WebSocket failure: ${t.message}")
                isConnected = false
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("STOMP", "WebSocket closed: $reason")
                isConnected = false
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (userClosed) return
        val attempt = reconnectAttempts++
        val delaySec = minOf(30L, Math.round(Math.pow(2.0, attempt.toDouble())).toLong()).coerceAtLeast(2L)
        Log.d("STOMP", "Reconnecting in ${delaySec}s (attempt ${attempt + 1})")
        try {
            scheduler.schedule({ if (!userClosed) openSocket() }, delaySec, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.e("STOMP", "Failed to schedule reconnect: ${e.message}")
        }
    }

    private fun parseMessage(frame: String) {
        val lines = frame.split("\n")
        val destination = lines.find { it.startsWith("destination:") }?.substringAfter(":")
        val body = frame.substringAfter("\n\n").substringBefore(nullTerminator)
        if (destination != null) {
            listeners[destination]?.invoke(body)
        }
    }

    fun subscribe(topic: String, callback: (String) -> Unit) {
        listeners[topic] = callback
        if (isConnected) {
            sendSubscribeFrame(topic)
        }
        // Otherwise it will be (re)sent automatically once CONNECTED arrives.
    }

    fun unsubscribe(topic: String) {
        listeners.remove(topic)
    }

    private fun sendSubscribeFrame(topic: String) {
        val id = UUID.randomUUID().toString()
        val frame = "SUBSCRIBE\nid:$id\ndestination:$topic\nack:auto\n\n$nullTerminator"
        webSocket?.send(frame)
        Log.d("STOMP", "Subscribed to $topic")
    }

    fun disconnect() {
        userClosed = true
        listeners.clear()
        try {
            webSocket?.send("DISCONNECT\n\n$nullTerminator")
        } catch (_: Exception) {
        }
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        isConnected = false
    }
}
