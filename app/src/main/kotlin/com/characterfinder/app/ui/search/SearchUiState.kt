package com.characterfinder.app.ui.search

import com.characterfinder.domain.model.Character
import com.characterfinder.domain.result.DomainError

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data object Empty : SearchUiState
    data class Error(val error: DomainError) : SearchUiState
    data class Success(val characters: List<Character>) : SearchUiState
}
