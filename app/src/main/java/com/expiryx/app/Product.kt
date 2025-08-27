package com.expiryx.app

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

enum class ProductStatus {
    ACTIVE,
    CONSUMED,
    EXPIRED
}

@Parcelize
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val expirationDate: Long?,
    val quantity: Int = 1,
    val reminderDays: Int = 0,
    val notes: String? = null,
    val weight: String? = null,
    val imageUri: String? = null,
    val isFavorite: Boolean = false,
    val status: ProductStatus = ProductStatus.ACTIVE // 🆕 new field
) : Parcelable
