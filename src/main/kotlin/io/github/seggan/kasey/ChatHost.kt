package io.github.seggan.kasey

enum class ChatHost(host: String) {

    STACK_OVERFLOW("stackoverflow.com"),
    META_STACK_EXCHANGE("meta.stackexchange.com"),
    STACK_EXCHANGE("stackexchange.com");

    val chatUrl = "https://chat.$host"
}