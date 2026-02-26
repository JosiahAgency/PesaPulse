package com.example.cashia

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.cashia.daos.CategoryDao
import com.example.cashia.daos.TransactionDao
import com.example.cashia.models.MpesaTransactions

@Database(entities = [MpesaTransactions::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
	   abstract fun transactionDao(): TransactionDao
	   abstract fun categoryDao(): CategoryDao
	   
	   companion object {
			@Volatile
			private var INSTANCE: AppDatabase? = null
			
			fun getDatabase(context: Context): AppDatabase {
				   return INSTANCE ?: synchronized(this) {
						Room.databaseBuilder(
							   context, AppDatabase::class.java, "pesa_pulse_db"
						).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
				   }
			}
	   }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
	   override fun migrate(db: SupportSQLiteDatabase) {
			db.execSQL(
				   """
            CREATE TABLE IF NOT EXISTS Categories (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                emoji TEXT NOT NULL DEFAULT '📦',
                keywords TEXT NOT NULL DEFAULT '',
                isDefault INTEGER NOT NULL DEFAULT 0
            )
        """
			)
	   }
}