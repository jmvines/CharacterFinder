package com.characterfinder.domain.result

/**
 * Resultado de una operación que puede fallar: [Left] con el error o [Right] con el valor.
 * A diferencia de [kotlin.Result], el error va tipado ([Left] de cualquier tipo), así que la UI
 * puede distinguir causas y el compilador obliga a tratarlas todas.
 */
sealed interface Either<out E, out A> {
    data class Left<out E>(val value: E) : Either<E, Nothing>
    data class Right<out A>(val value: A) : Either<Nothing, A>
}

/** Colapsa el [Either] en un único valor tratando ambas ramas. */
inline fun <E, A, R> Either<E, A>.fold(ifLeft: (E) -> R, ifRight: (A) -> R): R = when (this) {
    is Either.Left -> ifLeft(value)
    is Either.Right -> ifRight(value)
}
