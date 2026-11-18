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
}
