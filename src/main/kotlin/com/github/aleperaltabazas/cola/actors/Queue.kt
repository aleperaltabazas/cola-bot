package com.github.aleperaltabazas.cola.actors

import arrow.core.computations.nullable
import com.github.aleperaltabazas.cola.user.User
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import org.slf4j.LoggerFactory
import java.util.*

sealed class QueueMessage(val message: Message) {
    fun log() {
        LOGGER.info("${message.author?.username}: $action")
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

@OptIn(ObsoleteCoroutinesApi::class)
fun CoroutineScope.queueActor() = actor<QueueMessage> {
    val queues: MutableMap<String, Queue<User>> = mutableMapOf()
    val okHand = ReactionEmoji.Unicode("\uD83D\uDC4C")

    fun Queue<User>.pretty(indent: Int = 0) = joinToString("\n") { "${" ".repeat(indent)}- ${it.userId}" }

    for (msg in channel) {
        msg.log()
        suspend fun queueAlreadyExists(queue: String) {
            msg.message.channel.createMessage("Queue **$queue** already exists!")
        }

        suspend fun queueDoesNotExist(queue: String) {
            msg.message.channel.createMessage("Queue **$queue** does not exist!")
        }

        when (msg) {
            is CreateQueue -> nullable {
                val author = msg.message.getAuthorAsUser().bind()

                if (!author.isAdmin()) return@nullable

                queues[msg.queueName]
                    ?.let { queueAlreadyExists(msg.queueName) }
                    ?: run {
                        queues[msg.queueName] = LinkedList()
                        msg.message.addReaction(okHand)
                    }
            }
            is JoinQueue -> nullable {
                val queue = queues[msg.queueName] ?: run {
                    queueDoesNotExist(msg.queueName)
                    return@nullable
                }
                val author = msg.message.getAuthorAsUser().bind()

                queue.takeUnless { it.contains(author) }?.add(author)
                msg.message.addReaction(okHand)
            }
            is LeaveQueue -> nullable {
                val queue = queues[msg.queueName] ?: run {
                    queueDoesNotExist(msg.queueName)
                    return@nullable
                }
                val author = msg.message.getAuthorAsUser().bind()

                queue.takeIf { it.contains(author) }?.remove(author)
                msg.message.addReaction(okHand)
            }
            is PopQueue -> nullable {
                val queue = queues[msg.queueName] ?: run {
                    queueDoesNotExist(msg.queueName)
                    return@nullable
                }
                val author = msg.message.getAuthorAsUser().bind()

                if (!author.isAdmin()) return@nullable

                val next = queue.poll()
                if (next == null) msg.message.channel.createMessage("Queue **${msg.queueName}** is empty!")
                else msg.message.channel.createMessage("@${next.userId} - you're up!")
            }
            is QueueStatus -> nullable {
                val queue = queues[msg.queueName] ?: run {
                    queueDoesNotExist(msg.queueName)
                    return@nullable
                }
                val author = msg.message.getAuthorAsUser().bind()
                if (!author.isAdmin()) return@nullable

                if (queue.isEmpty()) msg.message.channel.createMessage("Queue **${msg.queueName}** is empty")
                else {
                    val message = queue.pretty(indent = 4)
                    msg.message.channel.createMessage("${msg.queueName}\n$message")
                }
            }
            is DeleteQueue -> nullable {
                queues[msg.queueName] ?: run {
                    queueDoesNotExist(msg.queueName)
                    return@nullable
                }

                val author = msg.message.getAuthorAsUser().bind()
                if (!author.isAdmin()) return@nullable

                queues.remove(msg.queueName)
                msg.message.addReaction(okHand)
            }
            is ListQueues -> nullable {
                val author = msg.message.getAuthorAsUser().bind()
                if (!author.isAdmin()) return@nullable

                if (queues.isEmpty()) msg.message.channel.createMessage("No queues have been created yet")
                else {
                    val message = queues.toList().joinToString("\n") { (listName, users) ->
                        "**$listName\n**${users.pretty(4)}"
                    }
                    msg.message.channel.createMessage(message)
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

                msg.message.channel.createMessage(message)
            }
        }
    }
}

private fun Message.getAuthorAsUser(): User? = author?.let { a ->
    User(
        userId = "${a.username}#${a.discriminator}",
        roles = emptyList(),
    )
}

private val LOGGER = LoggerFactory.getLogger("QueueActor")
