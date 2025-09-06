package com.expiryx.app

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    // Note: This migration appears to be a placeholder and may not be correctly
    // structured for migrating a live database, as the schema is incomplete.
    // It is not currently being used because the database is configured with
    // .fallbackToDestructiveMigration().
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS history_table (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    productName TEXT NOT NULL,
                    action TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    quantity INTEGER NOT NULL
                )
            """)
        }
    }
}