package com.characterfinder.domain.usecase

import com.characterfinder.domain.model.Character
import com.characterfinder.domain.repository.CharacterRepository

class GetCharacterDetailUseCase(private val repository: CharacterRepository) {

    suspend operator fun invoke(id: Int): Character = repository.getCharacter(id)
}
