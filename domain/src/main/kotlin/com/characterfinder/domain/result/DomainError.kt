package com.characterfinder.domain.result

sealed interface DomainError {
    data object Connectivity : DomainError
    data object Server : DomainError
    data object Unexpected : DomainError
}
