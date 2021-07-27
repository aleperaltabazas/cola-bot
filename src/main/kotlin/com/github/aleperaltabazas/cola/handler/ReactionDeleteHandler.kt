package com.github.aleperaltabazas.cola.handler

import com.github.aleperaltabazas.cola.QUEUES
import com.github.aleperaltabazas.cola.actors.LeaveQueue
import com.github.aleperaltabazas.cola.actors.QueueActor
import com.github.aleperaltabazas.cola.actors.QueueMessage
import com.github.aleperaltabazas.cola.constants.PLUS
import com.github.aleperaltabazas.cola.extensions.getAuthorAsUser
import dev.kord.core.Kord
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.on
import org.slf4j.LoggerFactory

class ReactionDeleteHandler(
    private val queuesActor: QueueActor,
    private val client: Kord,
) : DiscordMessageHandler {
    override fun register() {
        client.on<ReactionRemoveEvent> {
            if (message.getChannel().data.name.value != QUEUES) return@on
            if (emoji !in SUPPORTED_REMOVE_REACTIONS) return@on
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

    private suspend fun ReactionRemoveEvent.parseCommand(): QueueMessage? = when (emoji) {
        PLUS -> LeaveQueue(
            messageId = this.messageId.asString,
            channel = channel,
            author = getAuthorAsUser()!!,
        )
        else -> null
    }

    companion object {
        private val SUPPORTED_REMOVE_REACTIONS = listOf(PLUS)
        private val LOGGER = LoggerFactory.getLogger(ReactionDeleteHandler::class.java)
    }
}