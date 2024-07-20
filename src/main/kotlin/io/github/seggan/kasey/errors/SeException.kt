package io.github.seggan.kasey.errors

open class SeException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class LoginException(message: String, cause: Throwable? = null) : SeException(message, cause)