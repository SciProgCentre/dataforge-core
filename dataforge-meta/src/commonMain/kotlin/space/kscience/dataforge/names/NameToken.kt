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
            body.forEach {
                if (it in escapedChars) {
                    append('\\')
                }
                append(it)
            }
        }
        if (escaped == body) body else escaped
    }

    override fun toString(): String = if (hasIndex()) {
        "${bodyEscaped}[$index]"
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

        private val escapedChars = listOf('\\', '.', '[', ']')

        /**
         * Parse name token from a string
         */
        public fun parse(string: String): NameToken {
            var indexStart = -1
            var indexEnd = -1
            string.forEachIndexed { index, c ->
                when (c) {
                    '[' -> when {
                        indexStart >= 0 -> error("Second opening bracket not allowed in NameToken: $string")
                        else -> indexStart = index
                    }

                    ']' -> when {
                        indexStart < 0 -> error("Closing index bracket could not be used before opening bracket in NameToken: $string")
                        indexEnd >= 0 -> error("Second closing bracket not allowed in NameToken: $string")
                        else -> indexEnd = index
                    }

                    else -> if(indexEnd>=0) error("Symbols not allowed after index in NameToken: $string")
                }
            }
            if(indexStart>=0 && indexEnd<0) error("Opening bracket without closing bracket not allowed in NameToken: $string")
            return NameToken(
                if(indexStart>=0) string.substring(0, indexStart) else string,
                if(indexStart>=0) string.substring(indexStart + 1, indexEnd) else null
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
