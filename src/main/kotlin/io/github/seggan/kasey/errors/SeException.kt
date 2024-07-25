package io.github.seggan.kasey.errors

import io.ktor.client.statement.*

/**
 * The base exception for all exceptions thrown by Kasey in the course of interfacing with
 * Stack Exchange chat.
 */
open class SeException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * An exception thrown when the client is unable to log in.
 */
open class LoginException(message: String, cause: Throwable? = null) : SeException(message, cause)

class InvalidCredentialsException : LoginException("Invalid credentials")

/**
 * An exception thrown when the server rate limits the client.
 */
class RatelimitException(cause: Throwable? = null) : SeException("", cause)

/**
 * An exception thrown when the client receives a bad response from the server.
 */
class BadResponseException(val response: HttpResponse, message: String? = null) : SeException(
    "Bad response from ${response.request.url}: ${response.status.value} ${response.status.description}"
            + if (message != null) ": $message" else ""
)