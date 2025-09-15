package com.expiryx.app

import android.app.Application

class ProductApplication : Application() {
    val database: ProductDatabase by lazy { ProductDatabase.getDatabase(this) }
    val repository: ProductRepository by lazy {
        ProductRepository(database.productDao(), database.historyDao())
    }
    
    override fun onCreate() {
        super.onCreate()
        // Initialize theme based on user preference or system default
        ThemeManager.initializeTheme(this)
    }
}