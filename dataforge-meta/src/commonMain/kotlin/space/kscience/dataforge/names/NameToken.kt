package space.kscience.dataforge.names

import kotlinx.serialization.Serializable
import space.kscience.dataforge.misc.DFExperimental

/**
 * A single name token. Body is not allowed to be empty.
 * Following symbols are prohibited in name tokens: `{}.:\`.
 * A name token could have appendix in square brackets called *index*
 */
@Serializable(NameTokenSerializer::class)
public data class NameToken(val body: String, val index: String? = null) {

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

    public companion object {

        private val escapedChars = listOf('\\', '.', '[', ']')

        /**
         * Parse name token from a string
         */
        @DFExperimental
        public fun parse(string: String): NameToken {
            val body = string.substringBefore('[')
            val index = string.substringAfter('[', "")
            if (index.isNotEmpty() && index.endsWith(']')) error("NameToken with index must end with ']'")
            return NameToken(body, index.removeSuffix("]"))
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
