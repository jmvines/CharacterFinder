package com.characterfinder.data.cache

import com.characterfinder.data.characterStub
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchCacheTest {

    private var now = 0L
    private val cache = SearchCache(ttlMillis = 1_000, now = { now })

    @Test
    fun `devuelve null cuando no hay entrada para la query`() = runTest {
        assertNull(cache.get("rick"))
    }

    @Test
    fun `devuelve la entrada mientras no se cumple el ttl`() = runTest {
        val characters = listOf(characterStub(1, "Rick Sanchez"))
        cache.put("rick", characters)

        now = 999
        assertEquals(characters, cache.get("rick"))
    }

    @Test
    fun `expira la entrada cumplido el ttl`() = runTest {
        cache.put("rick", listOf(characterStub(1, "Rick Sanchez")))

        now = 1_000
        assertNull(cache.get("rick"))
    }

    @Test
    fun `cada query tiene su propia entrada`() = runTest {
        val ricks = listOf(characterStub(1, "Rick Sanchez"))
        cache.put("rick", ricks)

        assertNull(cache.get("morty"))
        assertEquals(ricks, cache.get("rick"))
    }

    @Test
    fun `al superar el tope descarta la entrada menos usada`() = runTest {
        val limited = SearchCache(ttlMillis = 1_000, maxEntries = 2, now = { now })
        val rick = listOf(characterStub(1, "Rick Sanchez"))
        val summer = listOf(characterStub(3, "Summer Smith"))
        limited.put("rick", rick)
        limited.put("morty", listOf(characterStub(2, "Morty Smith")))
        limited.get("rick")
        limited.put("summer", summer)

        assertNull(limited.get("morty"))
        assertEquals(rick, limited.get("rick"))
        assertEquals(summer, limited.get("summer"))
    }
}
