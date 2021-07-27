package com.github.aleperaltabazas.cola.types

import kotlinx.coroutines.channels.SendChannel

typealias Actor<T> = SendChannel<T>
