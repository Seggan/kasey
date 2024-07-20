package io.github.seggan.kasey

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.seggan.kasey.errors.LoginException
import io.ktor.client.call.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import org.jsoup.nodes.Document
import java.text.Normalizer.Form
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private val logger = KotlinLogging.logger {}

class User(
    private val cookiesStorage: CookiesStorage = AcceptAllCookiesStorage(),
    private val loginHost: String = "meta.stackexchange.com",
) : AutoCloseable {

    private val client = constructClient(cookiesStorage)
    private var fkey: String by LoggedInProperty()
    private var userId: String by LoggedInProperty()
    private val rooms = mutableMapOf<String, Room>()

    /**
     * Logs in to Stack Exchange chat.
     *
     * @param email The email to log in with.
     * @param password The password to log in with.
     * @throws LoginException If the login fails.
     */
    suspend fun login(email: String, password: String) {
        logger.info { "Logging in as $email" }
        if (!client.cookies("stackexchange.com").any { it.name == "acct" }) {
            logger.info { "Cookie expired, logging in with password" }
            logger.debug { "Getting fkey" }
            val fkey = getFkey("https://$loginHost/users/login")
            logger.debug { "Logging in" }
            val response = doLogin(email, password, fkey)
            if (response != "Login-OK") {
                throw LoginException("Failed to log in: $response")
            }
            logger.debug { "Loading profile" }
            loadProfile(email, password, fkey)
        }

        logger.debug { "Getting chat info" }
        try {
            this.fkey = getFkey("https://chat.stackexchange.com/chats/join/favorite")
            this.userId = getUserId()
        } catch (e: Exception) {
            throw LoginException("Invalid credentials", e)
        }
        logger.info { "Logged in as $email" }
    }

    fun joinRoom(id: String): Room {
        return rooms.getOrPut(id) { Room(cookiesStorage, fkey, id) }
    }

    private suspend fun getFkey(url: String): String {
        val document = client.get(url).body<Document>()
        val fkey = document.select("input[name=fkey]").attr("value")
        if (fkey.isEmpty()) {
            throw LoginException("Failed to get fkey")
        }
        return fkey
    }

    private suspend fun loadProfile(email: String, password: String, fkey: String) {
        val response = client.submitForm(
            "https://$loginHost/users/login",
            formParameters = parameters {
                append("email", email)
                append("password", password)
                append("fkey", fkey)
                append("ssrc", "head")
            }
        )
        if (response.status == HttpStatusCode.Found) {
            return
        } else if ("Human verification" in response.body<Document>().select("title").first()!!.text()) {
            throw LoginException("Captcha required, wait a few minutes and try again.")
        } else {
            throw LoginException("Failed to load profile")
        }
    }

    private suspend fun getUserId(): String {
        val document = client.get("https://chat.stackexchange.com/chats/join/favorite").body<Document>()
        val userIdString = document.select(".topbar-menu-links > a").attr("href")
        val userId = userIdString.split("/").getOrNull(2)
        if (userId != null) {
            return userId
        } else if ("login" in userIdString) {
            throw LoginException("Invalid credentials")
        } else {
            throw LoginException("Failed to get user ID from '$userIdString'")
        }
    }

    private suspend fun doLogin(email: String, password: String, fkey: String): String {
        return client.submitForm(
            "https://$loginHost/users/login-or-signup/validation/track",
            formParameters = parameters {
                append("email", email)
                append("password", password)
                append("fkey", fkey)
                append("isSignup", "false")
                append("isLogin", "true")
                append("isPassword", "false")
                append("isAddLogin", "false")
                append("hasCaptcha", "false")
                append("ssrc", "head")
                append("submitButton", "Log in")
            }
        ).body<String>()
    }

    override fun close() {
        client.close()
        rooms.values.forEach(Room::close)
    }
}

private class LoggedInProperty<T> : ReadWriteProperty<Any?, T> {

    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: throw LoginException("User must be logged in before accessing ${property.name}")
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}