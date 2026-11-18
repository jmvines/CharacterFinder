package com.characterfinder.domain.usecase

import com.characterfinder.domain.model.Character
import com.characterfinder.domain.repository.CharacterRepository

class SearchCharactersUseCase(private val repository: CharacterRepository) {

    suspend operator fun invoke(query: String): List<Character> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()
        return repository.search(normalized)
    }
}
