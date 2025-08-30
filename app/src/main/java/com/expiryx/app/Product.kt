package com.expiryx.app

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "product_table") // keep consistent across DAO & DB
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val expirationDate: Long?, // in millis. can be null
    val quantity: Int = 1,
    val reminderDays: Int = 0, // user defined; used for future features if needed
    val notes: String? = null,
    val weight: String? = null,
    val imageUri: String? = null,
    val isFavorite: Boolean = false,
    val isUsed: Boolean = false // ✅ NEW FIELD
) : Parcelable
