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
        // ✅ Log history on save
        historyDao.insert(
            History(
                productName = product.name,
                action = "Added",
                timestamp = System.currentTimeMillis(),
                quantity = product.quantity,
                weight = product.weight
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
                productName = product.name,
                action = "Deleted",
                timestamp = System.currentTimeMillis(),
                quantity = product.quantity,
                weight = product.weight
            )
        )
    }

    suspend fun markAsUsed(product: Product) {
        productDao.delete(product) // remove from active list
        historyDao.insert(
            History(
                productName = product.name,
                action = "Used",
                timestamp = System.currentTimeMillis(),
                quantity = product.quantity,
                weight = product.weight
            )
        )
    }

    // Called at app start to auto-move expired items
    suspend fun archiveExpiredProducts() {
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L

        // ✅ now suspend call, runs off main thread automatically
        val products = productDao.getAllProductsNow()

        products.forEach { product ->
            val expiry = product.expirationDate ?: return@forEach
            if (now - expiry >= sevenDaysMs) {
                productDao.delete(product)
                historyDao.insert(
                    History(
                        productName = product.name,
                        action = "Expired",
                        timestamp = System.currentTimeMillis(),
                        quantity = product.quantity,
                        weight = product.weight
                    )
                )
            }
        }
    }


    suspend fun getAllProductsNow(): List<Product> = productDao.getAllProductsNow()
}
