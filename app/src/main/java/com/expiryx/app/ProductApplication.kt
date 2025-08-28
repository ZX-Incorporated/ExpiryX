package com.expiryx.app

import android.app.Application

class ProductApplication : Application() {
    // Explicit types avoid lazy delegate inference issues
    val database: ProductDatabase by lazy { ProductDatabase.getDatabase(this) }
    val repository: ProductRepository by lazy { ProductRepository(database.productDao()) }
}
