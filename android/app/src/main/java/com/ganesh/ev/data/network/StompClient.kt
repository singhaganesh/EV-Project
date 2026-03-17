package com.ganesh.ev.data.network

import android.util.Log
import okhttp3.*
import java.util.*

class StompClient(private val url: String) {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private val listeners = mutableMapOf<String, (String) -> Unit>()
    private var isConnected = false

    fun connect(headers: Map<String, String> = emptyMap()) {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("STOMP", "WebSocket Opened. Sending STOMP CONNECT...")
                val connectFrame = StringBuilder("CONNECT\n")
                connectFrame.append("accept-version:1.2\n")
                headers.forEach { (k, v) -> connectFrame.append("$k:$v\n") }
                connectFrame.append("\n\u0000")
                webSocket.send(connectFrame.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.startsWith("CONNECTED")) {
                    isConnected = true
                    Log.d("STOMP", "STOMP Connected")
                    // Re-subscribe to existing listeners if needed
                } else if (text.startsWith("MESSAGE")) {
                    parseMessage(text)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("STOMP", "WebSocket Failure: ${t.message}")
                isConnected = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("STOMP", "WebSocket Closed: $reason")
                isConnected = false
            }
        })
    }

    private fun parseMessage(frame: String) {
        val lines = frame.split("\n")
        val destination = lines.find { it.startsWith("destination:") }?.substringAfter(":")
        val body = frame.substringAfter("\n\n").substringBefore("\u0000")
        
        if (destination != null) {
            listeners[destination]?.invoke(body)
        }
    }

    fun subscribe(topic: String, callback: (String) -> Unit) {
        listeners[topic] = callback
        if (isConnected) {
            sendSubscribeFrame(topic)
        } else {
            // This is a simplification. Ideally, queue subscriptions until CONNECTED.
            Log.w("STOMP", "Subscribed to $topic but not yet connected.")
            // Polling check or simple delay for this prototype
            Thread {
                while (!isConnected) Thread.sleep(500)
                sendSubscribeFrame(topic)
            }.start()
        }
    }

    private fun sendSubscribeFrame(topic: String) {
        val id = UUID.randomUUID().toString()
        val frame = "SUBSCRIBE\nid:$id\ndestination:$topic\nack:auto\n\n\u0000"
        webSocket?.send(frame)
        Log.d("STOMP", "Subscribed to $topic")
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
    }
}
