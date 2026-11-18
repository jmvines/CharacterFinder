package com.characterfinder.domain.usecase

import com.characterfinder.domain.model.Character
import com.characterfinder.domain.repository.CharacterRepository
import com.characterfinder.domain.result.DomainError
import com.characterfinder.domain.result.Either

class SearchCharactersUseCase(private val repository: CharacterRepository) {

    suspend operator fun invoke(query: String): Either<DomainError, List<Character>> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return Either.Right(emptyList())
        return repository.search(normalized)
    }
}
