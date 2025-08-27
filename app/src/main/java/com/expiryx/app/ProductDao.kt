package com.expiryx.app

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE status = 'ACTIVE' ORDER BY expirationDate ASC")
    fun getAllProducts(): LiveData<List<Product>>

    @Query("SELECT * FROM products WHERE isFavorite = 1 AND status = 'ACTIVE'")
    fun getFavoriteProducts(): LiveData<List<Product>>

    @Query("SELECT * FROM products WHERE status != 'ACTIVE' ORDER BY expirationDate DESC")
    fun getHistory(): LiveData<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)
}
