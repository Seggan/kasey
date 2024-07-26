package io.github.seggan.kasey

import io.github.seggan.kasey.objects.Room
import io.ktor.client.call.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document

/**
 * One of the three chat hosts.
 */
enum class ChatHost(host: String) {

    STACK_OVERFLOW("stackoverflow.com"),
    META_STACK_EXCHANGE("meta.stackexchange.com"),
    STACK_EXCHANGE("stackexchange.com");

    val chatUrl = "https://chat.$host"

    private val client = constructClient(AcceptAllCookiesStorage())

    /**
     * Gets all rooms as a [Flow], sorted by activity.
     */
    fun getAllRooms(): Flow<Room> = flow {
        val client = constructClient(AcceptAllCookiesStorage())
        val params = mapOf(
            "tab" to "all",
            "sort" to "active",
            "filter" to "",
            "pageSize" to "21"
        )

        val pageDocument = client.get("$chatUrl/rooms") {
            parameter("tab", "all")
            parameter("sort", "active")
        }.body<Document>()
        val pages = pageDocument.select(".page-numbers")
            .mapNotNull { it.text().toIntOrNull() }
            .max()
        val previous = mutableSetOf<Room>()
        for (page in 1..pages) {
            val document = client.submitForm(
                "$chatUrl/rooms",
                formParameters = parameters {
                    for ((key, value) in params) {
                        append(key, value)
                    }
                    append("page", page.toString())
                }
            ).body<Document>()
            val rooms = document.select(".room-name > a")
                .map { room ->
                    val id = room.attr("href").split('/')[2].toLong()
                    Room(this@ChatHost, id, room.text())
                }
            for (room in rooms) {
                if (room !in previous) {
                    previous.add(room)
                    emit(room)
                }
            }
        }
        client.close()
    }

    suspend fun getRoom(id: Long): Room? {
        val document = try {
            client.get("$chatUrl/rooms/info/$id").body<Document>()
        } catch (e: Exception) {
            return null
        }
        return Room.fromRoomDocument(document, id)
    }
}