package com.characterfinder.app.ui.search

import app.cash.turbine.test
import com.characterfinder.app.MainDispatcherRule
import com.characterfinder.domain.model.Character
import com.characterfinder.domain.model.CharacterStatus
import com.characterfinder.domain.repository.CharacterRepository
import com.characterfinder.domain.result.DomainError
import com.characterfinder.domain.result.Either
import com.characterfinder.domain.usecase.SearchCharactersUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeCharacterRepository : CharacterRepository {
        var responseDelayMillis = 0L
        var resultForQuery: (String) -> Either<DomainError, List<Character>> = { query ->
            Either.Right(listOf(characterStub(1, query)))
        }
        val searchedQueries = mutableListOf<String>()
        val searchedStatuses = mutableListOf<CharacterStatus?>()
        val cancelledQueries = mutableListOf<String>()

        override suspend fun search(
            query: String,
            status: CharacterStatus?,
        ): Either<DomainError, List<Character>> {
            searchedQueries += query
            searchedStatuses += status
            try {
                delay(responseDelayMillis)
            } catch (e: CancellationException) {
                cancelledQueries += query
                throw e
            }
            return resultForQuery(query)
        }

        override suspend fun getCharacter(id: Int): Either<DomainError, Character> =
            Either.Right(characterStub(id, "sin uso"))
    }

    private val repository = FakeCharacterRepository()

    private fun viewModel() = SearchViewModel(SearchCharactersUseCase(repository))

    @Test
    fun `una busqueda nueva cancela la anterior y no publica sus resultados`() = runTest {
        repository.responseDelayMillis = 1_000
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())

            viewModel.onQueryChange("rick")
            advanceTimeBy(400)
            assertEquals(SearchUiState.Loading, awaitItem())

            viewModel.onQueryChange("morty")
            advanceTimeBy(2_000)

            val success = awaitItem() as SearchUiState.Success
            assertEquals(listOf("morty"), success.characters.map { it.name })
            assertEquals(listOf("rick"), repository.cancelledQueries)
            expectNoEvents()
        }
    }

    @Test
    fun `el debounce agrupa el tecleo en una sola busqueda`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())

            viewModel.onQueryChange("r")
            advanceTimeBy(100)
            viewModel.onQueryChange("ri")
            advanceTimeBy(100)
            viewModel.onQueryChange("rick")
            advanceTimeBy(400)

            assertEquals(SearchUiState.Loading, awaitItem())
            val success = awaitItem() as SearchUiState.Success
            assertEquals(listOf("rick"), success.characters.map { it.name })
            assertEquals(listOf("rick"), repository.searchedQueries)
        }
    }

    @Test
    fun `con query en blanco queda en Idle sin tocar el repositorio`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())

            viewModel.onQueryChange("   ")
            advanceTimeBy(400)

            expectNoEvents()
            assertTrue(repository.searchedQueries.isEmpty())
        }
    }

    @Test
    fun `sin coincidencias emite Empty`() = runTest {
        repository.resultForQuery = { Either.Right(emptyList()) }
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())

            viewModel.onQueryChange("nadie")
            advanceTimeBy(400)

            assertEquals(SearchUiState.Loading, awaitItem())
            assertEquals(SearchUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `reintentar tras un error relanza la misma busqueda sin esperar al debounce`() = runTest {
        var failing = true
        repository.resultForQuery = { query ->
            if (failing) Either.Left(DomainError.Unexpected) else Either.Right(listOf(characterStub(1, query)))
        }
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())

            viewModel.onQueryChange("rick")
            advanceTimeBy(400)
            assertEquals(SearchUiState.Loading, awaitItem())
            assertEquals(SearchUiState.Error(DomainError.Unexpected), awaitItem())

            failing = false
            viewModel.retry()

            assertEquals(SearchUiState.Loading, awaitItem())
            val success = awaitItem() as SearchUiState.Success
            assertEquals(listOf("rick"), success.characters.map { it.name })
            assertEquals(listOf("rick", "rick"), repository.searchedQueries)
        }
    }

    @Test
    fun `cambiar el filtro de estado relanza la busqueda cancelando la que esta en curso`() = runTest {
        repository.responseDelayMillis = 1_000
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())

            viewModel.onQueryChange("rick")
            advanceTimeBy(400)
            assertEquals(SearchUiState.Loading, awaitItem())

            viewModel.onStatusFilterChange(CharacterStatus.ALIVE)
            advanceTimeBy(2_000)

            awaitItem() as SearchUiState.Success
            assertEquals(listOf("rick"), repository.cancelledQueries)
            assertEquals(listOf(null, CharacterStatus.ALIVE), repository.searchedStatuses)
            expectNoEvents()
        }
    }

    @Test
    fun `cambiar el filtro con texto a medio escribir no lanza busqueda con el texto anterior`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())

            viewModel.onQueryChange("rick")
            advanceTimeBy(400)
            assertEquals(SearchUiState.Loading, awaitItem())
            awaitItem() as SearchUiState.Success

            viewModel.onQueryChange("ricky")
            advanceTimeBy(100)
            viewModel.onStatusFilterChange(CharacterStatus.ALIVE)
            advanceTimeBy(400)
            assertEquals(SearchUiState.Loading, awaitItem())
            awaitItem() as SearchUiState.Success

            assertEquals(listOf("rick", "ricky"), repository.searchedQueries)
            assertEquals(listOf(null, CharacterStatus.ALIVE), repository.searchedStatuses)
        }
    }

    @Test
    fun `el reintento tras un error conserva el filtro activo`() = runTest {
        var failing = true
        repository.resultForQuery = { query ->
            if (failing) Either.Left(DomainError.Server) else Either.Right(listOf(characterStub(1, query)))
        }
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())

            viewModel.onQueryChange("rick")
            viewModel.onStatusFilterChange(CharacterStatus.DEAD)
            advanceTimeBy(400)
            assertEquals(SearchUiState.Loading, awaitItem())
            assertEquals(SearchUiState.Error(DomainError.Server), awaitItem())

            failing = false
            viewModel.retry()

            assertEquals(SearchUiState.Loading, awaitItem())
            awaitItem() as SearchUiState.Success
            assertEquals(listOf(CharacterStatus.DEAD, CharacterStatus.DEAD), repository.searchedStatuses)
        }
    }

    @Test
    fun `un fallo de red emite Error con su tipo`() = runTest {
        repository.resultForQuery = { Either.Left(DomainError.Connectivity) }
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())

            viewModel.onQueryChange("rick")
            advanceTimeBy(400)

            assertEquals(SearchUiState.Loading, awaitItem())
            assertEquals(SearchUiState.Error(DomainError.Connectivity), awaitItem())
        }
    }
}

private fun characterStub(id: Int, name: String) = Character(
    id = id,
    name = name,
    status = "Alive",
    species = "Human",
    gender = "Male",
    imageUrl = "https://example.com/$id.jpeg",
    origin = "Earth (C-137)",
    lastLocation = "Citadel of Ricks",
    episodeCount = 51,
)
