package com.github.aleperaltabazas.cola.handler

import com.github.aleperaltabazas.cola.ADMIN_ROLE
import com.github.aleperaltabazas.cola.QUEUES
import com.github.aleperaltabazas.cola.constants.BOOM
import com.github.aleperaltabazas.cola.constants.NEXT
import com.github.aleperaltabazas.cola.constants.PLUS
import com.github.aleperaltabazas.cola.extensions.addRoleOverwrite
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.Kord
import dev.kord.core.any
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.flow.toList

class GuildCreateHandler(
    private val client: Kord,
) : DiscordMessageHandler {
    override fun register() {
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
    }
}