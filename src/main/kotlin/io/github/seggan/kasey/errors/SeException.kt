package io.github.seggan.kasey.errors

import io.ktor.client.call.*
import io.ktor.client.statement.*

open class SeException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class LoginException(message: String, cause: Throwable? = null) : SeException(message, cause)

class RatelimitException(cause: Throwable? = null) : SeException("", cause)

class BadResponseException(val response: HttpResponse, message: String? = null) : SeException(
    "Bad response from ${response.request.url}: ${response.status.value} ${response.status.description}"
            + if (message != null) ": $message" else ""
)