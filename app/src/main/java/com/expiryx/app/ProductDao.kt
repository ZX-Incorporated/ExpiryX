package com.expiryx.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ProductDao {
    @Insert
    suspend fun insert(product: Product)

    @Query("SELECT * FROM products")
    suspend fun getAll(): List<Product>

    @Query("DELETE FROM products")
    suspend fun clear()
}
