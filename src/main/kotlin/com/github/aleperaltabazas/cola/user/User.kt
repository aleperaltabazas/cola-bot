package com.github.aleperaltabazas.cola.user

data class User(
    val userId: String,
    val roles: List<String>,
) {
    // TODO: check the user's roles (make it configurable? :thinking:)
    fun isAdmin(): Boolean = true
}
