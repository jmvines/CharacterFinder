package com.characterfinder.app.ui

import com.characterfinder.app.R
import com.characterfinder.domain.result.DomainError

internal fun DomainError.messageRes(): Int = when (this) {
    DomainError.Connectivity -> R.string.error_connectivity
    DomainError.Server -> R.string.error_server
    DomainError.Unexpected -> R.string.error_generic
}
