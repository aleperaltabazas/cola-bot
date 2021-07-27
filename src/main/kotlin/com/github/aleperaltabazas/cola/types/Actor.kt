package com.github.aleperaltabazas.cola.types

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor

abstract class Actor<T>(
    private val scope: CoroutineScope,
) {
    @OptIn(ObsoleteCoroutinesApi::class)
    private val actorRef: SendChannel<T> = scope.actor {
        for (message in channel) {
            handle(message)
        }
    }

    suspend fun send(message: T) {
        actorRef.send(message)
    }

    // This breaks, for some reason
//    suspend fun defer(message: T, timeInMillis: Long) {
//        scope.launch {
//            delay(timeMillis = timeInMillis)
//            send(message)
//        }
//    }

    protected abstract suspend fun handle(message: T)
}