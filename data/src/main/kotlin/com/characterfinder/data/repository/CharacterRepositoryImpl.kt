package com.characterfinder.data.repository

import com.characterfinder.data.cache.SearchCache
import com.characterfinder.data.remote.RickAndMortyApi
import com.characterfinder.data.remote.toDomain
import com.characterfinder.domain.model.Character
import com.characterfinder.domain.repository.CharacterRepository
import com.characterfinder.domain.result.DomainError
import com.characterfinder.domain.result.Either
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

class CharacterRepositoryImpl(
    private val api: RickAndMortyApi,
    private val cache: SearchCache,
) : CharacterRepository {

    override suspend fun search(query: String): Either<DomainError, List<Character>> {
        val key = query.trim().lowercase()
        cache.get(key)?.let { return Either.Right(it) }

        return try {
            val characters = try {
                api.searchCharacters(key).results.map { it.toDomain() }
            } catch (e: HttpException) {
                // La API de Rick & Morty responde 404 cuando la búsqueda no tiene coincidencias.
                if (e.code() == 404) emptyList() else throw e
            }
            cache.put(key, characters)
            Either.Right(characters)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Either.Left(e.toDomainError())
        }
    }

    override suspend fun getCharacter(id: Int): Either<DomainError, Character> = try {
        Either.Right(api.getCharacter(id).toDomain())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Either.Left(e.toDomainError())
    }
}

/** Traduce la excepción técnica al error de dominio según la reacción que merece en la UI. */
private fun Throwable.toDomainError(): DomainError = when (this) {
    is IOException -> DomainError.Connectivity
    is HttpException -> if (code() in 500..599) DomainError.Server else DomainError.Unexpected
    else -> DomainError.Unexpected
}
