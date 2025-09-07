package com.expiryx.app

import android.database.Cursor
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    /**
     * Helper function to robustly check if a column exists in a table.
     * This prevents crashes if a migration tries to add a column that already exists.
     */
    private fun columnExists(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
        try {
            // Query the table's schema information, pass emptyArray for no bindArgs
            // Use the 'use' block for automatic cursor management (closing)
            db.query("PRAGMA table_info($tableName)", emptyArray<Any?>())?.use { cursor ->
                // Inside this 'use' block, 'cursor' is non-null.
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex >= 0) { // Check if 'name' column was found in the pragma result
                    while (cursor.moveToNext()) {
                        // Check if the current column name matches the one we're looking for
                        if (columnName.equals(cursor.getString(nameIndex), ignoreCase = true)) {
                            return true // Column found
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Optional: Log the exception if needed, e.g., using android.util.Log
            // For now, if any error occurs, we'll assume the column doesn't exist or the check failed.
        }
        return false // Column not found or an error occurred
    }

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Safely add new columns to product_table, checking if they exist first.
            if (!columnExists(database, "product_table", "barcode")) {
                database.execSQL("ALTER TABLE product_table ADD COLUMN barcode TEXT")
            }
            if (!columnExists(database, "product_table", "dateAdded")) {
                database.execSQL("ALTER TABLE product_table ADD COLUMN dateAdded INTEGER NOT NULL DEFAULT 0")
            }
            if (!columnExists(database, "product_table", "dateModified")) {
                database.execSQL("ALTER TABLE product_table ADD COLUMN dateModified INTEGER")
            }
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // PRODUCT TABLE: Change weight from TEXT to INTEGER
            database.execSQL("""                CREATE TABLE product_table_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    expirationDate INTEGER,
                    brand TEXT,
                    weight INTEGER,
                    imageUri TEXT,
                    reminderDays INTEGER NOT NULL,
                    isFavorite INTEGER NOT NULL,
                    barcode TEXT,
                    dateAdded INTEGER NOT NULL,
                    dateModified INTEGER
                )
            """)
            database.execSQL("""                INSERT INTO product_table_new (id, name, quantity, expirationDate, brand, weight, imageUri, reminderDays, isFavorite, barcode, dateAdded, dateModified)
                SELECT id, name, quantity, expirationDate, brand, 
                    CASE 
                        WHEN weight IS NULL OR weight = '' THEN NULL
                        ELSE CAST(weight AS INTEGER)
                    END, 
                    imageUri, reminderDays, isFavorite, barcode, dateAdded, dateModified
                FROM product_table
            """)
            database.execSQL("DROP TABLE product_table")
            database.execSQL("ALTER TABLE product_table_new RENAME TO product_table")

            // HISTORY TABLE: Change weight from TEXT to INTEGER
            database.execSQL("""                CREATE TABLE history_table_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    productId INTEGER,
                    productName TEXT NOT NULL,
                    expirationDate INTEGER,
                    quantity INTEGER NOT NULL,
                    weight INTEGER,
                    brand TEXT,
                    imageUri TEXT,
                    isFavorite INTEGER NOT NULL,
                    action TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    barcode TEXT,
                    dateAdded INTEGER NOT NULL,
                    dateModified INTEGER
                )
            """)
            database.execSQL("""                INSERT INTO history_table_new (id, productId, productName, expirationDate, quantity, weight, brand, imageUri, isFavorite, action, timestamp, barcode, dateAdded, dateModified)
                SELECT id, productId, productName, expirationDate, quantity, 
                    CASE 
                        WHEN weight IS NULL OR weight = '' THEN NULL
                        ELSE CAST(weight AS INTEGER)
                    END, 
                    brand, imageUri, isFavorite, action, timestamp, barcode, dateAdded, dateModified
                FROM history_table
            """)
            database.execSQL("DROP TABLE history_table")
            database.execSQL("ALTER TABLE history_table_new RENAME TO history_table")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Safely add weightUnit column to both tables, checking if it exists first.
            if (!columnExists(database, "product_table", "weightUnit")) {
                database.execSQL("ALTER TABLE product_table ADD COLUMN weightUnit TEXT NOT NULL DEFAULT 'g'")
            }
            if (!columnExists(database, "history_table", "weightUnit")) {
                database.execSQL("ALTER TABLE history_table ADD COLUMN weightUnit TEXT NOT NULL DEFAULT 'g'")
            }
        }
    }
}
