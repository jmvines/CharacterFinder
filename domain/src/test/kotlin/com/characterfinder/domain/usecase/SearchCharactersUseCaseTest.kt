package com.characterfinder.domain.usecase

import com.characterfinder.domain.model.Character
import com.characterfinder.domain.model.CharacterStatus
import com.characterfinder.domain.model.SearchQuery
import com.characterfinder.domain.repository.CharacterRepository
import com.characterfinder.domain.result.DomainError
import com.characterfinder.domain.result.Either
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCharactersUseCaseTest {

    private class FakeRepository : CharacterRepository {
        val searchedQueries = mutableListOf<String>()
        val searchedStatuses = mutableListOf<CharacterStatus?>()
        var result: Either<DomainError, List<Character>> = Either.Right(emptyList())

        override suspend fun search(
            query: String,
            status: CharacterStatus?,
        ): Either<DomainError, List<Character>> {
            searchedQueries += query
            searchedStatuses += status
            return result
        }

        override suspend fun getCharacter(id: Int): Either<DomainError, Character> =
            error("sin uso en estos tests")
    }

    private val repository = FakeRepository()
    private val useCase = SearchCharactersUseCase(repository)

    @Test
    fun `con query en blanco devuelve vacio sin tocar el repositorio`() = runTest {
        val result = useCase(SearchQuery("   "))

        assertEquals(Either.Right(emptyList<Character>()), result)
        assertTrue(repository.searchedQueries.isEmpty())
    }

    @Test
    fun `recorta los espacios antes de delegar en el repositorio`() = runTest {
        useCase(SearchQuery("  rick  "))

        assertEquals(listOf("rick"), repository.searchedQueries)
    }

    @Test
    fun `propaga el filtro de estado al repositorio y por defecto no filtra`() = runTest {
        useCase(SearchQuery("rick"))
        useCase(SearchQuery("rick"), CharacterStatus.ALIVE)

        assertEquals(listOf(null, CharacterStatus.ALIVE), repository.searchedStatuses)
    }

    @Test
    fun `devuelve lo que entrega el repositorio`() = runTest {
        val characters = listOf(characterStub(1, "Rick Sanchez"), characterStub(2, "Rick Prime"))
        repository.result = Either.Right(characters)

        assertEquals(Either.Right(characters), useCase(SearchQuery("rick")))
    }
}

internal fun characterStub(id: Int, name: String) = Character(
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
