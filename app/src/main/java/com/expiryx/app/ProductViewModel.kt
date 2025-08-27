package com.expiryx.app

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    val allProducts: LiveData<List<Product>> = repository.allProducts
    val favoriteProducts: LiveData<List<Product>> = repository.favoriteProducts
    val history: LiveData<List<Product>> = repository.history

    fun insert(product: Product) = viewModelScope.launch {
        repository.insert(product)
    }

    fun update(product: Product) = viewModelScope.launch {
        repository.update(product)
    }

    fun delete(product: Product) = viewModelScope.launch {
        repository.delete(product)
    }
}

class ProductViewModelFactory(private val repository: ProductRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
