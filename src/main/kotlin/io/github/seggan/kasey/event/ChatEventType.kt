package io.github.seggan.kasey.event

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

enum class ChatEventType(val id: Int, private val serializer: KSerializer<out ChatEvent>? = null) {
    MESSAGE(1, serializer<ChatEvent.Message>()),
    EDIT(2),
    JOIN(3),
    LEAVE(4),
    NAME_CHANGE(5),
    MESSAGE_STARRED(6),
    DEBUG(7),
    MENTION(8),
    FLAG(9),
    DELETE(10),
    FILE_UPLOAD(11),
    MODERATOR_FLAG(12),
    SETTINGS_CHANGED(13),
    GLOBAL_NOTIFICATION(14),
    ACCESS_CHANGED(15),
    USER_NOTIFICATION(16),
    INVITATION(17),
    REPLY(18),
    MESSAGE_MOVED_OUT(19),
    MESSAGE_MOVED_IN(20),
    TIME_BREAK(21),
    FEED_TICKER(22),
    USER_SUSPENSION(29),
    USER_MERGE(30),
    USER_NAME_OR_AVATAR_CHANGE(34);

    companion object {

        private val json = Json {
            ignoreUnknownKeys = true
        }

        fun constructEvent(obj: JsonObject): ChatEvent {
            val details = json.decodeFromJsonElement<ChatEventDetails>(obj)
            ChatEvent.currentDetails = details
            val typeNum = obj["event_type"]?.jsonPrimitive?.int
            val type = entries.first { it.id == typeNum }
            val serializer = type.serializer ?: throw UnsupportedOperationException("Event type $typeNum is not yet supported")
            return json.decodeFromJsonElement(serializer, obj)
        }
    }
}