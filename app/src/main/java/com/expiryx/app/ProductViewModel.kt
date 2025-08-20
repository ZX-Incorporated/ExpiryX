package com.expiryx.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ProductViewModel(private val dao: ProductDao) : ViewModel() {

    val allProducts: LiveData<List<Product>> = dao.getAllProducts()

    fun insert(product: Product) = viewModelScope.launch {
        dao.insert(product)
    }

    fun update(product: Product) = viewModelScope.launch {
        dao.update(product)
    }

    fun delete(product: Product) = viewModelScope.launch {
        dao.delete(product)
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
