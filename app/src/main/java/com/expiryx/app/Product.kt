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
    val reminderDays: Int = 3,
    val isFavorite: Boolean = false
    // ✅ FIX: Removed the 'isUsed' field as it's no longer needed.
    // Used products are now moved directly to the history table.
) : Parcelable