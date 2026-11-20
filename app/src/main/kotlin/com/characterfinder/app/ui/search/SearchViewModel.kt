package com.characterfinder.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.characterfinder.domain.model.CharacterStatus
import com.characterfinder.domain.model.SearchQuery
import com.characterfinder.domain.result.DomainError
import com.characterfinder.domain.result.fold
import com.characterfinder.domain.usecase.SearchCharactersUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(
    private val searchCharacters: SearchCharactersUseCase,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _statusFilter = MutableStateFlow<CharacterStatus?>(null)
    val statusFilter: StateFlow<CharacterStatus?> = _statusFilter.asStateFlow()

    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<SearchUiState> = merge(
        combine(_query.debounce(DEBOUNCE_MILLIS), _statusFilter, ::SearchRequest),
        retryTrigger.map { SearchRequest(_query.value, _statusFilter.value) },
    )
        .flatMapLatest { (query, status) ->
            val searchQuery = SearchQuery(query)
            if (!searchQuery.isValid) {
                flowOf(SearchUiState.Idle)
            } else {
                flow {
                    emit(SearchUiState.Loading)
                    val state = searchCharacters(searchQuery, status).fold(
                        ifLeft = { SearchUiState.Error(it) },
                        ifRight = { characters ->
                            if (characters.isEmpty()) SearchUiState.Empty
                            else SearchUiState.Success(characters)
                        },
                    )
                    emit(state)
                }.catch { emit(SearchUiState.Error(DomainError.Unexpected)) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SearchUiState.Idle)

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onStatusFilterChange(status: CharacterStatus?) {
        _statusFilter.value = status
    }

    fun retry() {
        retryTrigger.tryEmit(Unit)
    }

    private data class SearchRequest(val query: String, val status: CharacterStatus?)

    private companion object {
        const val DEBOUNCE_MILLIS = 300L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
