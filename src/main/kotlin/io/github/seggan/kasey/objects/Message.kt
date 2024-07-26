package io.github.seggan.kasey.objects

import io.github.seggan.kasey.JoinedRoom
import io.github.seggan.kasey.errors.SeException
import io.ktor.client.call.*
import kotlinx.serialization.json.*
import org.jsoup.nodes.Document
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Represents a message in a chat room.
 */
class Message(
    val id: Long,
    content: String,
    stars: Int,
    clientStarring: User?,
    val author: User,
    val timestamp: Instant,
    val room: JoinedRoom
) {

    var content = content
        private set

    var stars = stars
        private set

    /**
     * The user that starred the message or null if the client hasn't starred it.
     */
    var clientStarring = clientStarring
        private set

    /**
     * Gets the Markdown content of the message.
     */
    suspend fun getMarkdownContent(): String {
        return room.request("/messages/${room.room.id}/$id").body<String>()
    }

    /**
     * Replies to the message.
     *
     * @return The message that was sent as a reply.
     */
    suspend fun reply(content: String): Message {
        return room.sendMessage(":$id $content")
    }

    /**
     * Gets the message that this message is replying to.
     *
     * @return The message that this message is replying to, or null if it isn't a reply.
     */
    suspend fun getReplyingTo(): Message? {
        if (!content.startsWith(":")) return null
        val replyId = content.substringAfter(":").substringBefore(" ").toLongOrNull() ?: return null
        return room.getMessage(replyId)
    }

    /**
     * Checks if the message is editable.
     *
     * @return true if the message is editable, false otherwise.
     */
    suspend fun isEditable(): Boolean {
        val document = room.request(
            "/messages/$id/history",
            emptyMap()
        ).body<Document>()
        val time = LocalTime.parse(
            document.select(".timestamp").last()!!.text(),
            MESSAGE_TIME_FORMATTER
        )
        return ChronoUnit.SECONDS.between(time, LocalTime.now(ZoneOffset.UTC)) < EDIT_WINDOW_SECONDS
    }

    /**
     * Edits the message.
     */
    suspend fun edit(newContent: String) {
        val result = room.request("/messages/$id", mapOf("text" to newContent)).body<JsonElement>()
        if (result.jsonPrimitive.content != "ok") {
            throw SeException("Failed to edit message $id: $result")
        }
        content = newContent
    }

    /**
     * Stars the message if it isn't already starred, or unstars it if it is.
     */
    suspend fun toggleStar() {
        val result = room.request("/messages/$id/star").body<JsonElement>()
        if (result.jsonPrimitive.content != "ok") {
            throw SeException("Failed to ${if (clientStarring != null) "unstar" else "star"} message $id: $result")
        }
        stars += if (clientStarring != null) -1 else 1
        clientStarring = if (clientStarring != null) null else room.client.user
    }

    /**
     * Deletes the message.
     */
    suspend fun delete() {
        val result = room.request("/messages/$id/delete").body<JsonElement>()
        if (result.jsonPrimitive.content != "ok") {
            throw SeException("Failed to delete message $id: $result")
        }
    }

    override fun equals(other: Any?): Boolean = other is Message && other.id == id
    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Message(id=$id, content=$content, stars=$stars, author=$author, timestamp=$timestamp)"
    }

    companion object {
        private val MESSAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneOffset.UTC)
        private const val EDIT_WINDOW_SECONDS = 115

        internal fun fromJson(json: JsonObject, room: JoinedRoom): Message? {
            val id = json["message_id"]!!.jsonPrimitive.long
            val content = json["content"]?.jsonPrimitive?.content ?: return null
            val stars = json["message_stars"]?.jsonPrimitive?.int ?: 0
            val starred = json["message_starred"]?.jsonPrimitive?.boolean ?: false
            val clientStarring = if (starred) room.client.user else null
            val author = User(
                json["user_id"]!!.jsonPrimitive.long,
                json["user_name"]!!.jsonPrimitive.content
            )
            val timestamp = Instant.ofEpochSecond(json["time_stamp"]!!.jsonPrimitive.long)
            return Message(id, content, stars, clientStarring, author, timestamp, room)
        }
    }
}
