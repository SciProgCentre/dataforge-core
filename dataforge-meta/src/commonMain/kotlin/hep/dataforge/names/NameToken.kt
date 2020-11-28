package hep.dataforge.names

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A single name token. Body is not allowed to be empty.
 * Following symbols are prohibited in name tokens: `{}.:\`.
 * A name token could have appendix in square brackets called *index*
 */
@Serializable(NameToken.Companion::class)
public data class NameToken(val body: String, val index: String? = null) {

    init {
        if (body.isEmpty()) error("Syntax error: Name token body is empty")
    }

    private fun String.escape() =
        replace("\\", "\\\\")
            .replace(".", "\\.")
            .replace("[", "\\[")
            .replace("]", "\\]")

    override fun toString(): String = if (hasIndex()) {
        "${body.escape()}[$index]"
    } else {
        body.escape()
    }

    public companion object : KSerializer<NameToken> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("hep.dataforge.names.NameToken", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): NameToken {
            return decoder.decodeString().toName().firstOrNull()!!
        }

        override fun serialize(
            encoder: Encoder,
            value: NameToken,
        ) {
            encoder.encodeString(value.toString())
        }
    }
}

/**
 * Check if index is defined for this token
 */
public fun NameToken.hasIndex(): Boolean = index != null

/**
 * Add or replace index part of this token
 */
public fun NameToken.withIndex(newIndex: String): NameToken = NameToken(body, newIndex)
