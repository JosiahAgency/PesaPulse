package com.example.cashia.utility

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cashia.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ReliabilityMigrationTest {
    @Test fun version13MigratesWithoutLosingRecordsOrTrustingLegacyManualDefaults() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "reliability-migration-test"
        context.deleteDatabase(name)
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
                // Version 13 schema: all optional transaction fields were required; no match table or anchor.
                old.execSQL("""CREATE TABLE MpesaTransactions (
                    id TEXT NOT NULL PRIMARY KEY, smsId INTEGER NOT NULL, amount REAL NOT NULL,
                    transactionCost REAL NOT NULL, recipient TEXT NOT NULL, date TEXT NOT NULL,
                    type TEXT NOT NULL, balance REAL NOT NULL, time TEXT NOT NULL,
                    timestamp INTEGER NOT NULL, category TEXT NOT NULL, reversedTxId TEXT)""")
                old.execSQL("""CREATE TABLE UpcomingTransactions (
                    id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, amount REAL NOT NULL,
                    categoryId TEXT NOT NULL, dueDate TEXT NOT NULL, type TEXT NOT NULL,
                    recurrence TEXT NOT NULL, notes TEXT NOT NULL, status TEXT NOT NULL,
                    createdAt INTEGER NOT NULL, matchedTransactionId TEXT)""")
                old.execSQL("""CREATE TABLE UnmatchedSms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, smsId INTEGER NOT NULL,
                    rawBody TEXT NOT NULL, receivedAt INTEGER NOT NULL, status TEXT NOT NULL,
                    contentHash TEXT NOT NULL, parserVersion INTEGER NOT NULL, confidence REAL NOT NULL,
                    extractedFields TEXT, patternGroup TEXT)""")
                old.execSQL("""CREATE TABLE CustomCategories (
                    id TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL, emoji TEXT NOT NULL,
                    keywords TEXT NOT NULL, parentId TEXT, createdAt INTEGER NOT NULL)""")
                old.execSQL("""CREATE TABLE SystemCategoryOverrides (
                    id TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL, emoji TEXT NOT NULL)""")
                old.execSQL("""CREATE TABLE UserCorrections (
                    recipient TEXT NOT NULL PRIMARY KEY, categoryId TEXT NOT NULL,
                    hitCount INTEGER NOT NULL, updatedAt INTEGER NOT NULL)""")
                old.execSQL("CREATE TABLE SyncState (`key` TEXT NOT NULL PRIMARY KEY, lastSmsId INTEGER NOT NULL)")
                for ((table, columns) in mapOf(
                    "MpesaTransactions" to listOf("timestamp", "date", "type", "category", "reversedTxId"),
                    "UpcomingTransactions" to listOf("dueDate", "status", "type", "categoryId"),
                    "UnmatchedSms" to listOf("status", "contentHash", "parserVersion", "confidence")
                )) for (column in columns) old.execSQL("CREATE INDEX index_${table}_$column ON $table($column)")
                for (table in listOf("MpesaTransactions", "UnmatchedSms")) {
                    old.execSQL("CREATE UNIQUE INDEX index_${table}_smsId ON $table(smsId)")
                }
                old.execSQL("INSERT INTO MpesaTransactions VALUES ('ABC1234567',1,1000,10,'Jane Wanjiku','2024-01-31','expense',500,'1:30 PM',1706707800000,'OTHER',NULL)")
                old.execSQL("INSERT INTO MpesaTransactions VALUES ('2',2,0,0,'Manual Entry (from SMS)','2024-01-31','expense',0,'12:00 PM',1706702400000,'OTHER',NULL)")
                old.execSQL("INSERT INTO UnmatchedSms VALUES (1,2,'Original unrecognised SMS',1706702400000,'SAVED','hash',2,0.3,NULL,NULL)")
                old.execSQL("INSERT INTO UpcomingTransactions VALUES ('rent','Jane Wanjiku',1000,'OTHER','2024-01-31','EXPENSE','MONTHLY','','PENDING',0,NULL)")
                old.execSQL("INSERT INTO SyncState VALUES ('mpesa_sync',42)")
                old.execSQL("INSERT INTO CustomCategories VALUES ('custom','Family','F','family',NULL,0)")
                old.version = 13
            }
            val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(AppDatabase.MIGRATION_13_14).build()
            try {
                val rows = migrated.transactionDao().getAllTransactionsOnce()
                assertEquals(1, rows.size)
                assertEquals("ABC1234567", rows.single().reference)
                assertEquals(10.0, rows.single().transactionCost!!, 0.0)
                assertEquals(1, migrated.unmatchedSmsDao().pendingCount())
                assertEquals("Original unrecognised SMS", migrated.unmatchedSmsDao().getOutdatedRows(4).single().rawBody)
                assertEquals("2024-01-31", migrated.upcomingTransactionDao().getById("rent")!!.recurrenceAnchor)
                assertEquals(42L, migrated.syncStateDao().getLastSmsId("mpesa_sync"))
                assertEquals("Family", migrated.customCategoryDao().getAllCustomCategoriesOnce().single().label)
                UpcomingPaymentService(migrated).reconcilePending()
                assertNotNull(migrated.paymentMatchDao().get("rent", "ABC1234567"))
            } finally { migrated.close() }
        } finally { context.deleteDatabase(name) }
    }
}
