package com.example.cashia.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cashia.models.PaymentMatch
import com.example.cashia.models.PaymentMatchReview
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMatchDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(matches: List<PaymentMatch>)

    @Query("""
        SELECT m.upcomingId, m.transactionId, u.title, u.amount AS expectedAmount,
               u.dueDate, t.recipient, t.amount, t.date, t.reference, m.reason
        FROM PaymentMatches m
        JOIN UpcomingTransactions u ON u.id = m.upcomingId
        JOIN MpesaTransactions t ON t.id = m.transactionId
        WHERE m.dismissed = 0 AND u.status = 'PENDING'
          AND t.reversedTxId IS NULL
          AND NOT EXISTS (SELECT 1 FROM MpesaTransactions reversal WHERE reversal.reversedTxId = t.id)
          AND NOT EXISTS (SELECT 1 FROM UpcomingTransactions paid
                          WHERE paid.matchedTransactionId = t.id)
        ORDER BY u.dueDate, t.timestamp DESC
    """)
    fun pendingReviews(): Flow<List<PaymentMatchReview>>

    @Query("SELECT * FROM PaymentMatches WHERE upcomingId = :upcomingId AND transactionId = :transactionId AND dismissed = 0")
    suspend fun get(upcomingId: String, transactionId: String): PaymentMatch?

    @Query("UPDATE PaymentMatches SET dismissed = 1 WHERE upcomingId = :upcomingId AND transactionId = :transactionId")
    suspend fun dismiss(upcomingId: String, transactionId: String)

    @Query("DELETE FROM PaymentMatches WHERE upcomingId = :upcomingId AND dismissed = 0")
    suspend fun clearActive(upcomingId: String)
}
