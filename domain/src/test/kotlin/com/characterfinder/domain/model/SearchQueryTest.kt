package com.characterfinder.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryTest {

    @Test
    fun `normaliza recortando los espacios`() {
        assertEquals("rick", SearchQuery("  rick  ").normalized)
    }

    @Test
    fun `es valida cuando tiene contenido tras normalizar`() {
        assertTrue(SearchQuery("rick").isValid)
        assertTrue(SearchQuery("  rick  ").isValid)
    }

    @Test
    fun `no es valida vacia o solo con espacios`() {
        assertFalse(SearchQuery("").isValid)
        assertFalse(SearchQuery("   ").isValid)
    }
}
