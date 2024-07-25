package io.github.seggan.kasey

import io.ktor.client.call.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.util.date.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.lang.invoke.MethodHandles
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

class Test {

    private val email: String
    private val password: String

    init {
        val properties = Properties()
        Path("creds.properties").inputStream().use(properties::load)
        email = properties.getProperty("email")!!
        password = properties.getProperty("password")!!
    }

    @Test
    fun test() = runBlocking {
        val client = Client(AcceptAllCookiesStorage())
        client.login(email, password)
        val room = client.joinRoom(1u)
        room.sendMessage("Hello, world!")
        client.leaveRoom(1u)
    }
}