// app/src/main/java/com/expiryx/app/ProductDao.kt
package com.expiryx.app

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT * FROM product_table ORDER BY expirationDate ASC")
    fun getAllProducts(): LiveData<List<Product>>

    @Query("SELECT * FROM product_table WHERE isFavorite = 1 ORDER BY expirationDate ASC")
    fun getFavoriteProducts(): LiveData<List<Product>>

    // ✅ synchronous list query
    @Query("SELECT * FROM product_table ORDER BY expirationDate ASC")
    suspend fun getAllProductsNow(): List<Product>
}

