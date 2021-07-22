package com.github.aleperaltabazas.cola

import com.github.aleperaltabazas.cola.actors.*
import com.github.aleperaltabazas.cola.extensions.words
import com.github.aleperaltabazas.cola.message.ChannelHandler
import com.typesafe.config.ConfigFactory
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import spark.Spark

@OptIn(ObsoleteCoroutinesApi::class)
fun main() = runBlocking {
    val port = ProcessBuilder().environment()
        ?.get("PORT")
        ?.toInt() ?: 9290

    LOGGER.info("Using port $port")

    Spark.port(port)
    Spark.get("/*") { _, _ -> "I feel fantastic and I'm still alive" }

    val config = ConfigFactory.load()
    val client = Kord(config.getString("discord.bot.token"))
    val supportedCommands = listOf("!queue")

    val queue = queueActor(ChannelHandler(config))

    client.on<MessageCreateEvent> {
        if (supportedCommands.none { message.content.startsWith(it) }) return@on
        if (message.author?.isBot == true) return@on

        try {
            val command = message.cheapParseCommand()
            if (command != null) queue.send(command)
        } catch (e: Exception) {
            LOGGER.error("An error occurred", e)
            message.channel.createMessage("Beep boop andá a mirar a los logs")
        }
    }

    client.login()
}

// TODO: use a fucking parser combinator
private fun Message.cheapParseCommand(): QueueMessage? {
    return content.words()
        .takeIf { (main) -> main == "!queue" }
        ?.takeUnless { it.size < 2 }
        ?.drop(1)
        ?.let { words ->
            when (words[0]) {
                "delete" -> DeleteQueue(queueName = words[1], message = this)
                "help" -> QueueHelp(this)
                "leave" -> LeaveQueue(queueName = words[1], message = this)
                "list" -> ListQueues(message = this)
                "join" -> JoinQueue(queueName = words[1], message = this)
                "next" -> PopQueue(queueName = words[1], message = this)
                "new" -> CreateQueue(queueName = words[1], message = this)
                "status" -> QueueStatus(queueName = words[1], message = this)
                else -> null
            }
        }
}

private val LOGGER = LoggerFactory.getLogger("ColaBot")
