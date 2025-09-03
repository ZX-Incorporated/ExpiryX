// app/src/main/java/com/expiryx/app/ProductRepository.kt
package com.expiryx.app

import androidx.lifecycle.LiveData

class ProductRepository(
    private val productDao: ProductDao,
    private val historyDao: HistoryDao
) {
    val allProducts: LiveData<List<Product>> = productDao.getAllProducts()
    val allHistory: LiveData<List<History>> = historyDao.getAllHistory()

    suspend fun insertProduct(product: Product) {
        productDao.insert(product)
        historyDao.insert(
            History(
                productId = product.id,
                productName = product.name,
                expirationDate = product.expirationDate,
                quantity = product.quantity,
                weight = product.weight,
                notes = product.notes,
                imageUri = product.imageUri,
                isFavorite = product.isFavorite,
                action = "Added",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun update(product: Product) {
        productDao.update(product)
    }

    suspend fun insertHistory(history: History) {
        historyDao.insert(history)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.delete(product)
        historyDao.insert(
            History(
                productId = product.id,
                productName = product.name,
                expirationDate = product.expirationDate,
                quantity = product.quantity,
                weight = product.weight,
                notes = product.notes,
                imageUri = product.imageUri,
                isFavorite = product.isFavorite,
                action = "Deleted",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun markAsUsed(product: Product) {
        productDao.delete(product)
        historyDao.insert(
            History(
                productId = product.id,
                productName = product.name,
                expirationDate = product.expirationDate,
                quantity = product.quantity,
                weight = product.weight,
                notes = product.notes,
                imageUri = product.imageUri,
                isFavorite = product.isFavorite,
                action = "Used",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun archiveExpiredProducts() {
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L

        val products = productDao.getAllProductsNow()

        products.forEach { product ->
            val expiry = product.expirationDate ?: return@forEach
            if (now - expiry >= sevenDaysMs) {
                productDao.delete(product)
                historyDao.insert(
                    History(
                        productId = product.id,
                        productName = product.name,
                        expirationDate = product.expirationDate,
                        quantity = product.quantity,
                        weight = product.weight,
                        notes = product.notes,
                        imageUri = product.imageUri,
                        isFavorite = product.isFavorite,
                        action = "Expired",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun getAllProductsNow(): List<Product> = productDao.getAllProductsNow()
}
