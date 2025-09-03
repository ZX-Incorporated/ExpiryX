// app/src/main/java/com/expiryx/app/History.kt
package com.expiryx.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_table")
data class History(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productName: String,
    val action: String,        // "Used" or "Deleted"
    val timestamp: Long,       // System.currentTimeMillis()
    val quantity: Int,
    val weight: String? = null
)
