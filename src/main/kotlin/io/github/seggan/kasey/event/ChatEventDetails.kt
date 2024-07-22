package io.github.seggan.kasey.event

import io.github.seggan.kasey.Room
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

@Serializable
data class ChatEventDetails internal constructor(
    val id: ULong,
    @SerialName("message_id") val messageId: ULong,
    @SerialName("room_id") val roomId: ULong,
    @SerialName("room_name") val roomName: String,
    @Serializable(with = InstantAsSeconds::class)
    @SerialName("time_stamp") val timestamp: Instant,
    @SerialName("user_id") val userId: ULong,
    @SerialName("user_name") val username: String,
) {

    @Transient val room = currentRoom

    companion object {
        internal lateinit var currentRoom: Room
    }
}

private object InstantAsSeconds : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeLong(value.epochSecond)
    override fun deserialize(decoder: Decoder): Instant = Instant.ofEpochSecond(decoder.decodeLong())
}