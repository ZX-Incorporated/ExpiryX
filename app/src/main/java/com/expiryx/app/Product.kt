package com.expiryx.app

import android.os.Parcelable
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
    val weight: String? = null,
    val imageUri: String? = null,
    val reminderDays: Int = 7, // Updated default to 7 days
    val isFavorite: Boolean = false,
    val barcode: String? = null, // Barcode from scanning or image upload
    val dateAdded: Long = System.currentTimeMillis(), // When product was first created
    val dateModified: Long? = null // When product was last updated (null for new products)
    // ✅ FIX: Removed the 'isUsed' field as it's no longer needed.
    // Used products are now moved directly to the history table.
) : Parcelable