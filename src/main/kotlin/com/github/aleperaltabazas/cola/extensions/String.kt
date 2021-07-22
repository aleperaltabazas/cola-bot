package com.github.aleperaltabazas.cola.extensions

fun String.words() = split(" ")
    .map { it.trim() }
    .filterNot { it.isEmpty() }
