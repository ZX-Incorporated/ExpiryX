package com.expiryx.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_table")
data class History(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // Core product info
    val productId: Int? = null, // optional reference back to original product
    val productName: String,
    val expirationDate: Long?,   // same as Product.expirationDate
    val quantity: Int = 1,
    val weight: String? = null,
    val notes: String? = null,
    val imageUri: String? = null,
    val isFavorite: Boolean = false,

    // Action meta
    val action: String,          // "Added", "Deleted", "Used", "Expired"
    val timestamp: Long          // when action happened
)
