package com.expiryx.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val expirationDate: Long,
    val quantity: Int,
    val reminderDays: Int,
    val weight: String,
    val notes: String?,
    val isFavorite: Boolean
)
