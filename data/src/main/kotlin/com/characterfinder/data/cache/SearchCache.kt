package com.characterfinder.data.cache

import com.characterfinder.domain.model.Character
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SearchCache(
    private val ttlMillis: Long,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val now: () -> Long = { System.nanoTime() / 1_000_000 },
) {

    private data class Entry(val characters: List<Character>, val storedAtMillis: Long)

    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)
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
        mutex.withLock {
            entries[query] = Entry(characters, now())
            if (entries.size > maxEntries) {
                entries.remove(entries.keys.iterator().next())
            }
        }
    }

    private companion object {
        const val DEFAULT_MAX_ENTRIES = 64
    }
}
