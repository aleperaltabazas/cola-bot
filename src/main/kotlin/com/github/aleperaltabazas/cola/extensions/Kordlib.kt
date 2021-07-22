package com.github.aleperaltabazas.cola.extensions

import com.github.aleperaltabazas.cola.user.User
import dev.kord.core.entity.Message

fun Message.getAuthorAsUser(): User? = author?.let { a ->
    User(
        userId = "${a.username}#${a.discriminator}",
        roles = emptyList(),
    )
}
