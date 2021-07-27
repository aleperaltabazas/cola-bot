package com.github.aleperaltabazas.cola.message

import com.typesafe.config.Config
import dev.kord.core.entity.Message
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

object ChannelHandler {
    operator fun invoke(f: suspend HandlerStrategy.() -> Unit) = runBlocking {
        val st = HandlerStrategy()
        try {
            st.f()
        } catch (e: GuardFailException) {
            LOGGER.debug("Interrupted")
            return@runBlocking
        }
    }


    private val LOGGER = LoggerFactory.getLogger(ChannelHandler::class.java)
}
