package com.example.cashia.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.cashia.models.SmsStatus
import com.example.cashia.models.UnmatchedSms
import kotlinx.coroutines.flow.Flow

@Dao
interface UnmatchedSmsDao {

    // ── Writes ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sms: UnmatchedSms)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(list: List<UnmatchedSms>)

    @Update
    suspend fun update(sms: UnmatchedSms)

    @Delete
    suspend fun delete(sms: UnmatchedSms)

    // ── Status mutations ────────────────────────────────────────────────────

    @Query("UPDATE UnmatchedSms SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: SmsStatus)

    /** Ignore all messages sharing the same content hash — one tap clears duplicates. */
    @Query("UPDATE UnmatchedSms SET status = 'IGNORED' WHERE contentHash = :hash AND contentHash != ''")
    suspend fun ignoreAllByHash(hash: String)

    @Query("UPDATE UnmatchedSms SET patternGroup = :group WHERE id = :id")
    suspend fun assignPatternGroup(id: Long, group: String)

    // ── Live flows (UI) ─────────────────────────────────────────────────────

    /** Pending count — drives the badge. IGNORED and SAVED are excluded. */
    @Query("SELECT COUNT(*) FROM UnmatchedSms WHERE status = 'PENDING'")
    fun pendingCountFlow(): Flow<Int>

    /**
     * Active list: PENDING + SKIPPED items, newest first. Bounded to the same window
     * size as TransactionsViewModel.WINDOW_PAGE — nothing else in the app subscribes
     * to an unbounded table scan, this shouldn't either.
     * IGNORED and SAVED are hidden from the main view.
     */
    @Query(
        """
        SELECT * FROM UnmatchedSms
        WHERE status IN ('PENDING', 'SKIPPED')
        ORDER BY receivedAt DESC
        LIMIT 200
    """
    )
    fun allUnmatchedFlow(): Flow<List<UnmatchedSms>>

    /** Next item to review — PENDING only, highest confidence first. */
    @Query(
        """
        SELECT * FROM UnmatchedSms 
        WHERE status = 'PENDING'
        ORDER BY confidence DESC, receivedAt DESC 
        LIMIT 1
    """
    )
    fun nextPendingFlow(): Flow<UnmatchedSms?>

    /** Count of ignored messages — useful for a "show ignored" toggle. */
    @Query("SELECT COUNT(*) FROM UnmatchedSms WHERE status = 'IGNORED'")
    fun ignoredCountFlow(): Flow<Int>

    // ── One-shot queries ────────────────────────────────────────────────────

    @Query("SELECT MAX(receivedAt) FROM UnmatchedSms")
    suspend fun getLastReceivedAt(): Long?

    /** Rows stored by an older parser that haven't been manually resolved. */
    @Query(
        """
        SELECT * FROM UnmatchedSms 
        WHERE parserVersion < :currentVersion 
          AND status = 'PENDING'
        ORDER BY receivedAt DESC
    """
    )
    suspend fun getOutdatedRows(currentVersion: Int): List<UnmatchedSms>

    @Query(
        """
        SELECT contentHash, COUNT(*) as cnt 
        FROM UnmatchedSms 
        WHERE status = 'PENDING' AND contentHash != ''
        GROUP BY contentHash 
        HAVING cnt > 1
        ORDER BY cnt DESC
    """
    )
    suspend fun getDuplicateHashes(): List<HashCount>

    @Query("SELECT * FROM UnmatchedSms WHERE contentHash = :hash AND status = 'PENDING'")
    suspend fun getByHash(hash: String): List<UnmatchedSms>

    @Query(
        """
        SELECT * FROM UnmatchedSms 
        WHERE confidence > 0 AND confidence < :threshold AND status = 'PENDING'
        ORDER BY confidence DESC
    """
    )
    suspend fun getLowConfidence(threshold: Float): List<UnmatchedSms>

    @Query("SELECT * FROM UnmatchedSms WHERE patternGroup IS NULL AND status = 'PENDING'")
    suspend fun getUngrouped(): List<UnmatchedSms>

    @Query("SELECT COUNT(*) FROM UnmatchedSms WHERE status = 'PENDING'")
    suspend fun pendingCount(): Int

    @Query("SELECT AVG(confidence) FROM UnmatchedSms WHERE status = 'PENDING'")
    suspend fun averageConfidence(): Float?
}

data class HashCount(val contentHash: String, val cnt: Int)