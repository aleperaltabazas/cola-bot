package com.github.aleperaltabazas.cola.extensions

import com.github.aleperaltabazas.cola.model.User
import dev.kord.core.entity.Message
import kotlinx.coroutines.flow.toList

suspend fun Message.getAuthorAsUser(): User? = author?.let { a ->
    User(
        id = a.id.asString,
        discriminator = a.discriminator,
        username = "${a.username}#${a.discriminator}",
        roles = getAuthorAsMember()?.roles?.toList()?.map { it.name } ?: emptyList(),
        serverNickname = getAuthorAsMember()?.nickname,
    )
}
