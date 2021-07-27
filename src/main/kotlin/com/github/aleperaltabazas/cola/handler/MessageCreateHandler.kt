package com.github.aleperaltabazas.cola.handler

import com.github.aleperaltabazas.cola.QUEUES
import com.github.aleperaltabazas.cola.actors.CreateQueue
import com.github.aleperaltabazas.cola.actors.QueueMessage
import com.github.aleperaltabazas.cola.extensions.getAuthorAsUser
import com.github.aleperaltabazas.cola.extensions.words
import com.github.aleperaltabazas.cola.types.Actor
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import org.slf4j.LoggerFactory

class MessageCreateHandler(
    private val queuesActor: Actor<QueueMessage>,
    private val client: Kord,
) : DiscordMessageHandler {
    override fun register() {
        client.on<MessageCreateEvent> {
            if (message.getChannel().data.name.value != QUEUES) return@on
            if (SUPPORTED_COMMANDS.none { message.content.startsWith(it) }) return@on
            if (message.author?.isBot == true) return@on

            try {
                val command = message.cheapParseCommand()
                if (command != null) queuesActor.send(command)
            } catch (e: Exception) {
                LOGGER.error("An error occurred", e)
                message.channel.createMessage("Beep boop andá a mirar a los logs")
            }
        }
    }

    private suspend fun Message.cheapParseCommand(): QueueMessage? = content.words()
        .takeIf { (main) -> main == "!queue" }
        ?.takeUnless { it.size < 2 }
        ?.drop(1)
        ?.let { words ->
            when (words[0]) {
                "new" -> CreateQueue(
                    queueName = words[1],
                    message = this,
                    author = this.getAuthorAsUser()!!,
                    channel = this.channel,
                )
                else -> null
            }
        }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MessageCreateHandler::class.java)
        private val SUPPORTED_COMMANDS = listOf("!queue")
    }
}