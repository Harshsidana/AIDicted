package com.ai.aidicted.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.aidicted.data.model.NewsArticle
import com.ai.aidicted.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewsFeedState(
    val articles: List<NewsArticle> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NewsFeedViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NewsFeedState())
    val state: StateFlow<NewsFeedState> = _state.asStateFlow()

    init {
        loadArticles()
        observeArticles()
    }

    private fun observeArticles() {
        viewModelScope.launch {
            repository.getAllArticles().collect { articles ->
                _state.update { it.copy(articles = articles, isLoading = false) }
            }
        }
    }

    fun loadArticles() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = _state.value.articles.isEmpty()) }
            val success = repository.refreshArticles()
            _state.update {
                it.copy(
                    isLoading = false,
                    error = if (!success && it.articles.isEmpty()) "Unable to load news. Check your connection." else null
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            repository.refreshArticles()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun toggleFavorite(articleId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(articleId)
        }
    }
}
