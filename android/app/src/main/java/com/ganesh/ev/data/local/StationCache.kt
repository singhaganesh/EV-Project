package com.ganesh.ev.data.local

import android.content.Context
import com.ganesh.ev.data.model.Station

/**
 * Process-singleton offline cache for the stations list (CV-10). Initialized
 * from [com.ganesh.ev.EvApplication]; reads/writes are no-ops until then.
 */
object StationCache {

    private var dao: StationDao? = null

    fun init(context: Context) {
        dao = AppDatabase.getInstance(context).stationDao()
    }

    suspend fun cache(stations: List<Station>) {
        val d = dao ?: return
        if (stations.isEmpty()) return
        d.upsertAll(stations.map { it.toEntity() })
    }

    suspend fun cached(): List<Station> = dao?.getAll()?.map { it.toStation() } ?: emptyList()
}
