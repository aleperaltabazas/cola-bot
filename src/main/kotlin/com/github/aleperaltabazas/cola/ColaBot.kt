package com.github.aleperaltabazas.cola

import com.github.aleperaltabazas.cola.actors.*
import com.github.aleperaltabazas.cola.constants.BOOM
import com.github.aleperaltabazas.cola.constants.NEXT
import com.github.aleperaltabazas.cola.constants.PLUS
import com.github.aleperaltabazas.cola.extensions.addRoleOverwrite
import com.github.aleperaltabazas.cola.extensions.getAuthorAsUser
import com.github.aleperaltabazas.cola.extensions.words
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.Kord
import dev.kord.core.any
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.entity.Message
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.on
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import spark.Spark

val CONFIG: Config = ConfigFactory.load()
val ADMIN_ROLE: String = CONFIG.getString("queue.manager.role")
const val QUEUES = "queues"

@OptIn(ObsoleteCoroutinesApi::class)
fun main() = runBlocking {
    val port = ProcessBuilder().environment()
        ?.get("PORT")
        ?.toInt() ?: 9290

    LOGGER.info("Using port $port")

    Spark.port(port)
    Spark.get("/*") { _, _ -> "I feel fantastic and I'm still alive" }

    val client = Kord(CONFIG.getString("discord.bot.token"))
    val supportedCommands = listOf("!queue")
    val supportedAddReactions = listOf(PLUS, NEXT, BOOM)
    val supportedRemoveReactions = listOf(PLUS)

    val queue = queueActor()

    client.on<MessageCreateEvent> {
        if (message.getChannel().data.name.value != QUEUES) return@on
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

    client.on<ReactionAddEvent> {
        if (message.getChannel().data.name.value != QUEUES) return@on
        if (emoji !in supportedAddReactions) return@on
        if (getUserOrNull()?.isBot == true) return@on

        try {
            val command = parseCommand()
            if (command != null) queue.send(command)
        } catch (e: Exception) {
            LOGGER.error("An error occurred", e)
            message.channel.createMessage("Beep boop andá a mirar a los logs")
        }
    }

    client.on<ReactionRemoveEvent> {
        if (message.getChannel().data.name.value != QUEUES) return@on
        if (emoji !in supportedRemoveReactions) return@on
        if (getUserOrNull()?.isBot == true) return@on

        try {
            val command = parseCommand()
            if (command != null) queue.send(command)
        } catch (e: Exception) {
            LOGGER.error("An error occurred", e)
            message.channel.createMessage("Beep boop andá a mirar a los logs")
        }
    }

    client.on<GuildCreateEvent> {
        val role = if (!this.guild.roles.any { it.name == ADMIN_ROLE }) this.guild.createRole {
            name = ADMIN_ROLE
            color = Color(149, 16, 241)
        } else this.guild.roles.toList().find { it.name == ADMIN_ROLE }!!

        if (!guild.getMember(client.selfId).roles.any { it.id == role.id }) {
            guild.getMember(client.selfId).addRole(role.id)
        }

        if (!this.guild.channels.toList().any { it.name == QUEUES }) {
            val helpMessage = """
                > `!queue new <queueName>` to create a new queue if it doesn't exist
                > React with ${PLUS.name} to the message to join the queue. Delete said reaction to leave the queue.
                > To advance the queue, react with the ${NEXT.name} emoji. To delete the queue, react with the ${BOOM.name} emoji.
                        """.trimIndent()

            val channel = this.guild.createTextChannel(QUEUES) {
                this.topic = "queues"
                val everyone = guild.getEveryoneRole()
                addRoleOverwrite(role.id) {
                    this.allowed = Permissions(
                        Permission.ManageMessages,
                        Permission.ReadMessageHistory,
                        Permission.ManageChannels,
                        Permission.SendMessages,
                    )
                }

                addRoleOverwrite(everyone.id) {
                    this.denied = Permissions(
                        Permission.ManageMessages,
                        Permission.ManageChannels,
                        Permission.SendMessages,
                        Permission.UseExternalEmojis,
                    )
                    this.allowed = Permissions(
                        Permission.ReadMessageHistory,
                        Permission.AddReactions,
                    )
                }
            }
            channel.createMessage(helpMessage)
        }
    }

    client.login()
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

private suspend fun ReactionRemoveEvent.parseCommand(): QueueMessage? = when (emoji) {
    PLUS -> LeaveQueue(
        messageId = this.messageId.asString,
        channel = channel,
        author = getAuthorAsUser()!!,
    )
    else -> null
}

private val LOGGER = LoggerFactory.getLogger("ColaBot")
