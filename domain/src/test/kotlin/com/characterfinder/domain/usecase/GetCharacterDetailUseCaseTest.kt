package com.characterfinder.domain.usecase

import com.characterfinder.domain.model.Character
import com.characterfinder.domain.repository.CharacterRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetCharacterDetailUseCaseTest {

    private class FakeRepository : CharacterRepository {
        val requestedIds = mutableListOf<Int>()
        var character: Character = characterStub(1, "Rick Sanchez")

        override suspend fun search(query: String): List<Character> = error("sin uso en estos tests")

        override suspend fun getCharacter(id: Int): Character {
            requestedIds += id
            return character
        }
    }

    @Test
    fun `pide al repositorio el personaje con el id solicitado`() = runTest {
        val repository = FakeRepository()
        val useCase = GetCharacterDetailUseCase(repository)

        val result = useCase(42)

        assertEquals(listOf(42), repository.requestedIds)
        assertEquals(repository.character, result)
    }
}
