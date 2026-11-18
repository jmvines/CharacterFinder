package com.characterfinder.domain.usecase

import com.characterfinder.domain.model.Character
import com.characterfinder.domain.repository.CharacterRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCharactersUseCaseTest {

    private class FakeRepository : CharacterRepository {
        val searchedQueries = mutableListOf<String>()
        var result: List<Character> = emptyList()

        override suspend fun search(query: String): List<Character> {
            searchedQueries += query
            return result
        }

        override suspend fun getCharacter(id: Int): Character = error("sin uso en estos tests")
    }

    private val repository = FakeRepository()
    private val useCase = SearchCharactersUseCase(repository)

    @Test
    fun `con query en blanco devuelve vacio sin tocar el repositorio`() = runTest {
        val result = useCase("   ")

        assertTrue(result.isEmpty())
        assertTrue(repository.searchedQueries.isEmpty())
    }

    @Test
    fun `recorta los espacios antes de delegar en el repositorio`() = runTest {
        useCase("  rick  ")

        assertEquals(listOf("rick"), repository.searchedQueries)
    }

    @Test
    fun `devuelve lo que entrega el repositorio`() = runTest {
        val characters = listOf(characterStub(1, "Rick Sanchez"), characterStub(2, "Rick Prime"))
        repository.result = characters

        assertEquals(characters, useCase("rick"))
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
