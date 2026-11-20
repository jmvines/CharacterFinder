package com.characterfinder.domain.usecase

import com.characterfinder.domain.model.Character
import com.characterfinder.domain.model.CharacterStatus
import com.characterfinder.domain.model.SearchQuery
import com.characterfinder.domain.repository.CharacterRepository
import com.characterfinder.domain.result.DomainError
import com.characterfinder.domain.result.Either

class SearchCharactersUseCase(private val repository: CharacterRepository) {

    suspend operator fun invoke(
        query: SearchQuery,
        status: CharacterStatus? = null,
    ): Either<DomainError, List<Character>> {
        if (!query.isValid) return Either.Right(emptyList())
        return repository.search(query.normalized, status)
    }
}
