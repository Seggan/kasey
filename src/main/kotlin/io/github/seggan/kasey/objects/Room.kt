package io.github.seggan.kasey.objects

import io.github.seggan.kasey.ChatHost

data class Room(
    val host: ChatHost,
    val id: Long,
    val name: String
)
