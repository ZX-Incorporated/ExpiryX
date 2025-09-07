package com.expiryx.app

import android.os.Parcelable
import androidx.room.ColumnInfo // Ensure this import is present
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "product_table")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val quantity: Int = 1,
    val expirationDate: Long? = null,
    val brand: String? = null,
    @ColumnInfo(name = "weight") val weight: Int? = null, // Explicit ColumnInfo
    val weightUnit: String = "g", // "g" for grams or "ml" for milliliters
    val imageUri: String? = null,
    val reminderDays: Int = 7, // Updated default to 7 days
    val isFavorite: Boolean = false,
    val barcode: String? = null, // Barcode from scanning or image upload
    val dateAdded: Long = System.currentTimeMillis(), // When product was first created
    val dateModified: Long? = null // When product was last updated (null for new products)
) : Parcelable
// Trivial change to ensure recompilation