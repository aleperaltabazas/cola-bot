package com.github.aleperaltabazas.cola.extensions

import com.github.aleperaltabazas.cola.model.User
import dev.kord.common.entity.OverwriteType
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import dev.kord.rest.builder.channel.PermissionOverwriteBuilder
import dev.kord.rest.builder.channel.TextChannelCreateBuilder
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock

suspend fun Message.getAuthorAsUser(): User? = author?.let { a ->
    User(
        id = a.id.asString,
        discriminator = a.discriminator,
        username = "${a.username}#${a.discriminator}",
        roles = getAuthorAsMember()?.roles?.toList()?.map { it.name } ?: emptyList(),
        serverNickname = getAuthorAsMember()?.nickname,
    )
}

fun TextChannelCreateBuilder.addRoleOverwrite(
    id: Snowflake = Snowflake(Clock.System.now()),
    builder: PermissionOverwriteBuilder.() -> Unit = {},
) {
    val overwrite = PermissionOverwriteBuilder(type = OverwriteType.Role, id = id).apply(builder).toOverwrite()
    permissionOverwrites.add(overwrite)
}
