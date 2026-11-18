package com.characterfinder.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.characterfinder.domain.result.fold
import com.characterfinder.domain.usecase.GetCharacterDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val getCharacterDetail: GetCharacterDetailUseCase,
    private val characterId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            _uiState.value = getCharacterDetail(characterId).fold(
                ifLeft = { DetailUiState.Error(it) },
                ifRight = { DetailUiState.Success(it) },
            )
        }
    }
}
