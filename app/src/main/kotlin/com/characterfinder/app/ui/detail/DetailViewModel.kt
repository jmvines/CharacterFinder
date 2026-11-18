package com.characterfinder.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.characterfinder.domain.usecase.GetCharacterDetailUseCase
import kotlinx.coroutines.CancellationException
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
            _uiState.value = try {
                DetailUiState.Success(getCharacterDetail(characterId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DetailUiState.Error
            }
        }
    }
}
