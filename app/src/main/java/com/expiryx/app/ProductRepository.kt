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
        val existing = historyDao.findByProductAndAction(product.id, "Used")
        if (existing == null) {
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
        productDao.delete(product)
    }

    suspend fun archiveExpiredProducts() {
        val now = System.currentTimeMillis()
        val all = productDao.getAllProductsNow()
        for (p in all) {
            val expiry = p.expirationDate ?: continue
            if (now - expiry >= 7 * 24 * 60 * 60 * 1000) {
                val existing = historyDao.findByProductAndAction(p.id, "Expired")
                if (existing == null) {
                    historyDao.insert(
                        History(
                            productId = p.id,
                            productName = p.name,
                            expirationDate = p.expirationDate,
                            quantity = p.quantity,
                            weight = p.weight,
                            notes = p.notes,
                            imageUri = p.imageUri,
                            isFavorite = p.isFavorite,
                            action = "Expired",
                            timestamp = now
                        )
                    )
                }
                productDao.delete(p)
            }
        }
    }

    suspend fun getAllProductsNow(): List<Product> = productDao.getAllProductsNow()
    suspend fun getAllHistoryNow(): List<History> = historyDao.getAllHistoryNow()

    // ✅ new methods used in SettingsActivity
    suspend fun clearAllProducts() = productDao.clearAllProducts()
    suspend fun clearAllHistory() = historyDao.clearAllHistory()
}
