package com.github.aleperaltabazas.cola.actors

import com.github.aleperaltabazas.cola.constants.BOOM
import com.github.aleperaltabazas.cola.constants.NEXT
import com.github.aleperaltabazas.cola.constants.PLUS
import com.github.aleperaltabazas.cola.message.ChannelHandler
import com.github.aleperaltabazas.cola.model.ChannelQueues
import com.github.aleperaltabazas.cola.model.User
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import org.slf4j.LoggerFactory
import java.util.*

sealed class QueueMessage(val channel: MessageChannelBehavior, val author: User) {
    fun log() {
        LOGGER.info("Channel[$channelId] - User[${author.username}]: $action")
    }

    protected abstract val action: String

    val channelId = channel.id.asString
}

class CreateQueue(
    val queueName: String,
    channel: MessageChannelBehavior,
    author: User,
    val message: Message,
) : QueueMessage(channel, author) {
    override val action = "create queue $queueName"
}

class JoinQueue(val messageId: String, channel: MessageChannelBehavior, author: User) : QueueMessage(channel, author) {
    override val action = "enqueue queue $messageId"
}

class LeaveQueue(val messageId: String, channel: MessageChannelBehavior, author: User) : QueueMessage(channel, author) {
    override val action = "leave queue $messageId"
}

class PopQueue(val messageId: String, channel: MessageChannelBehavior, author: User) : QueueMessage(channel, author) {
    override val action = "pop queue $messageId"
}

class DeleteQueue(val messageId: String, channel: MessageChannelBehavior, author: User) : QueueMessage(channel, author) {
    override val action = "delete queue $messageId"
}

// TODO. might be wise to consider a structure a bit more complex to use DI or something like that for better testing
//  and extensibility
@OptIn(ObsoleteCoroutinesApi::class)
fun CoroutineScope.queueActor() = actor<QueueMessage> {
    val queues = ChannelQueues()
    fun Queue<User>.pretty(indent: Int = 0) = joinToString("\n") { "${" ".repeat(indent)}- ${it.username}" }

    for (msg in channel) {
        msg.log()

        ChannelHandler {
            when (msg) {
                is CreateQueue -> {
                    val author = msg.author
                    guard(author.isAdmin())

                    guard(!queues.exists(msg.channelId, msg.queueName)) {
                        msg.message.delete()
                    }

                    val queueMessage = msg.message.channel.createMessage(msg.queueName)
                    val queue = queues.new(msg.channelId, queueMessage, msg.queueName)
                    queueMessage.edit {
                        content = "**${msg.queueName}**\n${queue.pretty(4)}"
                    }
                    queueMessage.addReaction(PLUS)
                    queueMessage.addReaction(NEXT)
                    queueMessage.addReaction(BOOM)
                    msg.message.delete()
                }
                is JoinQueue -> {
                    val queue = guardNotNull(queues.getByMessageId(msg.channelId, msg.messageId))
                    val author = msg.author

                    queue.takeUnless { it.contains(author) }?.add(author)
                    queue.message.edit {
                        content = "**${queue.queueName}**\n${queue.pretty(4)}"
                    }
                }
                is LeaveQueue -> {
                    val queue = guardNotNull(queues.getByMessageId(msg.channelId, msg.messageId))
                    val author = msg.author

                    queue.takeIf { it.contains(author) }?.remove(author)
                    queues.get(msg.channelId)
                    queue.message.edit {
                        content = "**${queue.queueName}**\n${queue.pretty(4)}"
                    }
                }
                is PopQueue -> {
                    val queue = guardNotNull(queues.getByMessageId(msg.channelId, msg.messageId))
                    val author = msg.author
                    queue.message.deleteReaction(Snowflake(msg.author.id), NEXT)
                    guard(author.isAdmin())

                    val next = queue.poll()
                    if (next == null) msg.channel.createMessage("Queue **${msg.messageId}** is empty!")
                    else msg.channel.createMessage("<@${next.id}> - you're up!")
                    queue.message.deleteReaction(Snowflake(next.id), PLUS)
                    queue.message.edit {
                        content = "**${queue.queueName}**\n${queue.pretty(4)}"
                    }
                }
                is DeleteQueue -> {
                    val queue = guardNotNull(queues.getByMessageId(msg.channelId, msg.messageId))

                    val author = msg.author
                    guard(author.isAdmin())

                    queues.delete(msg.channelId, msg.messageId)
                    queue.message.delete()
                }
            }

        }
    }
}


private val LOGGER = LoggerFactory.getLogger("QueueActor")
