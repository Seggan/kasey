package io.github.seggan.kasey

import io.ktor.client.plugins.cookies.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.inputStream

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
        val room = client.joinRoom(1)
        room.sendMessage("Hello, world!")
        client.leaveRoom(1)

        ChatHost.STACK_EXCHANGE.getAllRooms().collect(::println)
    }
}