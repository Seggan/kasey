package io.github.seggan.kasey

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.seggan.kasey.errors.BadResponseException
import io.github.seggan.kasey.errors.RatelimitException
import io.github.seggan.kasey.event.ChatEvent
import io.github.seggan.kasey.event.ChatEventType
import io.github.seggan.kasey.objects.Message
import io.ktor.client.call.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.util.date.*
import io.ktor.util.reflect.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.*
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val logger = KotlinLogging.logger {}

class Room internal constructor(
    private val cookiesStorage: CookiesStorage,
    private val fkey: String,
    val id: ULong,
    val client: Client
) : AutoCloseable {

    private val httpClient = constructClient(cookiesStorage)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val eventHandlers = mutableMapOf<UUID, suspend (ChatEvent) -> Unit>()

    internal suspend fun join() {
        val latch = Channel<Unit>()
        logger.info { "Joining room $id" }
        scope.launch {
            val httpClient = constructClient(cookiesStorage) {
                install(WebSockets) {
                    contentConverter = KotlinxWebsocketSerializationConverter(Json {
                        ignoreUnknownKeys = true
                    })
                }
            }
            while (true) {
                logger.info { "Obtaining WS URL" }
                val response = httpClient.submitForm(
                    "${client.host.chatUrl}/ws-auth",
                    formParameters = parameters {
                        append("roomid", id.toString())
                        append("fkey", fkey)
                    }
                ).body<JsonObject>()
                logger.debug { "Received $response" }
                var url = response["url"]?.jsonPrimitive?.content
                if (url != null) {
                    url = "$url?l=${getTimeMillis() / 1000}"
                    logger.debug { "Connecting to websocket $url" }
                    httpClient.webSocket({
                        method = HttpMethod.Get
                        url {
                            takeFrom(url)
                            port = 443
                        }
                        header(HttpHeaders.Origin, client.host.chatUrl)
                    }) {
                        latch.send(Unit)
                        logger.info { "Connected to websocket" }
                        runWs()
                    }
                }
            }
        }
        latch.receive()
    }

    private suspend fun DefaultClientWebSocketSession.runWs() {
        val roomKey = "r$id"
        for (message in incoming) {
            logger.debug { "Received ${message.data.decodeToString()}" }
            val events = deserialize<JsonObject>(message)
                .filterKeys { it == roomKey }
                .values.asSequence()
                .mapNotNull { it.jsonObject["e"] }
                .map { it.jsonArray.first().jsonObject }
                .mapNotNull { ChatEventType.constructEvent(it, this@Room) }
            for (event in events) {
                for (handler in eventHandlers.values) {
                    scope.launch {
                        handler(event)
                    }
                }
            }
        }
    }

    internal suspend fun request(path: String, params: Map<String, String> = emptyMap()): HttpResponse {
        val url = client.host.chatUrl + path
        logger.debug { "Requesting $url with $params" }
        val response = httpClient.submitForm(
            url,
            formParameters = parameters {
                for ((key, value) in params) {
                    append(key, value)
                }
                append("fkey", fkey)
            }
        ) {
            header(HttpHeaders.Referrer, "${client.host.chatUrl}/rooms/$id")
        }
        if (response.status.isSuccess()) {
            return response
        } else if (response.status == HttpStatusCode.Conflict) {
            throw RatelimitException()
        } else {
            throw BadResponseException(response, response.body<String>())
        }
    }

    suspend fun loadPreviousMessages(count: Int): List<Message> {
        val response = request(
            "/chats/$id/events",
            mapOf("mode" to "Messages", "msgCount" to count.toString(), "since" to "0")
        ).body<JsonObject>()
        return response["events"]!!.jsonArray.mapNotNull { Message.fromJson(it.jsonObject, this) }
    }

    suspend fun sendMessage(message: String): Message {
        val json = request("/chats/$id/messages/new", mapOf("text" to message))
            .body<JsonObject>()
        val id = json["id"]!!.jsonPrimitive.ulong
        val time = json["time"]!!.jsonPrimitive.long
        logger.debug { "Sent message $id" }
        return Message(
            id,
            message,
            0,
            null,
            client.user,
            Instant.ofEpochSecond(time),
            this
        )
    }

    /**
     * Fetches a message by its ID.
     *
     * @param message The ID of the message.
     * @return The message, or null if it doesn't exist or was deleted.
     */
    suspend fun getMessage(message: ULong): Message? {
        val json = request(
            "/chats/$id/events",
            mapOf("mode" to "Messages", "msgCount" to "2", "before" to (message + 1u).toString())
        ).body<JsonObject>()
        return json["events"]!!.jsonArray
            .mapNotNull { Message.fromJson(it.jsonObject, this) }
            .firstOrNull { it.id == message }
    }

    fun registerEventHandler(handler: suspend (ChatEvent) -> Unit): UUID {
        val id = UUID.randomUUID()
        eventHandlers[id] = handler
        return id
    }

    fun unregisterEventHandler(id: UUID) {
        eventHandlers.remove(id)
    }

    suspend inline fun <reified T : ChatEvent> waitForEvent(): T {
        lateinit var handler: UUID
        return suspendCoroutine { cont ->
            handler = registerEventHandler {
                if (it is T) {
                    cont.resume(it)
                }
            }
        }.also {
            unregisterEventHandler(handler)
        }
    }

    private var closed: Boolean = false

    override fun close() {
        if (closed) return

        runBlocking {
            try {
                request("/chats/leave/$id")
            } catch (e: BadResponseException) {
                if (e.response.status != HttpStatusCode.Found) {
                    throw e
                } else {
                    // If you remove this branch, the linter will complain for some reason
                    // I'm too lazy to submit a bug report
                }
            }
        }
        eventHandlers.clear()
        scope.cancel()
        httpClient.close()
        closed = true

        client.leaveRoom(id)
    }
}

private suspend inline fun <reified T> DefaultClientWebSocketSession.deserialize(frame: Frame): T {
    return converter!!.deserialize(StandardCharsets.UTF_8, typeInfo<T>(), frame) as T
}

private fun HttpResponse.nullIfNotOk() = if (status.isSuccess()) this else null