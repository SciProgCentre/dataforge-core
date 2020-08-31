package hep.dataforge.names

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


/**
 * The general interface for working with names.
 * The name is a dot separated list of strings like `token1.token2.token3`.
 * Each token could contain additional index in square brackets.
 */
@Serializable
public class Name(public val tokens: List<NameToken>) {
    //TODO to be transformed into inline class after they are supported with serialization

    override fun toString(): String = tokens.joinToString(separator = NAME_SEPARATOR) { it.toString() }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Name -> this.tokens == other.tokens
            is NameToken -> this.length == 1 && this.tokens.first() == other
            else -> false
        }
    }

    override fun hashCode(): Int {
        return if (tokens.size == 1) {
            tokens.first().hashCode()
        } else {
            tokens.hashCode()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializer(Name::class)
    public companion object : KSerializer<Name> {
        public const val NAME_SEPARATOR: String = "."

        public val EMPTY: Name = Name(emptyList())

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("hep.dataforge.names.Name", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Name {
            return decoder.decodeString().toName()
        }

        override fun serialize(encoder: Encoder, value: Name) {
            encoder.encodeString(value.toString())
        }
    }
}

public operator fun Name.get(i: Int): NameToken = tokens[i]

/**
 * The reminder of the name after last element is cut. For empty name return itself.
 */
public fun Name.cutLast(): Name = Name(tokens.dropLast(1))

/**
 * The reminder of the name after first element is cut. For empty name return itself.
 */
public fun Name.cutFirst(): Name = Name(tokens.drop(1))

public val Name.length: Int get() = tokens.size

/**
 * Last token of the name or null if it is empty
 */
public fun Name.lastOrNull(): NameToken? = tokens.lastOrNull()

/**
 * First token of the name or null if it is empty
 */
public fun Name.firstOrNull(): NameToken? = tokens.firstOrNull()

/**
 * A single name token. Body is not allowed to be empty.
 * Following symbols are prohibited in name tokens: `{}.:\`.
 * A name token could have appendix in square brackets called *index*
 */
@Serializable
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

    @OptIn(ExperimentalSerializationApi::class)
    @Serializer(NameToken::class)
    public companion object : KSerializer<NameToken> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("hep.dataforge.names.NameToken", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): NameToken {
            return decoder.decodeString().toName().firstOrNull()!!
        }

        override fun serialize(encoder: Encoder, value: NameToken) {
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

/**
 * Convert a [String] to name parsing it and extracting name tokens and index syntax.
 * This operation is rather heavy so it should be used with care in high performance code.
 */
public fun String.toName(): Name {
    if (isBlank()) return Name.EMPTY
    val tokens = sequence {
        var bodyBuilder = StringBuilder()
        var queryBuilder = StringBuilder()
        var bracketCount: Int = 0
        var escape: Boolean = false
        fun queryOn() = bracketCount > 0

        for (it in this@toName) {
            when {
                escape -> {
                    if (queryOn()) {
                        queryBuilder.append(it)
                    } else {
                        bodyBuilder.append(it)
                    }
                    escape = false
                }
                it == '\\' -> {
                    escape = true
                }
                queryOn() -> {
                    when (it) {
                        '[' -> bracketCount++
                        ']' -> bracketCount--
                    }
                    if (queryOn()) queryBuilder.append(it)
                }
                else -> when (it) {
                    '.' -> {
                        val query = if(queryBuilder.isEmpty()) null else queryBuilder.toString()
                        yield(NameToken(bodyBuilder.toString(), query))
                        bodyBuilder = StringBuilder()
                        queryBuilder = StringBuilder()
                    }
                    '[' -> bracketCount++
                    ']' -> error("Syntax error: closing bracket ] not have not matching open bracket")
                    else -> {
                        if (queryBuilder.isNotEmpty()) error("Syntax error: only name end and name separator are allowed after index")
                        bodyBuilder.append(it)
                    }
                }
            }
        }
        val query = if(queryBuilder.isEmpty()) null else queryBuilder.toString()
        yield(NameToken(bodyBuilder.toString(), query))
    }
    return Name(tokens.toList())
}

/**
 * Convert the [String] to a [Name] by simply wrapping it in a single name token without parsing.
 * The input string could contain dots and braces, but they are just escaped, not parsed.
 */
public fun String.asName(): Name = if (isBlank()) Name.EMPTY else NameToken(this).asName()

public operator fun NameToken.plus(other: Name): Name = Name(listOf(this) + other.tokens)

public operator fun Name.plus(other: Name): Name = Name(this.tokens + other.tokens)

public operator fun Name.plus(other: String): Name = this + other.toName()

public operator fun Name.plus(other: NameToken): Name = Name(tokens + other)

public fun Name.appendLeft(other: String): Name = NameToken(other) + this

public fun NameToken.asName(): Name = Name(listOf(this))

public fun Name.isEmpty(): Boolean = this.length == 0

/**
 * Set or replace last token index
 */
public fun Name.withIndex(index: String): Name {
    val last = NameToken(tokens.last().body, index)
    if (length == 0) error("Can't add index to empty name")
    if (length == 1) {
        return last.asName()
    }
    val tokens = ArrayList(tokens)
    tokens.removeAt(tokens.size - 1)
    tokens.add(last)
    return Name(tokens)
}

/**
 * Fast [String]-based accessor for item map
 */
public operator fun <T> Map<NameToken, T>.get(body: String, query: String? = null): T? = get(NameToken(body, query))

public operator fun <T> Map<Name, T>.get(name: String): T? = get(name.toName())
public operator fun <T> MutableMap<Name, T>.set(name: String, value: T): Unit = set(name.toName(), value)

/* Name comparison operations */

public fun Name.startsWith(token: NameToken): Boolean = firstOrNull() == token

public fun Name.endsWith(token: NameToken): Boolean = lastOrNull() == token

public fun Name.startsWith(name: Name): Boolean =
    this.length >= name.length && tokens.subList(0, name.length) == name.tokens

public fun Name.endsWith(name: Name): Boolean =
    this.length >= name.length && tokens.subList(length - name.length, length) == name.tokens