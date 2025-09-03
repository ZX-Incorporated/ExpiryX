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

    // ✅ All products ordered by expiry date
    @Query("SELECT * FROM product_table ORDER BY expirationDate ASC")
    fun getAllProducts(): LiveData<List<Product>>

    // ✅ Favorites only
    @Query("SELECT * FROM product_table WHERE isFavorite = 1 ORDER BY expirationDate ASC")
    fun getFavoriteProducts(): LiveData<List<Product>>

    // ✅ Immediate snapshot (not LiveData)
    @Query("SELECT * FROM product_table ORDER BY expirationDate ASC")
    fun getAllProductsNow(): List<Product>

    // ✅ History items (used OR expired before cutoff)
    @Query("SELECT * FROM product_table WHERE isUsed = 1 OR (expirationDate IS NOT NULL AND expirationDate < :cutoff)")
    fun getHistoryProducts(cutoff: Long): LiveData<List<Product>>
}
