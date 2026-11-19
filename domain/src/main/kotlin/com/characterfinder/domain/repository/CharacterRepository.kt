package com.characterfinder.domain.repository

import com.characterfinder.domain.model.Character
import com.characterfinder.domain.model.CharacterStatus
import com.characterfinder.domain.result.DomainError
import com.characterfinder.domain.result.Either

interface CharacterRepository {

    /**
     * Busca personajes por nombre, opcionalmente filtrados por [status] (`null` = sin filtro).
     * [Either.Right] con la lista (vacía si no hay coincidencias), [Either.Left] con el
     * [DomainError] si falla.
     */
    suspend fun search(query: String, status: CharacterStatus? = null): Either<DomainError, List<Character>>

    suspend fun getCharacter(id: Int): Either<DomainError, Character>
}
