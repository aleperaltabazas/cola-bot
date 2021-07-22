package com.github.aleperaltabazas.cola.message

import com.github.aleperaltabazas.cola.constants.OK_HAND
import com.github.aleperaltabazas.cola.extensions.getAuthorAsUser
import com.github.aleperaltabazas.cola.user.User
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji

object GuardFailException : RuntimeException()

sealed class HandlerStrategy(
    val message: Message,
) {
    suspend fun guard(cond: Boolean, orElse: suspend () -> Unit = {}) = if (cond) Unit else {
        orElse()
        throw GuardFailException
    }

    suspend fun <T> guardNotNull(t: T?, orElse: suspend () -> Unit = {}): T {
        guard(t != null, orElse)
        return t!!
    }

    abstract suspend fun react(emoji: ReactionEmoji.Unicode)

    abstract suspend fun sendMessage(content: String)

    suspend fun author(): User = guardNotNull(message.getAuthorAsUser())

    suspend fun ack() = react(OK_HAND)
}

class ProdStrategy(
    message: Message,
) : HandlerStrategy(message) {
    override suspend fun react(emoji: ReactionEmoji.Unicode) = message.addReaction(emoji)

    override suspend fun sendMessage(content: String) {
        this.message.channel.createMessage(content)
    }
}

class FakeStrategy(
    message: Message
) : HandlerStrategy(message) {
    private val actions: MutableList<Action> = mutableListOf()

    sealed class Action

    class SendMessage(content: String) : Action()
    class React(emoji: ReactionEmoji.Unicode) : Action()

    override suspend fun react(emoji: ReactionEmoji.Unicode) {
        actions.add(React(emoji))
    }

    override suspend fun sendMessage(content: String) {
        actions.add(SendMessage(content))
    }
}
