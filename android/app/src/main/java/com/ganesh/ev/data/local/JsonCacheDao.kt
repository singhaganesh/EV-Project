package com.ganesh.ev.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface JsonCacheDao {

    @Query("SELECT * FROM json_cache WHERE cache_key = :key")
    suspend fun get(key: String): JsonCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: JsonCacheEntity)
}
