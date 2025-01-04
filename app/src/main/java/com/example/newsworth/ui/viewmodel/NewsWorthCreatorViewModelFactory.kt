package com.example.newsworth.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.newsworth.repository.NewsWorthCreatorRepository

class NewsWorthCreatorViewModelFactory(private val repository: NewsWorthCreatorRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsWorthCreatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewsWorthCreatorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
