package com.characterfinder.domain.result

/**
 * Errores de dominio que la UI trata de forma distinta. Se modelan por la reacción del usuario,
 * no por la causa técnica: dos causas con el mismo mensaje y la misma acción comparten tipo.
 */
sealed interface DomainError {
    /** Sin conexión (timeout, DNS, red caída). El usuario puede arreglarlo. */
    data object Connectivity : DomainError

    /** El backend falla (5xx). Temporal, no es culpa del usuario. */
    data object Server : DomainError

    /** Cualquier otra cosa: parsing, respuestas inesperadas, etc. */
    data object Unexpected : DomainError
}
