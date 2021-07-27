package com.github.aleperaltabazas.cola.actors

import com.github.aleperaltabazas.cola.constants.BOOM
import com.github.aleperaltabazas.cola.constants.NEXT
import com.github.aleperaltabazas.cola.constants.PLUS
import com.github.aleperaltabazas.cola.message.ChannelHandler
import com.github.aleperaltabazas.cola.model.ChannelQueues
import com.github.aleperaltabazas.cola.model.User
import com.github.aleperaltabazas.cola.types.Actor
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.util.*

sealed class QueueMessage {
    abstract fun log()
}

sealed class AuthoredMessage(val channel: MessageChannelBehavior, val author: User) : QueueMessage() {
    override fun log() {
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
) : AuthoredMessage(channel, author) {
    override val action = "create queue $queueName"
}

class JoinQueue(val messageId: String, channel: MessageChannelBehavior, author: User) : AuthoredMessage(channel, author) {
    override val action = "enqueue queue $messageId"
}

class LeaveQueue(val messageId: String, channel: MessageChannelBehavior, author: User) : AuthoredMessage(channel, author) {
    override val action = "leave queue $messageId"
}

class PopQueue(val messageId: String, channel: MessageChannelBehavior, author: User) : AuthoredMessage(channel, author) {
    override val action = "pop queue $messageId"
}

class DeleteQueue(val messageId: String, channel: MessageChannelBehavior, author: User) : AuthoredMessage(channel, author) {
    override val action = "delete queue $messageId"
}

class DeleteMessage(val message: Message) : QueueMessage() {
    override fun log() {
        LOGGER.info("Delete message ${message.id.asString}")
    }
}

class QueueActor(scope: CoroutineScope) : Actor<QueueMessage>(scope) {
    override suspend fun handle(message: QueueMessage) {
        ChannelHandler {
            when (message) {
                is CreateQueue -> {
                    val author = message.author
                    guard(author.isAdmin())

                    guard(!queues.exists(message.channelId, message.queueName)) {
                        message.message.delete()
                    }

                    val queueMessage = message.message.channel.createMessage(message.queueName)
                    val queue = queues.new(message.channelId, queueMessage, message.queueName)
                    queueMessage.edit {
                        content = "**${message.queueName}**\n${queue.pretty(4)}"
                    }
                    queueMessage.addReaction(PLUS)
                    queueMessage.addReaction(NEXT)
                    queueMessage.addReaction(BOOM)
                    message.message.delete()
                }
                is JoinQueue -> {
                    val queue = guardNotNull(queues.getByMessageId(message.channelId, message.messageId))
                    val author = message.author

                    queue.takeUnless { it.contains(author) }?.add(author)
                    queue.message.edit {
                        content = "**${queue.queueName}**\n${queue.pretty(4)}"
                    }
                }
                is LeaveQueue -> {
                    val queue = guardNotNull(queues.getByMessageId(message.channelId, message.messageId))
                    val author = message.author

                    queue.takeIf { it.contains(author) }?.remove(author)
                    queues.get(message.channelId)
                    queue.message.edit {
                        content = "**${queue.queueName}**\n${queue.pretty(4)}"
                    }
                }
                is PopQueue -> {
                    val queue = guardNotNull(queues.getByMessageId(message.channelId, message.messageId))
                    val author = message.author
                    queue.message.deleteReaction(Snowflake(message.author.id), NEXT)
                    guard(author.isAdmin())

                    val next = queue.poll()
                    if (next == null) {
                        val response = message.channel.createMessage("Queue **${message.messageId}** is empty!")
                    } else {
                        val response = message.channel.createMessage("<@${next.id}> - you're up!")
                    }
                    queue.message.deleteReaction(Snowflake(next.id), PLUS)
                    queue.message.edit {
                        content = "**${queue.queueName}**\n${queue.pretty(4)}"
                    }
                }
                is DeleteQueue -> {
                    val queue = guardNotNull(queues.getByMessageId(message.channelId, message.messageId))

                    val author = message.author
                    guard(author.isAdmin())

                    queues.delete(message.channelId, message.messageId)
                    queue.message.delete()
                }
                is DeleteMessage -> {
                    message.message.delete()
                }
            }
        }

    }

    private val queues = ChannelQueues()
    private fun Queue<User>.pretty(indent: Int = 0) = joinToString("\n") { "${" ".repeat(indent)}- ${it.username}" }
}

private val LOGGER = LoggerFactory.getLogger("QueueActor")
