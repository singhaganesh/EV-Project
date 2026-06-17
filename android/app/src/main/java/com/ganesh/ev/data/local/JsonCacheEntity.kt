package com.ganesh.ev.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Generic key→JSON cache row (A2). Used to keep the user's bookings and charging
 * history readable offline without modelling a normalized schema per type.
 */
@Entity(tableName = "json_cache")
data class JsonCacheEntity(
        @PrimaryKey @ColumnInfo(name = "cache_key") val key: String,
        @ColumnInfo(name = "value_json") val value: String,
        @ColumnInfo(name = "updated_at") val updatedAt: Long
)
