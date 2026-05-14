package com.example.cashia

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.cashia.daos.CustomCategoryDao
import com.example.cashia.daos.SyncStateDao
import com.example.cashia.daos.SystemCategoryOverrideDao
import com.example.cashia.daos.TransactionDao
import com.example.cashia.daos.UnmatchedSmsDao
import com.example.cashia.daos.UpcomingTransactionDao
import com.example.cashia.daos.UserCorrectionDao
import com.example.cashia.models.CustomCategory
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.SyncState
import com.example.cashia.models.SystemCategoryOverride
import com.example.cashia.models.UnmatchedSms
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.models.UserCorrection

@Database(
    entities = [MpesaTransactions::class, CustomCategory::class, SystemCategoryOverride::class, UpcomingTransaction::class, UnmatchedSms::class, UserCorrection::class, SyncState::class],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun systemCategoryOverrideDao(): SystemCategoryOverrideDao
    abstract fun upcomingTransactionDao(): UpcomingTransactionDao
    abstract fun unmatchedSmsDao(): UnmatchedSmsDao
    abstract fun userCorrectionDao(): UserCorrectionDao
    abstract fun syncStateDao(): SyncStateDao

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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS UnmatchedSms (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                rawBody TEXT NOT NULL,
                receivedAt INTEGER NOT NULL,
                reviewed INTEGER NOT NULL DEFAULT 0
            )
        """
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE UnmatchedSms ADD COLUMN contentHash TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE UnmatchedSms ADD COLUMN parserVersion INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE UnmatchedSms ADD COLUMN confidence REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE UnmatchedSms ADD COLUMN extractedFields TEXT")
                db.execSQL("ALTER TABLE UnmatchedSms ADD COLUMN patternGroup TEXT")

                // Indexes to support the queries added in the new DAO
                db.execSQL("CREATE INDEX IF NOT EXISTS index_UnmatchedSms_contentHash ON UnmatchedSms(contentHash)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_UnmatchedSms_parserVersion ON UnmatchedSms(parserVersion)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_UnmatchedSms_confidence ON UnmatchedSms(confidence)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_UnmatchedSms_reviewed ON UnmatchedSms(reviewed)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // 1. Create new table WITHOUT 'reviewed'
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS UnmatchedSms_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                rawBody TEXT NOT NULL,
                receivedAt INTEGER NOT NULL,
                status TEXT NOT NULL,
                contentHash TEXT NOT NULL,
                parserVersion INTEGER NOT NULL,
                confidence REAL NOT NULL,
                extractedFields TEXT,
                patternGroup TEXT
            )
        """
                )

                // 2. Copy + transform data
                db.execSQL(
                    """
            INSERT INTO UnmatchedSms_new (
                id,
                rawBody,
                receivedAt,
                status,
                contentHash,
                parserVersion,
                confidence,
                extractedFields,
                patternGroup
            )
            SELECT
                id,
                rawBody,
                receivedAt,
                CASE 
                    WHEN reviewed = 1 THEN 'SKIPPED'
                    ELSE 'PENDING'
                END,
                contentHash,
                parserVersion,
                confidence,
                extractedFields,
                patternGroup
            FROM UnmatchedSms
        """
                )

                db.execSQL("DROP TABLE UnmatchedSms")

                db.execSQL("ALTER TABLE UnmatchedSms_new RENAME TO UnmatchedSms")

                // 5. Recreate indexes
                db.execSQL("CREATE INDEX IF NOT EXISTS index_UnmatchedSms_status ON UnmatchedSms(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_UnmatchedSms_contentHash ON UnmatchedSms(contentHash)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_UnmatchedSms_parserVersion ON UnmatchedSms(parserVersion)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_UnmatchedSms_confidence ON UnmatchedSms(confidence)")
            }
        }
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add parentId column to CustomCategories (nullable, default null)
                db.execSQL(
                    "ALTER TABLE CustomCategories ADD COLUMN parentId TEXT"
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""CREATE TABLE IF NOT EXISTS `UserCorrections` (`recipient`   TEXT    NOT NULL,`categoryId`  TEXT    NOT NULL,`hitCount`    INTEGER NOT NULL DEFAULT 1,`updatedAt`   INTEGER NOT NULL,PRIMARY KEY(`recipient`))""")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {

                // SyncState table
                database.execSQL("""CREATE TABLE IF NOT EXISTS SyncState ( `key` TEXT NOT NULL PRIMARY KEY, `lastSmsId` INTEGER NOT NULL)""")

                // Add smsId to UnmatchedSms
                database.execSQL("""ALTER TABLE UnmatchedSms ADD COLUMN smsId INTEGER NOT NULL DEFAULT 0""")

                // Add smsId to MpesaTransactions
                database.execSQL("""ALTER TABLE MpesaTransactions ADD COLUMN smsId INTEGER NOT NULL DEFAULT 0""")

                // Unique index for SMS deduplication
                database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS index_MpesaTransactions_smsId ON MpesaTransactions(smsId)""")

                database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS index_UnmatchedSms_smsId ON UnmatchedSms(smsId)""")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pesa_pulse_db"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}