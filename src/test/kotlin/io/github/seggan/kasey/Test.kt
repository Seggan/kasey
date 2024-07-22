package io.github.seggan.kasey

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
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.writeText

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
        val client = Client(FileCookieStore(Path("cookies.json")))
        client.login(email, password)
        val room = client.joinRoom(1u)
        delay(1000)
        room.sendMessage("Hello, world!").edit { it.reversed() }
        client.leaveRoom(1u)
    }
}

private class FileCookieStore private constructor(
    private val file: Path,
    private val delegate: CookiesStorage
) : CookiesStorage by delegate {

    companion object {
        private val cookiesHandle = MethodHandles
            .privateLookupIn(AcceptAllCookiesStorage::class.java, MethodHandles.lookup())
            .findVarHandle(
                AcceptAllCookiesStorage::class.java,
                "container",
                MutableList::class.java
            )
    }

    @Suppress("UNCHECKED_CAST")
    private val cookies: List<Cookie>
        get() = cookiesHandle.get(delegate) as List<Cookie>

    constructor(path: Path) : this(path, AcceptAllCookiesStorage())

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        delegate.addCookie(requestUrl, cookie)
        save()
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        return delegate.get(requestUrl).also { save() }
    }

    override fun close() {
        delegate.close()
        save()
    }

    private fun save() {
        val surrogates = cookies.map(CookieSurrogate::fromCookie)
        val json = Json.encodeToString(surrogates)
        file.writeText(json)
    }
}

@Serializable
private class CookieSurrogate(
    val name: String,
    val value: String,
    val encoding: CookieEncoding = CookieEncoding.URI_ENCODING,
    val maxAge: Int = 0,
    val expires: Long? = null,
    val domain: String? = null,
    val path: String? = null,
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val extensions: Map<String, String?> = emptyMap()
) {
    fun toCookie() = Cookie(
        name,
        value,
        encoding,
        maxAge,
        GMTDate(expires),
        domain,
        path,
        secure,
        httpOnly,
        extensions
    )

    companion object {
        fun fromCookie(cookie: Cookie) = CookieSurrogate(
            cookie.name,
            cookie.value,
            cookie.encoding,
            cookie.maxAge,
            cookie.expires?.timestamp,
            cookie.domain,
            cookie.path,
            cookie.secure,
            cookie.httpOnly,
            cookie.extensions
        )
    }
}