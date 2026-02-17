package com.ai.aidicted.ui.favorites

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

data class FavoritesState(
    val favorites: List<NewsArticle> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesState())
    val state: StateFlow<FavoritesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getFavoriteArticles().collect { favorites ->
                _state.update { it.copy(favorites = favorites, isLoading = false) }
            }
        }
    }

    fun toggleFavorite(articleId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(articleId)
        }
    }
}
