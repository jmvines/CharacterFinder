package com.characterfinder.app.ui.detail

import com.characterfinder.domain.model.Character

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object Error : DetailUiState
    data class Success(val character: Character) : DetailUiState
}
