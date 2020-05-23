package com.pstor.utils

sealed class Either<A, B> {
    class Left<A, B>(val left: A) : Either<A, B>()
    class Right<A, B>(val right: B) : Either<A, B>()

    fun <C> map(f: (B) -> C): Either<A, C> {
        return when(this) {
            is Right -> Right(f(this.right))
            is Left -> Left(this.left)
        }
    }

    fun <C> flatMap(f: (B) -> Either<A, C>): Either<A, C> {
        return when(this) {
            is Right -> f(this.right)
            is Left -> Left(this.left)
        }
    }

    companion object Factory {
        fun <A, B> safe(f: () -> B, onError: (Throwable) -> A): Either<A, B> {
            return try {
                Right(f())
            } catch (ex: Throwable) {
                Left(onError(ex))
            }
        }
    }
}