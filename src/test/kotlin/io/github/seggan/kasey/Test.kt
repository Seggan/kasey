package io.github.seggan.kasey

import dev.reformator.stacktracedecoroutinator.runtime.DecoroutinatorRuntime
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
        println(ChatHost.STACK_EXCHANGE.getRoom(240)!!.getDescription())
    }
}