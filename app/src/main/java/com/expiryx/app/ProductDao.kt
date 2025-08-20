package com.expiryx.app

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query


@Dao
interface ProductDao {
    @Insert
    suspend fun insert(product: Product)

    @Query("SELECT * FROM products ORDER BY expirationDate ASC")
    fun getAll(): LiveData<List<Product>>   // <- must import androidx.lifecycle.LiveData
}

