package com.characterfinder.app.ui.detail

import com.characterfinder.domain.model.Character
import com.characterfinder.domain.result.DomainError

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Error(val error: DomainError) : DetailUiState
    data class Success(val character: Character) : DetailUiState
}
