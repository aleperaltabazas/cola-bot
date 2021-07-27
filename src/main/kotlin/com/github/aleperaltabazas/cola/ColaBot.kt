package com.github.aleperaltabazas.cola

import com.github.aleperaltabazas.cola.actors.QueueActor
import com.github.aleperaltabazas.cola.handler.GuildCreateHandler
import com.github.aleperaltabazas.cola.handler.MessageCreateHandler
import com.github.aleperaltabazas.cola.handler.ReactionAddHandler
import com.github.aleperaltabazas.cola.handler.ReactionDeleteHandler
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import dev.kord.core.Kord
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
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
        ?.toIntOrNull()

    if (port != null) {
        // So Heroku doesn't think the app crashed
        LOGGER.info("Using port $port")

        Spark.port(port)
        Spark.get("/*") { _, _ -> "I feel fantastic and I'm still alive" }
    }

    // If this starts growing, consider using dependency injection
    val client = Kord(CONFIG.getString("discord.bot.token"))
    val queuesActor = QueueActor(this)

    val handlers = listOf(
        GuildCreateHandler(
            client = client,
        ),
        MessageCreateHandler(
            client = client,
            queuesActor = queuesActor
        ),
        ReactionAddHandler(
            client = client,
            queuesActor = queuesActor,
        ),
        ReactionDeleteHandler(
            client = client,
            queuesActor = queuesActor,
        ),
    )

    handlers.forEach { it.register() }

    client.login()
}

private val LOGGER = LoggerFactory.getLogger("ColaBot")
