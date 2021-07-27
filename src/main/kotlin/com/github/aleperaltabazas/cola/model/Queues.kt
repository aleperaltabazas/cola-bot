package com.github.aleperaltabazas.cola.model

import dev.kord.core.entity.Message
import java.util.*

typealias UserQueueMap = MutableSet<UserQueue>

operator fun UserQueueMap.get(queueName: String) = find { it.queueName == queueName }

data class UserQueue(
    val queueName: String,
    val queue: Queue<User>,
    val message: Message,
) : Queue<User> by queue

@JvmInline
value class ChannelQueues(
    private val inner: MutableMap<String, UserQueueMap> = mutableMapOf(),
) {
    fun get(channelId: String, queue: String): UserQueue? = inner[channelId]?.get(queue)

    fun getByMessageId(channelId: String, messageId: String) = inner[channelId]?.find { it.message.id.asString == messageId }

    fun get(channelId: String): UserQueueMap = getOrRegister(channelId)

    fun exists(channelId: String, queue: String) = inner[channelId]?.find { it.queueName == queue } != null

    fun push(channelId: String, queue: String, user: User) {
        val q = get(channelId, queue)

        q?.add(user) ?: kotlin.run { getOrRegister(channelId) }
    }

    fun new(channelId: String, message: Message, queueName: String): UserQueue = getOrRegister(channelId)
        .let {
            val q = UserQueue(
                message = message,
                queue = LinkedList(),
                queueName = queueName
            )
            it.add(q)
            q
        }

    fun delete(channelId: String, queue: String) {
        inner[channelId]?.removeIf { it.queueName == queue }
    }

    private fun getOrRegister(channelId: String): UserQueueMap = inner.getOrPut(channelId) { mutableSetOf() }
}
