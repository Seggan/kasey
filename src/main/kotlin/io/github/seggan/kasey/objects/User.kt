package io.github.seggan.kasey.objects

import io.github.seggan.kasey.errors.LoginException
import org.jsoup.nodes.Element

data class User(val id: ULong, val name: String) {
    companion object {
        internal fun fromLink(link: Element): User? {
            val username = link.text()
            val userIdString = link.attr("href")
            val userId = userIdString.split("/").getOrNull(2)
            return if (userId != null) {
                User(userId.toULong(), username)
            } else if ("login" in userIdString) {
                null
            } else {
                throw IllegalArgumentException("Failed to get user ID from ${link.html()}'")
            }
        }
    }
}
