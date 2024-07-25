package io.github.seggan.kasey.event

import io.github.seggan.kasey.Room
import io.github.seggan.kasey.objects.User
import io.github.seggan.kasey.ulong
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.time.Instant
import io.github.seggan.kasey.objects.Message as AMessage

/**
 * Represents an event that occurred in a chat room.
 */
sealed class ChatEvent(jsonObject: JsonObject, val room: Room) {

    abstract val type: ChatEventType

    val timestamp = Instant.ofEpochSecond(jsonObject["time_stamp"]!!.jsonPrimitive.long)

    /**
     * The user that triggered the event.
     */
    val user = User(
        jsonObject["user_id"]!!.jsonPrimitive.ulong,
        jsonObject["user_name"]!!.jsonPrimitive.content
    )

    sealed class MessageBase(jsonObject: JsonObject, room: Room) : ChatEvent(jsonObject, room) {
        val messageId = jsonObject["message_id"]!!.jsonPrimitive.ulong

        /**
         * Gets the actual message object from the room.
         *
         * @return The message object, or null if it doesn't exist.
         */
        suspend fun getMessage(): AMessage? {
            return room.getMessage(messageId)
        }
    }

    /**
     * Invoked when a message is sent.
     */
    class Message internal constructor(jsonObject: JsonObject, room: Room) : MessageBase(jsonObject, room) {
        override val type = ChatEventType.MESSAGE
    }

    /**
     * Invoked when a message is edited.
     */
    class Edit internal constructor(jsonObject: JsonObject, room: Room) : MessageBase(jsonObject, room) {
        override val type = ChatEventType.EDIT
    }

    /**
     * Invoked when a user joins the room.
     */
    class Join internal constructor(jsonObject: JsonObject, room: Room) : ChatEvent(jsonObject, room) {
        override val type = ChatEventType.JOIN
    }

    /**
     * Invoked when a user leaves the room.
     */
    class Leave internal constructor(jsonObject: JsonObject, room: Room) : ChatEvent(jsonObject, room) {
        override val type = ChatEventType.LEAVE
    }

    /**
     * Invoked when a message is starred or unstarred.
     */
    class Star internal constructor(jsonObject: JsonObject, room: Room) : MessageBase(jsonObject, room) {
        override val type = ChatEventType.MESSAGE_STARRED

        val starred = jsonObject["message_starred"]?.jsonPrimitive?.boolean ?: false
        val pinned = jsonObject["message_owner_starred"]?.jsonPrimitive?.boolean ?: false
    }

    sealed class Ping(jsonObject: JsonObject, room: Room) : MessageBase(jsonObject, room) {

        /**
         * The user that was mentioned.
         */
        val targetUserId = jsonObject["target_user_id"]!!.jsonPrimitive.ulong

        /**
         * The message that was replied to.
         */
        val parentMessageId = jsonObject["parent_message_id"]!!.jsonPrimitive.ulong
    }

    /**
     * Invoked when a user is mentioned in a message.
     */
    class Mention internal constructor(jsonObject: JsonObject, room: Room) : Ping(jsonObject, room) {
        override val type = ChatEventType.MENTION
    }

    /**
     * Invoked when a message is deleted.
     */
    class Delete internal constructor(jsonObject: JsonObject, room: Room) : MessageBase(jsonObject, room) {
        override val type = ChatEventType.DELETE
    }

    /**
     * Invoked when a message is replied to.
     */
    class Reply internal constructor(jsonObject: JsonObject, room: Room) : Ping(jsonObject, room) {
        override val type = ChatEventType.REPLY
    }
}