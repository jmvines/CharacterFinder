package com.characterfinder.data.cache

import com.characterfinder.domain.model.Character
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Caché en memoria de resultados de búsqueda por query, con expiración por TTL.
 * El reloj se inyecta para poder testear la expiración sin esperas reales.
 */
class SearchCache(
    private val ttlMillis: Long,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private data class Entry(val characters: List<Character>, val storedAtMillis: Long)

    private val entries = mutableMapOf<String, Entry>()
    private val mutex = Mutex()

    suspend fun get(query: String): List<Character>? = mutex.withLock {
        val entry = entries[query] ?: return@withLock null
        if (now() - entry.storedAtMillis < ttlMillis) {
            entry.characters
        } else {
            entries.remove(query)
            null
        }
    }

    suspend fun put(query: String, characters: List<Character>) {
        mutex.withLock { entries[query] = Entry(characters, now()) }
    }
}
