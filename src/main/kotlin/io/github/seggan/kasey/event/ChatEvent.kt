package io.github.seggan.kasey.event

import io.github.seggan.kasey.objects.User
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

import io.github.seggan.kasey.objects.Message as MessageObject

sealed interface ChatEvent {

    val type: ChatEventType
    val details: ChatEventDetails

    @Serializable
    data class Message internal constructor(
        @Transient override val details: ChatEventDetails = currentDetails,
        val content: String
    ) : ChatEvent {

        @Transient
        override val type = ChatEventType.MESSAGE

        fun asMessage(): MessageObject {
            val author = User(details.userId, details.username)
            return MessageObject(details.messageId, content, author, details.timestamp, details.room)
        }
    }

    companion object {
        internal lateinit var currentDetails: ChatEventDetails
    }
}