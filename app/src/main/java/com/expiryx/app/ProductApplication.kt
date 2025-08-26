package com.expiryx.app

import android.app.Application

class ProductApplication : Application() {
    val database by lazy { ProductDatabase.getDatabase(this) }
    val repository by lazy { ProductRepository(database.productDao()) }
}
