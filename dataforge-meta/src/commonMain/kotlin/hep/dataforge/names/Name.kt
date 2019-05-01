package hep.dataforge.names


/**
 * The general interface for working with names.
 * The name is a dot separated list of strings like `token1.token2.token3`.
 * Each token could contain additional index in square brackets.
 */
inline class Name constructor(val tokens: List<NameToken>) {

    val length
        get() = tokens.size

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

    companion object {
        const val NAME_SEPARATOR = "."
    }
}

/**
 * A single name token. Body is not allowed to be empty.
 * Following symbols are prohibited in name tokens: `{}.:\`.
 * A name token could have appendix in square brackets called *index*
 */
data class NameToken(val body: String, val index: String = "") {

    init {
        if (body.isEmpty()) error("Syntax error: Name token body is empty")
    }

    override fun toString(): String = if (hasIndex()) {
        "$body[$index]"
    } else {
        body
    }

    fun hasIndex() = index.isNotEmpty()
}

fun String.toName(): Name {
    if (isBlank()) return EmptyName
    val tokens = sequence {
        var bodyBuilder = StringBuilder()
        var queryBuilder = StringBuilder()
        var bracketCount: Int = 0
        fun queryOn() = bracketCount > 0

        asSequence().forEach {
            if (queryOn()) {
                when (it) {
                    '[' -> bracketCount++
                    ']' -> bracketCount--
                }
                if (queryOn()) queryBuilder.append(it)
            } else {
                when (it) {
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

operator fun NameToken.plus(other: Name): Name = Name(listOf(this) + other.tokens)

operator fun Name.plus(other: Name): Name = Name(this.tokens + other.tokens)

operator fun Name.plus(other: String): Name = this + other.toName()

fun NameToken.asName() = Name(listOf(this))

val EmptyName = Name(emptyList())

fun Name.isEmpty(): Boolean = this.length == 0

/**
 * Set or replace last token index
 */
fun Name.withIndex(index: String): Name {
    val tokens = ArrayList(tokens)
    val last = NameToken(tokens.last().body, index)
    tokens.removeAt(tokens.size - 1)
    tokens.add(last)
    return Name(tokens)
}

operator fun <T> Map<Name, T>.get(name: String) = get(name.toName())
operator fun <T> MutableMap<Name, T>.set(name: String, value: T) = set(name.toName(), value)