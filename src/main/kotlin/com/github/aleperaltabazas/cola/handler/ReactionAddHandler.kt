package com.github.aleperaltabazas.cola.handler

import com.github.aleperaltabazas.cola.QUEUES
import com.github.aleperaltabazas.cola.actors.*
import com.github.aleperaltabazas.cola.constants.BOOM
import com.github.aleperaltabazas.cola.constants.NEXT
import com.github.aleperaltabazas.cola.constants.PLUS
import com.github.aleperaltabazas.cola.extensions.getAuthorAsUser
import dev.kord.core.Kord
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.on
import org.slf4j.LoggerFactory

class ReactionAddHandler(
    private val queuesActor: QueueActor,
    private val client: Kord,
) : DiscordMessageHandler {
    override fun register() {
        client.on<ReactionAddEvent> {
            if (message.getChannel().data.name.value != QUEUES) return@on
            if (emoji !in SUPPORTED_ADD_REACTIONS) return@on
            if (getUserOrNull()?.isBot == true) return@on

            try {
                val command = parseCommand()
                if (command != null) queuesActor.send(command)
            } catch (e: Exception) {
                LOGGER.error("An error occurred", e)
                message.channel.createMessage("Beep boop andá a mirar a los logs")
            }
        }
    }

    private suspend fun ReactionAddEvent.parseCommand(): QueueMessage? = when (emoji) {
        PLUS -> JoinQueue(
            messageId = this.messageId.asString,
            channel = channel,
            author = getAuthorAsUser()!!,
        )
        NEXT -> PopQueue(
            messageId = this.messageId.asString,
            channel = channel,
            author = getAuthorAsUser()!!,
        )
        BOOM -> DeleteQueue(
            messageId = this.messageId.asString,
            channel = channel,
            author = getAuthorAsUser()!!,
        )
        else -> null
    }

    companion object {
        private val SUPPORTED_ADD_REACTIONS = listOf(PLUS, NEXT, BOOM)
        private val LOGGER = LoggerFactory.getLogger(ReactionAddHandler::class.java)
    }
}