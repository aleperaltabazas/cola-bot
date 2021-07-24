package com.github.aleperaltabazas.cola.extensions

import com.github.aleperaltabazas.cola.model.User
import dev.kord.core.entity.Message

fun Message.getAuthorAsUser(): User? = author?.let { a ->
    User(
        userId = "${a.username}#${a.discriminator}",
        roles = emptyList(),
    )
}
