package io.github.seggan.kasey

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.seggan.kasey.event.ChatEvent
import io.github.seggan.kasey.event.ChatEventType
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.util.reflect.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

class Room internal constructor(
    cookiesStorage: CookiesStorage,
    private val fkey: String,
    private val id: String
) : AutoCloseable {

    private val client = constructClient(cookiesStorage)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val events = Channel<ChatEvent>(Channel.UNLIMITED)

    init {
        logger.info { "Joining room $id" }
        scope.launch {
            val client = constructClient(cookiesStorage) {
                install(WebSockets) {
                    contentConverter = KotlinxWebsocketSerializationConverter(Json {
                        ignoreUnknownKeys = true
                    })
                }
                install(Logging)
            }
            while (true) {
                logger.debug { "Obtaining WS URL" }
                val response = client.submitForm(
                    "https://chat.stackexchange.com/ws-auth",
                    formParameters = parameters {
                        append("roomid", id)
                        append("fkey", fkey)
                    }
                ).body<String>()
                logger.debug { "Received $response" }
                var url = response.let(Json::parseToJsonElement).jsonObject["url"]?.jsonPrimitive?.content
                if (url != null) {
                    url = "$url?l=${System.currentTimeMillis() / 1000}"
                    logger.debug { "Connecting to websocket $url" }
                    onWsConnection(url, client)
                }
            }
        }
    }

    private suspend fun onWsConnection(url: String, client: HttpClient) {
        val roomKey = "r$id"
        client.webSocket({
            method = HttpMethod.Get
            url {
                takeFrom(url)
                port = 443
            }
            header(HttpHeaders.Origin, "https://chat.stackexchange.com")
        }) {
            logger.debug { "Connected to websocket" }
            for (message in incoming) {
                val events = deserialize<JsonObject>(message)
                    .filterKeys { it == roomKey }
                    .values.asSequence()
                    .mapNotNull { it.jsonObject["e"] }
                    .map { it.jsonArray.first().jsonObject }
                    .map(ChatEventType::constructEvent)
                for (event in events) {
                    this@Room.events.send(event)
                }
            }
        }
    }

    suspend fun nextEvent(): ChatEvent {
        return events.receive()
    }

    override fun close() {
        scope.cancel()
        client.close()
    }
}

private suspend inline fun <reified T> DefaultClientWebSocketSession.deserialize(frame: Frame): T {
    return converter!!.deserialize(StandardCharsets.UTF_8, typeInfo<T>(), frame) as T
}