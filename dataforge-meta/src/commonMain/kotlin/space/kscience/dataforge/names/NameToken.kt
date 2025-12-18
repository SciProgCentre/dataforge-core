package space.kscience.dataforge.names

import kotlinx.serialization.Serializable

/**
 * A single name token. Body is not allowed to be empty.
 * Following symbols are prohibited in name tokens: `{}.:\`.
 * A name token could have appendix in square brackets called *index*
 */
@Serializable(NameTokenSerializer::class)
public class NameToken(public val body: String, public val index: String? = null) {

    init {
        if (body.isEmpty()) error("Syntax error: Name token body is empty")
    }

    private val bodyEscaped by lazy {
        val escaped = buildString {
            body.forEach { ch ->
                if (ch in escapedBodyChars) {
                    append('\\')
                }
                append(ch)
            }
        }
        if (escaped == body) body else escaped
    }

    private val indexEscaped by lazy {
        index?.replace("\\", "\\\\")
            ?.replace("]", "\\]")
            ?.replace("[", "\\[")
    }


    override fun toString(): String = if (hasIndex()) {
        "${bodyEscaped}[${indexEscaped!!}]"
    } else {
        bodyEscaped
    }


    /**
     * Return unescaped version of the [NameToken]. Should be used only for output because it is not possible to correctly
     * parse it back.
     */
    public fun toStringUnescaped(): String = if (hasIndex()) {
        "${body}[$index]"
    } else {
        body
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NameToken

        if (body != other.body) return false
        if (index != other.index) return false

        return true
    }

    private val cachedHashCode = body.hashCode() * 31 + (index?.hashCode() ?: 0)

    override fun hashCode(): Int = cachedHashCode

    public companion object {

        private val escapedBodyChars = listOf('\\', '.', '[', ']')

        /**
         * Parse name token from a string
         */
        public fun parse(string: String): NameToken {
            var indexStart = -1
            var indexEnd = -1
            var escape = false
            string.forEachIndexed { index, c ->
                if(escape){
                    escape = false
                    return@forEachIndexed
                }
                when (c) {
                    '\\' -> escape = true
                    '[' -> if(indexStart < 0) indexStart = index
                    ']' -> if(indexStart >= 0) indexEnd = index
                    else -> if (indexEnd >= 0) error("Symbols not allowed after index in NameToken: $string")
                }
            }

            if (indexStart >= 0 && indexEnd < 0) error("Opening bracket without closing bracket not allowed in NameToken: $string")
            if (indexStart > indexEnd && indexEnd != -1) error("Closing bracket before opening one in NameToken: $string")

            return NameToken(
                if (indexStart >= 0) string.take(indexStart) else string,
                if (indexStart >= 0) string.substring(indexStart + 1, indexEnd) else null
            )
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
