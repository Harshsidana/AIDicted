package com.ai.aidicted.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.aidicted.data.model.NewsArticle
import com.ai.aidicted.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val results: List<NewsArticle> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(results = emptyList(), hasSearched = false, isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _state.update { it.copy(isSearching = true) }
            repository.searchArticles(query).collect { results ->
                _state.update { it.copy(results = results, isSearching = false, hasSearched = true) }
            }
        }
    }
}
