package com.expiryx.app

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: ProductRepository) : ViewModel() {

    val allHistory: LiveData<List<History>> = repository.allHistory

    fun permanentlyDelete(history: History) = viewModelScope.launch {
        repository.deleteHistoryEntry(history)
    }

    fun restoreDeleted(history: History) = viewModelScope.launch {
        repository.restoreFromHistory(history, asUsed = false)
    }

    fun unuse(history: History) = viewModelScope.launch {
        repository.restoreFromHistory(history, asUsed = true)
    }

    fun changeExpiry(history: History, newExpiry: Long) = viewModelScope.launch {
        repository.restoreWithNewExpiry(history, newExpiry)
    }
}

class HistoryViewModelFactory(private val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
