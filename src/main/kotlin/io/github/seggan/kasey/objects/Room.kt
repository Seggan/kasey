package io.github.seggan.kasey.objects

import io.github.seggan.kasey.ChatHost
import io.github.seggan.kasey.constructClient
import io.ktor.client.call.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import org.jsoup.nodes.Document

data class Room(
    val host: ChatHost,
    val id: Long,
    val name: String
) {

    private lateinit var roomDocument: Document

    private suspend fun getRoomDocument() {
        if (!::roomDocument.isInitialized) {
            constructClient(AcceptAllCookiesStorage()).use { client ->
                roomDocument = client.get("${host.chatUrl}/rooms/info/$id").body()
            }
        }
    }

    suspend fun getDescription(): String {
        getRoomDocument()
        return roomDocument.selectFirst(".roomcard-xxl > p")?.text() ?: ""
    }

    suspend fun getParentSite(): String {
        getRoomDocument()
        return roomDocument.select("#header-logo > a").attr("href")
    }

    companion object {
        internal fun fromRoomDocument(document: Document, id: Long? = null): Room? {
            var host = document.selectFirst("title")
                ?.text()
                ?.split(" | ")
                ?.getOrNull(1)
                ?: return null
            host = "https://$host"
            val card = document.selectFirst(".roomcard-xxl") ?: return null
            val theId = id ?: card.attr("id").split("-").last().toLong()
            val name = card.selectFirst("h1")!!.text()
            return Room(
                ChatHost.entries.first { it.chatUrl == host },
                theId,
                name
            ).also { it.roomDocument = document }
        }
    }
}
