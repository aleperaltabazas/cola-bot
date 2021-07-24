package com.github.aleperaltabazas.cola.model

import com.github.aleperaltabazas.cola.ADMIN_ROLE

data class User(
    val userId: String,
    val roles: List<String>,
) {
    fun isAdmin(): Boolean = roles.contains(ADMIN_ROLE)
}
