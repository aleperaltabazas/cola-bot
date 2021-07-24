package com.github.aleperaltabazas.cola.actors

import com.github.aleperaltabazas.cola.message.ChannelHandler
import com.github.aleperaltabazas.cola.model.ChannelQueues
import com.github.aleperaltabazas.cola.model.User
import dev.kord.core.entity.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import org.slf4j.LoggerFactory
import java.util.*

sealed class QueueMessage(val message: Message) {
    fun log() {
        LOGGER.info("Channel[${message.channel.id.asString}] - User[${message.author?.username}]: $action")
    }

    protected abstract val action: String
}

class CreateQueue(val queueName: String, message: Message) : QueueMessage(message) {
    override val action = "create queue $queueName"
}

class JoinQueue(val queueName: String, message: Message) : QueueMessage(message) {
    override val action = "enqueue queue $queueName"
}

class LeaveQueue(val queueName: String, message: Message) : QueueMessage(message) {
    override val action = ""
}

class PopQueue(val queueName: String, message: Message) : QueueMessage(message) {
    override val action = "pop queue $queueName"
}

class QueueStatus(val queueName: String, message: Message) : QueueMessage(message) {
    override val action = "queue status $queueName"
}

class DeleteQueue(val queueName: String, message: Message) : QueueMessage(message) {
    override val action = "delete queue $queueName"
}

class ListQueues(message: Message) : QueueMessage(message) {
    override val action = "list queues"
}

class QueueHelp(message: Message) : QueueMessage(message) {
    override val action: String = "help"
}

// TODO. might be wise to consider a structure a bit more complex to use DI or something like that for better testing
//  and extensibility
@OptIn(ObsoleteCoroutinesApi::class)
fun CoroutineScope.queueActor(handler: ChannelHandler) = actor<QueueMessage> {
    val queues = ChannelQueues()
    fun Queue<User>.pretty(indent: Int = 0) = joinToString("\n") { "${" ".repeat(indent)}- ${it.username}" }

    for (msg in channel) {
        handler(msg.message) {
            suspend fun queueAlreadyExists(queue: String) {
                sendMessage("Queue **$queue** already exists!")
            }

            suspend fun queueDoesNotExist(queue: String) {
                sendMessage("Queue **$queue** does not exist!")
            }

            msg.log()

            when (msg) {
                is CreateQueue -> {
                    val author = author()
                    guard(author.isAdmin())

                    queues.get(channelId, msg.queueName)
                        ?.let { queueAlreadyExists(msg.queueName) }
                        ?: run {
                            queues.new(channelId, msg.queueName)
                            ack()
                        }
                }
                is JoinQueue -> {
                    val queue = guardNotNull(queues.get(channelId, msg.queueName)) {
                        queueDoesNotExist(msg.queueName)
                    }
                    val author = author()

                    queue.takeUnless { it.contains(author) }?.add(author)
                    ack()
                }
                is LeaveQueue -> {
                    val queue = guardNotNull(queues.get(channelId, msg.queueName)) {
                        queueDoesNotExist(msg.queueName)
                    }
                    val author = author()

                    queue.takeIf { it.contains(author) }?.remove(author)
                    ack()
                }
                is PopQueue -> {
                    val queue = guardNotNull(queues.get(channelId, msg.queueName)) {
                        queueDoesNotExist(msg.queueName)
                    }
                    val author = author()
                    guard(author.isAdmin())

                    val next = queue.poll()
                    if (next == null) sendMessage("Queue **${msg.queueName}** is empty!")
                    else sendMessage("<@${next.id}> - you're up!")
                }
                is QueueStatus -> {
                    val queue = guardNotNull(queues.get(channelId, msg.queueName)) {
                        queueDoesNotExist(msg.queueName)
                    }

                    if (queue.isEmpty()) sendMessage("Queue **${msg.queueName}** is empty")
                    else {
                        val message = queue.pretty(indent = 4)
                        sendMessage("${msg.queueName}\n$message")
                    }
                }
                is DeleteQueue -> {
                    guard(queues.exists(channelId, msg.queueName)) {
                        queueDoesNotExist(msg.queueName)
                    }

                    val author = author()
                    guard(author.isAdmin())

                    queues.delete(channelId, msg.queueName)
                    ack()
                }
                is ListQueues -> {
                    val queue = guardNotNull(queues.get(channelId))
                    if (queue.isEmpty()) sendMessage("No queues have been created yet")
                    else {
                        val message = queue.toList().joinToString("\n") { (listName, users) ->
                            "**$listName\n**${users.pretty(4)}"
                        }
                        sendMessage(message)
                    }
                }
                is QueueHelp -> {
                    val message = """
                        ```!queue delete q - delete queue 'q' (if it exists)
                        !queue help     - show this text message
                        !queue join q   - join queue 'q' (if you're not already in it)
                        !queue leave q  - leave queue 'q' (if you're already in it)
                        !queue list     - show all existing queues along with users awaiting in each
                        !queue next q   - call the next user in queue 'q' in
                        !queue new q    - create new queue 'q'
                        !queue status q - show the state of queue 'q' along with users waiting in it```
                    """.trimIndent()

                    sendMessage(message)
                }
            }
        }

    }
}


private val LOGGER = LoggerFactory.getLogger("QueueActor")
