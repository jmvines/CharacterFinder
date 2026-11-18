package com.characterfinder.data.repository

import com.characterfinder.data.cache.SearchCache
import com.characterfinder.data.characterDtoStub
import com.characterfinder.data.remote.CharacterDto
import com.characterfinder.data.remote.CharacterPageDto
import com.characterfinder.data.remote.RickAndMortyApi
import com.characterfinder.domain.model.Character
import com.characterfinder.domain.result.DomainError
import com.characterfinder.domain.result.Either
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class CharacterRepositoryImplTest {

    private class FakeApi : RickAndMortyApi {
        val searchedNames = mutableListOf<String>()
        var searchException: Exception? = null

        override suspend fun searchCharacters(name: String): CharacterPageDto {
            searchedNames += name
            searchException?.let { throw it }
            return CharacterPageDto(results = listOf(characterDtoStub(1, name)))
        }

        override suspend fun getCharacter(id: Int): CharacterDto = characterDtoStub(id, "Rick Sanchez")
    }

    private var now = 0L
    private val api = FakeApi()
    private val repository = CharacterRepositoryImpl(
        api = api,
        cache = SearchCache(ttlMillis = 1_000, now = { now }),
    )

    @Test
    fun `la segunda busqueda de la misma query sale de cache sin tocar la red`() = runTest {
        val first = repository.search("rick")
        val second = repository.search("rick")

        assertEquals(1, api.searchedNames.size)
        assertEquals(first, second)
    }

    @Test
    fun `con la cache expirada vuelve a la red y refresca la entrada`() = runTest {
        repository.search("rick")
        now = 1_000
        repository.search("rick")

        assertEquals(2, api.searchedNames.size)

        // La entrada refrescada vuelve a ser válida sin red durante otro TTL.
        repository.search("rick")
        assertEquals(2, api.searchedNames.size)
    }

    @Test
    fun `normaliza la query como clave de cache`() = runTest {
        repository.search("Rick")
        repository.search("  rick ")

        assertEquals(listOf("rick"), api.searchedNames)
    }

    @Test
    fun `el 404 de la api se traduce a lista vacia y tambien se cachea`() = runTest {
        api.searchException = httpException(404)

        assertEquals(Either.Right(emptyList<Character>()), repository.search("nadie"))

        assertEquals(Either.Right(emptyList<Character>()), repository.search("nadie"))
        assertEquals(1, api.searchedNames.size)
    }

    @Test
    fun `un 5xx del servidor se traduce a DomainError Server y no se cachea`() = runTest {
        api.searchException = httpException(500)

        assertEquals(Either.Left(DomainError.Server), repository.search("rick"))

        // Al no cachearse el error, un segundo intento vuelve a la red.
        repository.search("rick")
        assertEquals(2, api.searchedNames.size)
    }

    @Test
    fun `un fallo de red se traduce a DomainError Connectivity`() = runTest {
        api.searchException = IOException("sin red")

        assertEquals(Either.Left(DomainError.Connectivity), repository.search("rick"))
    }

    private fun httpException(code: Int): HttpException {
        val body = "{}".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<CharacterPageDto>(code, body))
    }
}
