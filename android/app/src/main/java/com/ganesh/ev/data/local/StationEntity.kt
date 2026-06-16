package com.ganesh.ev.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ganesh.ev.data.model.Station

/** Cached station row for offline display (CV-10). */
@Entity(tableName = "station_cache")
data class StationEntity(
        @PrimaryKey val id: Long,
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val meta: String?,
        val operatingHours: String?,
        val pricePerKwh: Double?,
        val truckPricePerKwh: Double?,
        val isOpen: Boolean?,
        val lastUsedTime: String?
)

fun Station.toEntity(): StationEntity =
        StationEntity(
                id = id,
                name = name,
                latitude = latitude,
                longitude = longitude,
                address = address,
                meta = meta,
                operatingHours = operatingHours,
                pricePerKwh = pricePerKwh,
                truckPricePerKwh = truckPricePerKwh,
                isOpen = isOpen,
                lastUsedTime = lastUsedTime
        )

fun StationEntity.toStation(): Station =
        Station(
                id = id,
                name = name,
                latitude = latitude,
                longitude = longitude,
                address = address,
                meta = meta,
                operatingHours = operatingHours,
                pricePerKwh = pricePerKwh,
                truckPricePerKwh = truckPricePerKwh,
                isOpen = isOpen,
                lastUsedTime = lastUsedTime
        )
