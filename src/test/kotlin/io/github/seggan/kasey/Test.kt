package io.github.seggan.kasey

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.util.Properties
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
        val user = User()
        user.login(email, password)
        val room = user.joinRoom("1")
        while (true) {
            println(room.nextEvent())
        }
    }
}