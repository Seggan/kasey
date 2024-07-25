package io.github.seggan.kasey.event

import io.github.seggan.kasey.Room
import io.github.seggan.kasey.errors.SeException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

enum class ChatEventType(val id: Int, private val constructor: ((JsonObject, Room) -> ChatEvent)? = null) {
    MESSAGE(1, ChatEvent::Message),
    EDIT(2, ChatEvent::Edit),
    JOIN(3, ChatEvent::Join),
    LEAVE(4, ChatEvent::Leave),
    NAME_CHANGE(5),
    MESSAGE_STARRED(6, ChatEvent::Star),
    DEBUG(7),
    MENTION(8, ChatEvent::Mention),
    FLAG(9),
    DELETE(10, ChatEvent::Delete),
    FILE_UPLOAD(11),
    MODERATOR_FLAG(12),
    SETTINGS_CHANGED(13),
    GLOBAL_NOTIFICATION(14),
    ACCESS_CHANGED(15),
    USER_NOTIFICATION(16),
    INVITATION(17),
    REPLY(18, ChatEvent::Reply),
    MESSAGE_MOVED_OUT(19),
    MESSAGE_MOVED_IN(20),
    TIME_BREAK(21),
    FEED_TICKER(22),
    USER_SUSPENSION(29),
    USER_MERGE(30),
    USER_NAME_OR_AVATAR_CHANGE(34);

    companion object {
        fun constructEvent(obj: JsonObject, room: Room): ChatEvent? {
            val type = obj["event_type"]!!.jsonPrimitive.int
            val eventType = entries.find { it.id == type } ?: throw SeException("Unknown event type: $type")
            val constructor = eventType.constructor ?: return null
            return constructor(obj, room)
        }
    }
}