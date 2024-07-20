package io.github.seggan.kasey.event

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

sealed interface ChatEvent {

    val type: ChatEventType
    val details: ChatEventDetails

    @Serializable
    data class Message(
        @Transient override val details: ChatEventDetails = currentDetails,
        val content: String
    ) : ChatEvent {
        @Transient override val type = ChatEventType.MESSAGE
    }


    companion object {
        internal lateinit var currentDetails: ChatEventDetails
    }
}