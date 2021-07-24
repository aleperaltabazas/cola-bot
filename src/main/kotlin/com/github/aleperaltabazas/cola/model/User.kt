package com.github.aleperaltabazas.cola.model

import com.github.aleperaltabazas.cola.ADMIN_ROLE

data class User(
    val id: String,
    val discriminator: String,
    val username: String,
    val roles: List<String>,
    val serverNickname: String?,
) {
    fun isAdmin(): Boolean = roles.contains(ADMIN_ROLE)
}
