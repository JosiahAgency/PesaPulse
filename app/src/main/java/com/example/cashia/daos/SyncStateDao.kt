package com.example.cashia.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cashia.models.SyncState

@Dao
interface SyncStateDao {
    @Query("SELECT lastSmsId FROM SyncState WHERE `key` = :key LIMIT 1")
    suspend fun getLastSmsId(key: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(syncState: SyncState)
}