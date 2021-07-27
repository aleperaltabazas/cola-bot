package com.github.aleperaltabazas.cola.message

import org.slf4j.LoggerFactory

object ChannelHandler {
    suspend operator fun invoke(f: suspend HandlerStrategy.() -> Unit) {
        val st = HandlerStrategy()
        try {
            st.f()
        } catch (e: GuardFailException) {
            LOGGER.debug("Interrupted")
            return
        }
    }


    private val LOGGER = LoggerFactory.getLogger(ChannelHandler::class.java)
}
