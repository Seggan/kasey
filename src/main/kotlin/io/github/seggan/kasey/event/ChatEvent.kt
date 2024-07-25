package io.github.seggan.kasey.event

import io.github.seggan.kasey.Room
import io.github.seggan.kasey.objects.Message as AMessage
import io.github.seggan.kasey.objects.User
import io.github.seggan.kasey.ulong
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.time.Instant

sealed class ChatEvent(jsonObject: JsonObject, val room: Room) {

    abstract val type: ChatEventType

    val timestamp = Instant.ofEpochSecond(jsonObject["time_stamp"]!!.jsonPrimitive.long)
    val user = User(
        jsonObject["user_id"]!!.jsonPrimitive.ulong,
        jsonObject["user_name"]!!.jsonPrimitive.content
    )

    sealed class MessageBase(jsonObject: JsonObject, room: Room) : ChatEvent(jsonObject, room) {
        val messageId = jsonObject["message_id"]!!.jsonPrimitive.ulong

        suspend fun getMessage(): AMessage? {
            return room.getMessage(messageId)
        }
    }

    class Message(jsonObject: JsonObject, room: Room) : MessageBase(jsonObject, room) {
        override val type = ChatEventType.MESSAGE
    }

    class Edit(jsonObject: JsonObject, room: Room) : MessageBase(jsonObject, room) {
        override val type = ChatEventType.EDIT
    }

    class Join(jsonObject: JsonObject, room: Room) : ChatEvent(jsonObject, room) {
        override val type = ChatEventType.JOIN
    }

    class Leave(jsonObject: JsonObject, room: Room) : ChatEvent(jsonObject, room) {
        override val type = ChatEventType.LEAVE
    }

    class Star(jsonObject: JsonObject, room: Room) : MessageBase(jsonObject, room) {
        override val type = ChatEventType.MESSAGE_STARRED

        val starred = jsonObject["message_starred"]?.jsonPrimitive?.boolean ?: false
        val pinned = jsonObject["message_owner_starred"]?.jsonPrimitive?.boolean ?: false
    }

    sealed class Ping(jsonObject: JsonObject, room: Room) : MessageBase(jsonObject, room) {
        val targetUserId = jsonObject["target_user_id"]!!.jsonPrimitive.ulong
        val parentMessageId = jsonObject["parent_message_id"]!!.jsonPrimitive.ulong
    }

    class Mention(jsonObject: JsonObject, room: Room) : Ping(jsonObject, room) {
        override val type = ChatEventType.MENTION
    }

    class Delete(jsonObject: JsonObject, room: Room) : MessageBase(jsonObject, room) {
        override val type = ChatEventType.DELETE
    }

    class Reply(jsonObject: JsonObject, room: Room) : Ping(jsonObject, room) {
        override val type = ChatEventType.REPLY
    }
}