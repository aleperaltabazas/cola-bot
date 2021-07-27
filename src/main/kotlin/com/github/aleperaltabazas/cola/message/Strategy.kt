package com.github.aleperaltabazas.cola.message

object GuardFailException : RuntimeException()

class HandlerStrategy {
    suspend fun guard(cond: Boolean, orElse: suspend () -> Unit = {}) = if (cond) Unit else {
        orElse()
        throw GuardFailException
    }

    suspend fun <T> guardNotNull(t: T?, orElse: suspend () -> Unit = {}): T {
        guard(t != null, orElse)
        return t!!
    }
}
