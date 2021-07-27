package com.github.aleperaltabazas.cola

import com.github.aleperaltabazas.cola.actors.*
import com.github.aleperaltabazas.cola.extensions.addRoleOverwrite
import com.github.aleperaltabazas.cola.extensions.words
import com.github.aleperaltabazas.cola.message.ChannelHandler
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

    val queue = queueActor(ChannelHandler(CONFIG))

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

    client.on<GuildCreateEvent> {
        val role = if (!this.guild.roles.any { it.name == ADMIN_ROLE }) this.guild.createRole {
            name = ADMIN_ROLE
            color = Color(149, 16, 241)
        } else this.guild.roles.toList().find { it.name == ADMIN_ROLE }!!

        if (!guild.getMember(client.selfId).roles.any { it.id == role.id }) {
            guild.getMember(client.selfId).addRole(role.id)
        }

        if (!this.guild.channels.toList().any { it.name == QUEUES }) {
            this.guild.createTextChannel(QUEUES) {
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
