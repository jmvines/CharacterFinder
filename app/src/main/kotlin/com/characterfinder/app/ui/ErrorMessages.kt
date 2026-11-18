package com.characterfinder.app.ui

import com.characterfinder.app.R
import com.characterfinder.domain.result.DomainError

/** Mapea cada [DomainError] al string que ve el usuario. El `when` exhaustivo obliga a dar
 *  mensaje a cualquier error nuevo que se añada. */
internal fun DomainError.messageRes(): Int = when (this) {
    DomainError.Connectivity -> R.string.error_connectivity
    DomainError.Server -> R.string.error_server
    DomainError.Unexpected -> R.string.error_generic
}
