package com.expiryx.app

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val expirationDate: Long,
    val quantity: Int = 1,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val reminderDays: Int = 0,
    val weight: String? = null,
    val imageUri: String? = null
) : Parcelable
