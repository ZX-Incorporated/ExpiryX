package com.expiryx.app

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add new columns to product_table
            database.execSQL("ALTER TABLE product_table ADD COLUMN barcode TEXT")
            database.execSQL("ALTER TABLE product_table ADD COLUMN dateAdded INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            database.execSQL("ALTER TABLE product_table ADD COLUMN dateModified INTEGER")
        }
    }
}