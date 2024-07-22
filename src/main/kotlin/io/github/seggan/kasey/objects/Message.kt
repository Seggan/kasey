package io.github.seggan.kasey.objects

import io.github.seggan.kasey.Room
import io.github.seggan.kasey.errors.SeException
import io.github.seggan.kasey.event.ChatEvent
import io.github.seggan.kasey.ulong
import io.ktor.client.call.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.jsoup.nodes.Document
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class Message(
    val id: ULong,
    content: String,
    val author: User,
    val timestamp: Instant,
    val room: Room
) {

    var content = content
        private set

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

    suspend fun edit(editor: (String) -> String) {
        content = editor(content)
        val result = room.request("/messages/$id", mapOf("text" to content)).body<JsonElement>()
        if (result.jsonPrimitive.content != "ok") {
            throw SeException("Failed to edit message $id: $result")
        }
    }

    override fun equals(other: Any?): Boolean = other is Message && other.id == id
    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Message(id=$id, content=$content, author=$author, timestamp=$timestamp)"
    }

    companion object {
        private val MESSAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneOffset.UTC)
        private const val EDIT_WINDOW_SECONDS = 115

        internal fun fromJson(json: JsonObject, room: Room): Message? {
            val id = json["message_id"]!!.jsonPrimitive.ulong
            val content = json["content"]?.jsonPrimitive?.content ?: return null
            val author = User(
                json["user_id"]!!.jsonPrimitive.ulong,
                json["user_name"]!!.jsonPrimitive.content
            )
            val timestamp = Instant.ofEpochSecond(json["time_stamp"]!!.jsonPrimitive.long)
            return Message(id, content, author, timestamp, room)
        }
    }
}
