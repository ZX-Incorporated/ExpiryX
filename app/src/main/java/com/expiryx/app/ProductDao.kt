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

    @Query("DELETE FROM product_table")
    suspend fun clearAllProducts()

    @Query("""
        SELECT * FROM product_table 
        ORDER BY 
            CASE WHEN expirationDate IS NULL THEN 1 ELSE 0 END,
            expirationDate ASC
    """)
    fun getAllProducts(): LiveData<List<Product>>

    @Query("SELECT * FROM product_table")
    suspend fun getAllProductsNow(): List<Product>

    @Query("SELECT * FROM product_table WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Int): Product?
}