package hep.dataforge.names

import kotlinx.serialization.*


/**
 * The general interface for working with names.
 * The name is a dot separated list of strings like `token1.token2.token3`.
 * Each token could contain additional index in square brackets.
 */
@Serializable
class Name(val tokens: List<NameToken>) {

    val length get() = tokens.size

    /**
     * First token of the name or null if it is empty
     */
    fun first(): NameToken? = tokens.firstOrNull()

    /**
     * Last token of the name or null if it is empty
     */
    fun last(): NameToken? = tokens.lastOrNull()

    /**
     * The reminder of the name after first element is cut. For empty name return itself.
     */
    fun cutFirst(): Name = Name(tokens.drop(1))

    /**
     * The reminder of the name after last element is cut. For empty name return itself.
     */
    fun cutLast(): Name = Name(tokens.dropLast(1))

    operator fun get(i: Int): NameToken = tokens[i]

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

    @Serializer(Name::class)
    companion object : KSerializer<Name> {
        const val NAME_SEPARATOR = "."

        val EMPTY = Name(emptyList())

        override val descriptor: SerialDescriptor =
            PrimitiveDescriptor("hep.dataforge.names.Name", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Name {
            return decoder.decodeString().toName()
        }

        override fun serialize(encoder: Encoder, value: Name) {
            encoder.encodeString(value.toString())
        }
    }
}

/**
 * A single name token. Body is not allowed to be empty.
 * Following symbols are prohibited in name tokens: `{}.:\`.
 * A name token could have appendix in square brackets called *index*
 */
@Serializable
data class NameToken(val body: String, val index: String = "") {

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

    fun hasIndex() = index.isNotEmpty()

    @Serializer(NameToken::class)
    companion object : KSerializer<NameToken> {
        override val descriptor: SerialDescriptor =
            PrimitiveDescriptor("hep.dataforge.names.NameToken", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): NameToken {
            return decoder.decodeString().toName().first()!!
        }

        override fun serialize(encoder: Encoder, value: NameToken) {
            encoder.encodeString(value.toString())
        }
    }
}

/**
 * Convert a [String] to name parsing it and extracting name tokens and index syntax.
 * This operation is rather heavy so it should be used with care in high performance code.
 */
fun String.toName(): Name {
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
                        yield(NameToken(bodyBuilder.toString(), queryBuilder.toString()))
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
        yield(NameToken(bodyBuilder.toString(), queryBuilder.toString()))
    }
    return Name(tokens.toList())
}

/**
 * Convert the [String] to a [Name] by simply wrapping it in a single name token without parsing.
 * The input string could contain dots and braces, but they are just escaped, not parsed.
 */
fun String.asName(): Name = if (isBlank()) Name.EMPTY else NameToken(this).asName()

operator fun NameToken.plus(other: Name): Name = Name(listOf(this) + other.tokens)

operator fun Name.plus(other: Name): Name = Name(this.tokens + other.tokens)

operator fun Name.plus(other: String): Name = this + other.toName()

operator fun Name.plus(other: NameToken): Name = Name(tokens + other)

fun Name.appendLeft(other: String): Name = NameToken(other) + this

fun NameToken.asName() = Name(listOf(this))

fun Name.isEmpty(): Boolean = this.length == 0

/**
 * Set or replace last token index
 */
fun Name.withIndex(index: String): Name {
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
operator fun <T> Map<NameToken, T>.get(body: String, query: String = ""): T? = get(NameToken(body, query))

operator fun <T> Map<Name, T>.get(name: String) = get(name.toName())
operator fun <T> MutableMap<Name, T>.set(name: String, value: T) = set(name.toName(), value)

/* Name comparison operations */

fun Name.startsWith(token: NameToken): Boolean = first() == token

fun Name.endsWith(token: NameToken): Boolean = last() == token

fun Name.startsWith(name: Name): Boolean =
    this.length >= name.length && tokens.subList(0, name.length) == name.tokens

fun Name.endsWith(name: Name): Boolean =
    this.length >= name.length && tokens.subList(length - name.length, length) == name.tokens