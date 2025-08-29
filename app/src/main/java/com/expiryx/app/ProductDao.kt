package com.expiryx.app

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ProductDao {

    @Query("SELECT * FROM product_table ORDER BY expirationDate ASC")
    fun getAllProducts(): LiveData<List<Product>>

    @Query("SELECT * FROM product_table ORDER BY expirationDate ASC")
    fun getAllProductsNow(): List<Product> // blocking version

    @Query("SELECT * FROM product_table WHERE isFavorite = 1 ORDER BY expirationDate ASC")
    fun getFavoriteProducts(): LiveData<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)
}
