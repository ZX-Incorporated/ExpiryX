package com.expiryx.app

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    val allProducts: LiveData<List<Product>> = repository.allProducts

    fun insert(product: Product) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertProduct(product)
    }

    fun update(product: Product) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(product)
    }

    fun delete(product: Product) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteProduct(product)
    }

    fun markAsUsed(product: Product) = viewModelScope.launch(Dispatchers.IO) {
        repository.markAsUsed(product)
    }

    fun archiveExpiredProducts() = viewModelScope.launch(Dispatchers.IO) {
        repository.archiveExpiredProducts()
    }
}

class ProductViewModelFactory(
    private val repository: ProductRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}