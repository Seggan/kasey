package io.github.seggan.kasey

import com.tfowl.ktor.client.plugins.JsoupPlugin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

internal inline fun constructClient(
    cookiesStorage: CookiesStorage,
    crossinline customConfig: HttpClientConfig<*>.() -> Unit = {}
) = HttpClient(CIO) {
    followRedirects = true
    install(HttpCookies) {
        storage = cookiesStorage
    }
    install(UserAgent) {
        agent = USER_AGENT
    }
    install(JsoupPlugin)
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
    customConfig()
}

private const val USER_AGENT = "Mozilla/5.0 (compatible; automated) Kasey/1.0"