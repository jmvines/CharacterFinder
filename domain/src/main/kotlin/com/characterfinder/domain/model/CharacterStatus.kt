package com.characterfinder.domain.model

/** Estado vital por el que se puede filtrar una búsqueda. Sin filtro = `null` en la búsqueda. */
enum class CharacterStatus {
    ALIVE,
    DEAD,
    UNKNOWN,
}
