package com.characterfinder.domain.model

@JvmInline
value class SearchQuery(private val raw: String) {

    val normalized: String
        get() = raw.trim()

    val isValid: Boolean
        get() = normalized.isNotEmpty()
}
