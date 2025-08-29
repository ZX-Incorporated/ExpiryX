package com.expiryx.app

import androidx.lifecycle.LiveData

class ProductRepository(private val productDao: ProductDao) {
    val allProducts: LiveData<List<Product>> = productDao.getAllProducts()
    val favoriteProducts: LiveData<List<Product>> = productDao.getFavoriteProducts()

    suspend fun insert(product: Product) = productDao.insert(product)
    suspend fun update(product: Product) = productDao.update(product)
    suspend fun delete(product: Product) = productDao.delete(product)

    fun getAllProductsNow(): List<Product> = productDao.getAllProductsNow()
}
