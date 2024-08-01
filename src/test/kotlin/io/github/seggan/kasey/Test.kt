package io.github.seggan.kasey

import dev.reformator.stacktracedecoroutinator.runtime.DecoroutinatorRuntime
import io.github.seggan.kasey.event.ChatEvent
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
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
        DecoroutinatorRuntime.load()
    }

    @Test
    fun test() = runBlocking {
        val client = Client()
        client.login(email, password)
        val room = client.joinRoom(1)!!
        room.eventFlow()
            .filterIsInstance<ChatEvent.Message>()
            .map { it.getMessage() }.
            collect(::println)
    }
}