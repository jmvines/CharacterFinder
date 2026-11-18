package com.characterfinder.domain.usecase

import com.characterfinder.domain.model.Character
import com.characterfinder.domain.repository.CharacterRepository
import com.characterfinder.domain.result.DomainError
import com.characterfinder.domain.result.Either

class GetCharacterDetailUseCase(private val repository: CharacterRepository) {

    suspend operator fun invoke(id: Int): Either<DomainError, Character> = repository.getCharacter(id)
}
