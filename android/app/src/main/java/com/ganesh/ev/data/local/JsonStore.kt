package com.ganesh.ev.data.local

import android.content.Context

/**
 * Process-singleton key→JSON cache (A2). Initialized from
 * [com.ganesh.ev.EvApplication]; reads/writes are no-ops until then. Callers
 * serialize/deserialize with Gson so this stays type-agnostic.
 */
object JsonStore {

    private var dao: JsonCacheDao? = null

    fun init(context: Context) {
        dao = AppDatabase.getInstance(context).jsonCacheDao()
    }

    suspend fun put(key: String, json: String) {
        dao?.put(JsonCacheEntity(key, json, System.currentTimeMillis()))
    }

    suspend fun get(key: String): String? = dao?.get(key)?.value
}
