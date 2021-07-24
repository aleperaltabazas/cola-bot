package com.github.aleperaltabazas.cola.model

import java.util.*

typealias UserQueueMap = MutableMap<String, Queue<User>>

@JvmInline
value class ChannelQueues(
    private val inner: MutableMap<String, UserQueueMap> = mutableMapOf(),
) {
    fun get(channelId: String, queue: String): Queue<User>? = inner[channelId]?.get(queue)

    fun get(channelId: String): UserQueueMap = getOrRegister(channelId)

    fun exists(channelId: String, queue: String) = inner[channelId]?.get(queue) != null

    fun push(channelId: String, queue: String, user: User) {
        val q = get(channelId, queue)

        q?.add(user) ?: kotlin.run { getOrRegister(channelId) }
    }

    fun new(channelId: String, queue: String): Queue<User> = getOrRegister(channelId)
        .let {
            val q = LinkedList<User>()
            it[queue] = q
            q
        }

    fun delete(channelId: String, queue: String) {
        inner[channelId]?.remove(queue)
    }

    private fun getOrRegister(channelId: String): UserQueueMap = inner.getOrPut(channelId) { mutableMapOf() }
}
