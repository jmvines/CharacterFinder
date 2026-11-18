package com.characterfinder.domain.repository

import com.characterfinder.domain.model.Character

interface CharacterRepository {

    /** Busca personajes por nombre. Devuelve lista vacía si no hay coincidencias. */
    suspend fun search(query: String): List<Character>

    suspend fun getCharacter(id: Int): Character
}
