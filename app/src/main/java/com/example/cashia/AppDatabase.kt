package com.example.cashia

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.cashia.daos.CustomCategoryDao
import com.example.cashia.daos.SystemCategoryOverrideDao
import com.example.cashia.daos.TransactionDao
import com.example.cashia.daos.UpcomingTransactionDao
import com.example.cashia.models.CustomCategory
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.SystemCategoryOverride
import com.example.cashia.models.UpcomingTransaction

@Database(
    entities = [MpesaTransactions::class, CustomCategory::class, SystemCategoryOverride::class, UpcomingTransaction::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun systemCategoryOverrideDao(): SystemCategoryOverrideDao
    abstract fun upcomingTransactionDao(): UpcomingTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE MpesaTransactions ADD COLUMN category TEXT NOT NULL DEFAULT 'OTHER'"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS CustomCategories (
                        id TEXT NOT NULL PRIMARY KEY,
                        label TEXT NOT NULL,
                        emoji TEXT NOT NULL,
                        keywords TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS SystemCategoryOverrides (
                        id TEXT NOT NULL PRIMARY KEY,
                        label TEXT NOT NULL,
                        emoji TEXT NOT NULL
                    )"""
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS UpcomingTransactions (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        categoryId TEXT NOT NULL,
                        dueDate TEXT NOT NULL,
                        type TEXT NOT NULL,
                        recurrence TEXT NOT NULL,
                        notes TEXT NOT NULL DEFAULT '',
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        createdAt INTEGER NOT NULL,
                        matchedTransactionId TEXT
                    )"""
                )
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_MpesaTransactions_timestamp ON MpesaTransactions(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_MpesaTransactions_date ON MpesaTransactions(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_MpesaTransactions_type ON MpesaTransactions(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_MpesaTransactions_category ON MpesaTransactions(category)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pesa_pulse_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}