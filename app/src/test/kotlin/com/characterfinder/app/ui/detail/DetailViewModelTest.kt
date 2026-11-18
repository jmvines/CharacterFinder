package com.characterfinder.app.ui.detail

import app.cash.turbine.test
import com.characterfinder.app.MainDispatcherRule
import com.characterfinder.domain.model.Character
import com.characterfinder.domain.repository.CharacterRepository
import com.characterfinder.domain.usecase.GetCharacterDetailUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeCharacterRepository : CharacterRepository {
        var failing = false
        val requestedIds = mutableListOf<Int>()

        override suspend fun search(query: String): List<Character> = emptyList()

        override suspend fun getCharacter(id: Int): Character {
            requestedIds += id
            if (failing) error("fallo de red simulado")
            return characterStub(id)
        }
    }

    private val repository = FakeCharacterRepository()

    private fun viewModel(id: Int = 1) =
        DetailViewModel(GetCharacterDetailUseCase(repository), characterId = id)

    @Test
    fun `carga el personaje del id solicitado al crearse`() = runTest {
        val viewModel = viewModel(id = 7)

        viewModel.uiState.test {
            assertEquals(DetailUiState.Loading, awaitItem())

            val success = awaitItem() as DetailUiState.Success
            assertEquals(7, success.character.id)
            assertEquals(listOf(7), repository.requestedIds)
        }
    }

    @Test
    fun `un fallo al cargar emite Error`() = runTest {
        repository.failing = true
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(DetailUiState.Loading, awaitItem())
            assertEquals(DetailUiState.Error, awaitItem())
        }
    }

    @Test
    fun `reintentar tras un error vuelve a pedir el personaje y emite Success`() = runTest {
        repository.failing = true
        val viewModel = viewModel(id = 3)

        viewModel.uiState.test {
            assertEquals(DetailUiState.Loading, awaitItem())
            assertEquals(DetailUiState.Error, awaitItem())

            repository.failing = false
            viewModel.retry()

            assertEquals(DetailUiState.Loading, awaitItem())
            val success = awaitItem() as DetailUiState.Success
            assertEquals(3, success.character.id)
            assertEquals(listOf(3, 3), repository.requestedIds)
        }
    }
}

private fun characterStub(id: Int) = Character(
    id = id,
    name = "Rick Sanchez",
    status = "Alive",
    species = "Human",
    gender = "Male",
    imageUrl = "https://example.com/$id.jpeg",
    origin = "Earth (C-137)",
    lastLocation = "Citadel of Ricks",
    episodeCount = 51,
)
