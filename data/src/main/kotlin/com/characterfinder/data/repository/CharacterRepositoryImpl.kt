package com.characterfinder.data.repository

import com.characterfinder.data.cache.SearchCache
import com.characterfinder.data.remote.RickAndMortyApi
import com.characterfinder.data.remote.toDomain
import com.characterfinder.domain.model.Character
import com.characterfinder.domain.repository.CharacterRepository
import retrofit2.HttpException

class CharacterRepositoryImpl(
    private val api: RickAndMortyApi,
    private val cache: SearchCache,
) : CharacterRepository {

    override suspend fun search(query: String): List<Character> {
        val key = query.trim().lowercase()
        cache.get(key)?.let { return it }

        val characters = try {
            api.searchCharacters(key).results.map { it.toDomain() }
        } catch (e: HttpException) {
            // La API de Rick & Morty responde 404 cuando la búsqueda no tiene coincidencias.
            if (e.code() == 404) emptyList() else throw e
        }
        cache.put(key, characters)
        return characters
    }

    override suspend fun getCharacter(id: Int): Character = api.getCharacter(id).toDomain()
}
