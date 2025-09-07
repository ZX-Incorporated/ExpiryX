package com.expiryx.app

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "history_table")
data class History(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int? = null,
    val productName: String,
    val expirationDate: Long?,
    val quantity: Int = 1,
    val weight: Int? = null, // Changed from String? to Int?
    val weightUnit: String = "g", // "g" for grams or "ml" for milliliters
    val brand: String? = null,
    val imageUri: String? = null,
    val isFavorite: Boolean = false,
    val action: String, // "Expired", "Used", "Deleted"
    val timestamp: Long,
    val barcode: String? = null,
    val dateAdded: Long, // Should match Product's dateAdded (non-nullable generally)
    val dateModified: Long? = null
) : Parcelable