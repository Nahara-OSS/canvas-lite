package io.github.naharaoss.canvaslite.ext

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZonedDateTime

object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("io.github.naharaoss.canvaslite.ext.ZonedDateTimeAsStringSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ZonedDateTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder) = ZonedDateTime.parse(decoder.decodeString())!!
}

object ColorSerializer : KSerializer<Color> {
    @Serializable
    @SerialName("Color")
    private data class Surrogate(val r: Float, val g: Float, val b: Float, val a: Float = 1f) { val color get() = Color(r, g, b, a) }

    override val descriptor = SerialDescriptor("androidx.compose.ui.graphics.Color", Surrogate.serializer().descriptor)
    override fun serialize(encoder: Encoder, value: Color) = encoder.encodeSerializableValue(Surrogate.serializer(), Surrogate(value.red, value.green, value.blue, value.alpha))
    override fun deserialize(decoder: Decoder) = decoder.decodeSerializableValue(Surrogate.serializer()).color
}


object OffsetSerializer : KSerializer<Offset> {
    @Serializable
    @SerialName("Offset")
    private data class Surrogate(val x: Float, val y: Float) { val offset get() = Offset(x, y) }

    override val descriptor: SerialDescriptor get() = SerialDescriptor("androidx.compose.ui.geometry.Offset", Surrogate.serializer().descriptor)
    override fun serialize(encoder: Encoder, value: Offset) = encoder.encodeSerializableValue(Surrogate.serializer(), Surrogate(value.x, value.y))
    override fun deserialize(decoder: Decoder) = decoder.decodeSerializableValue(Surrogate.serializer()).offset
}