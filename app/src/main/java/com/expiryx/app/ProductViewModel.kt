package com.expiryx.app

import androidx.lifecycle.*

import kotlinx.coroutines.launch

class ProductViewModel(private val productDao: ProductDao) : ViewModel() {
    val allProducts: LiveData<List<Product>> = productDao.getAll()

    fun insert(product: Product) = viewModelScope.launch {
        productDao.insert(product)
    }
}

class ProductViewModelFactory(private val dao: ProductDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
