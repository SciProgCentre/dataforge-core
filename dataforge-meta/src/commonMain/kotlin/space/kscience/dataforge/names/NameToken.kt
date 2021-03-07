package space.kscience.dataforge.names

import kotlinx.serialization.Serializable

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
}

/**
 * Check if index is defined for this token
 */
public fun NameToken.hasIndex(): Boolean = index != null

/**
 * Add or replace index part of this token
 */
public fun NameToken.withIndex(newIndex: String): NameToken = NameToken(body, newIndex)
