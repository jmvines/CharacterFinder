package com.characterfinder.domain.result

sealed interface Either<out E, out A> {
    data class Left<out E>(val value: E) : Either<E, Nothing>
    data class Right<out A>(val value: A) : Either<Nothing, A>
}

inline fun <E, A, R> Either<E, A>.fold(ifLeft: (E) -> R, ifRight: (A) -> R): R = when (this) {
    is Either.Left -> ifLeft(value)
    is Either.Right -> ifRight(value)
}
