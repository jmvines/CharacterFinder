package com.characterfinder.app.ui.search

import com.characterfinder.domain.model.Character

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data object Empty : SearchUiState
    data object Error : SearchUiState
    data class Success(val characters: List<Character>) : SearchUiState
}
