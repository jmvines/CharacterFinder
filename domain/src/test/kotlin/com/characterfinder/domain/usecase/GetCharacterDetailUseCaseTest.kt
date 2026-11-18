package com.characterfinder.domain.usecase

import com.characterfinder.domain.model.Character
import com.characterfinder.domain.repository.CharacterRepository
import com.characterfinder.domain.result.DomainError
import com.characterfinder.domain.result.Either
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetCharacterDetailUseCaseTest {

    private class FakeRepository : CharacterRepository {
        val requestedIds = mutableListOf<Int>()
        var character: Either<DomainError, Character> = Either.Right(characterStub(1, "Rick Sanchez"))

        override suspend fun search(query: String): Either<DomainError, List<Character>> =
            error("sin uso en estos tests")

        override suspend fun getCharacter(id: Int): Either<DomainError, Character> {
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
