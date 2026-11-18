package com.characterfinder.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.characterfinder.domain.usecase.SearchCharactersUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(
    private val searchCharacters: SearchCharactersUseCase,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Cancelación de búsquedas en curso: [flatMapLatest] cancela estructuradamente la corrutina
     * de la búsqueda anterior en cuanto el query emite un valor nuevo, lo que aborta también la
     * llamada HTTP de Retrofit. Nunca se publican resultados de una búsqueda obsoleta.
     */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<SearchUiState> = _query
        .debounce(DEBOUNCE_MILLIS)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(SearchUiState.Idle)
            } else {
                flow {
                    emit(SearchUiState.Loading)
                    val characters = searchCharacters(query)
                    emit(
                        if (characters.isEmpty()) SearchUiState.Empty
                        else SearchUiState.Success(characters),
                    )
                }.catch { emit(SearchUiState.Error) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState.Idle)

    fun onQueryChange(value: String) {
        _query.value = value
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 300L
    }
}
