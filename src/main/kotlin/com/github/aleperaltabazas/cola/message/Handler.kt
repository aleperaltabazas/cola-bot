package com.github.aleperaltabazas.cola.message

import com.typesafe.config.Config
import dev.kord.core.entity.Message
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

sealed class ChannelHandler {
    operator fun invoke(message: Message, f: suspend HandlerStrategy.() -> Unit) = runBlocking {
        val st = strategy(message)
        try {
            st.f()
        } catch (e: GuardFailException) {
            LOGGER.debug("Interrupted")
            return@runBlocking
        }
    }

    protected abstract fun strategy(message: Message): HandlerStrategy

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ChannelHandler::class.java)

        operator fun invoke(config: Config) = when (val handler = config.getString("message.handler")?.lowercase()) {
            "prod" -> ProdHandler
            "test" -> FakeHandler
            else -> throw IllegalArgumentException("Unknown strategy $handler")
        }
    }
}

object ProdHandler : ChannelHandler() {
    override fun strategy(message: Message): HandlerStrategy = ProdStrategy(message)
}

object FakeHandler : ChannelHandler() {
    override fun strategy(message: Message): HandlerStrategy = FakeStrategy(message)
}
