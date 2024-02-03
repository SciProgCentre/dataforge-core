package space.kscience.dataforge.names

import kotlinx.serialization.Serializable
import space.kscience.dataforge.misc.DFExperimental
import kotlin.native.concurrent.ThreadLocal


/**
 * The general interface for working with names.
 * The name is a dot separated list of strings like `token1.token2.token3`.
 * Each token could contain additional index in square brackets.
 */
@Serializable(NameSerializer::class)
public class Name(public val tokens: List<NameToken>) {
    //TODO to be transformed into inline class after they are supported with serialization

    override fun toString(): String = tokens.joinToString(separator = NAME_SEPARATOR) { it.toString() }

    override fun equals(other: Any?): Boolean = when (other) {
        is Name -> this.tokens == other.tokens
        is NameToken -> this.length == 1 && this.tokens.first() == other
        else -> false
    }

    private val cachedHashCode = if (tokens.size == 1) {
        tokens.first().hashCode()
    } else {
        tokens.hashCode()
    }

    override fun hashCode(): Int = cachedHashCode

    public companion object {
        public const val NAME_SEPARATOR: String = "."

        /**
         * Match any single token (both body and index)
         */
        @DFExperimental
        public val MATCH_ANY_TOKEN: NameToken = NameToken("*")

        /**
         * Token that allows to match the whole tail or the whole head of the name. Must match at least one token.
         */
        @DFExperimental
        public val MATCH_ALL_TOKEN: NameToken = NameToken("**")

        public val EMPTY: Name = Name(emptyList())

        /**
         * Convert a list of strings to a [Name] interpreting all arguments as token bodies without indices
         */
        public fun of(vararg strings: String): Name = Name(strings.map { NameToken(it) })

        /**
         * Convert a [String] to name parsing it and extracting name tokens and index syntax.
         * This operation is rather heavy, so it should be used with care in high performance code.
         */
        public fun parse(string: String): Name {
            if (string.isBlank()) return EMPTY
            val tokens = sequence {
                var bodyBuilder = StringBuilder()
                var queryBuilder = StringBuilder()
                var bracketCount = 0
                var escape = false
                fun queryOn() = bracketCount > 0

                for (it in string) {
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
                                val query = if (queryBuilder.isEmpty()) null else queryBuilder.toString()
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
                val query = if (queryBuilder.isEmpty()) null else queryBuilder.toString()
                yield(NameToken(bodyBuilder.toString(), query))
            }
            return Name(tokens.toList())
        }
    }
}

/**
 * Transform this [Name] to a string without escaping special characters in tokens.
 *
 * Parsing it back will produce a valid, but different name
 */
public fun Name.toStringUnescaped(): String = tokens.joinToString(separator = Name.NAME_SEPARATOR) { it.toStringUnescaped() }

public operator fun Name.get(i: Int): NameToken = tokens[i]

/**
 * The reminder of the name after last element is cut. For empty name return itself.
 */
public fun Name.cutLast(): Name = Name(tokens.subList(0, tokens.size - 1))

/**
 * The reminder of the name after first element is cut. For empty name return itself.
 */
public fun Name.cutFirst(): Name = Name(tokens.subList(1, tokens.size))

public val Name.length: Int get() = tokens.size

/**
 * Last token of the name or null if it is empty
 */
public fun Name.lastOrNull(): NameToken? = tokens.lastOrNull()

/**
 * Last token or throw exception
 */
public fun Name.last(): NameToken = tokens.last()

/**
 * First token of the name or null if it is empty
 */
public fun Name.firstOrNull(): NameToken? = tokens.firstOrNull()

/**
 * First token or throw exception
 */
public fun Name.first(): NameToken = tokens.first()

/**
 * Return a [Name] with its last token replaced via [replacement] rule.
 * If initial [Name] is empty, return empty name.
 */
public fun Name.replaceLast(replacement: (NameToken) -> NameToken): Name {
    if (isEmpty()) return Name.EMPTY
    return cutLast() + replacement(lastOrNull()!!)
}


/**
 * Convert the [String] to a [Name] by simply wrapping it in a single name token without parsing.
 * The input string could contain dots and braces, but they are just escaped, not parsed.
 */
public fun String.asName(): Name = if (isBlank()) Name.EMPTY else NameToken(this).asName()

public operator fun NameToken.plus(other: Name): Name = Name(listOf(this) + other.tokens)

public operator fun Name.plus(other: Name): Name = Name(this.tokens + other.tokens)

public operator fun Name.plus(other: String): Name = this + Name.parse(other)

public operator fun Name.plus(other: NameToken): Name = Name(tokens + other)

@Deprecated(
    "Use appendFirst instead",
    ReplaceWith("appendFirst(other)")
)
public fun Name.appendLeft(other: String): Name = NameToken(other) + this

public fun Name.appendFirst(other: String): Name = NameToken(other) + this

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
public operator fun <T> Map<Name, T>.get(name: String): T? = get(Name.parse(name))
public operator fun <T> MutableMap<Name, T>.set(name: String, value: T): Unit = set(Name.parse(name), value)

/* Name comparison operations */

public fun Name.startsWith(token: NameToken): Boolean = firstOrNull() == token

public fun Name.endsWith(token: NameToken): Boolean = lastOrNull() == token

public fun Name.startsWith(name: Name): Boolean =
    this.length >= name.length && (this == name || tokens.subList(0, name.length) == name.tokens)

public fun Name.startsWith(name: String): Boolean = startsWith(name.parseAsName())

public fun Name.endsWith(name: Name): Boolean =
    this.length >= name.length && (this == name || tokens.subList(length - name.length, length) == name.tokens)

public fun Name.endsWith(name: String): Boolean = endsWith(name.parseAsName())

/**
 * if [this] starts with given [head] name, returns the reminder of the name (could be empty). Otherwise, returns null
 */
@Deprecated(
    "Use removeFirstOrNull instead",
    ReplaceWith("removeFirstOrNull(head)")
)
public fun Name.removeHeadOrNull(head: Name): Name? = if (startsWith(head)) {
    Name(tokens.subList(head.length, length))
} else {
    null
}

/**
 * if [this] starts with given [first] name, returns the reminder of the name (could be empty). Otherwise, returns null
 */
public fun Name.removeFirstOrNull(first: Name): Name? = if (startsWith(first)) {
    Name(tokens.subList(first.length, length))
} else {
    null
}

@ThreadLocal
private val nameCache = HashMap<String, Name>()

/**
 * Parse a string as name. If [cache], use global cache for parsed name to avoid multiple parsings.
 */
public fun String.parseAsName(cache: Boolean = false): Name = if (cache) {
    nameCache.getOrPut(this) { Name.parse(this) }
} else {
    Name.parse(this)
}