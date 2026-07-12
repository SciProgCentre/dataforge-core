package space.kscience.dataforge.names

import kotlin.jvm.JvmInline

/**
 * A pattern to match body or index strings in [NameToken]
 */
public sealed interface StringPattern {
    public fun matches(string: String?): Boolean

    /**
     * Exact match
     */
    public data class Exact(val string: String) : StringPattern {
        override fun matches(string: String?): Boolean = string == this@Exact.string
    }

    /**
     * Regex match
     */
    public data class Regex(val regex: kotlin.text.Regex) : StringPattern {
        override fun matches(string: String?): Boolean = string?.matches(regex) ?: false
    }

    /**
     * Ensure that index is null
     */
    public data object Null : StringPattern {
        override fun matches(string: String?): Boolean = string == null
    }

    /**
     * Matches and string or missing index
     */
    public data object Any : StringPattern {
        override fun matches(string: String?): Boolean = true
    }
}

/**
 * A pattern to match single [NameToken]
 */
public sealed interface NameTokenPattern {

    /**
     * Match any single [NameToken]
     */
    public data object AnySingleToken : NameTokenPattern

    /**
     * Match any number of [NameToken]s.
     */
    public data object AnyMultipleTokens : NameTokenPattern

    public data class Token(val body: StringPattern, val index: StringPattern) : NameTokenPattern
}

/**
 * A pattern to match [Name] against token rules
 */
@JvmInline
public value class NamePattern(public val tokens: List<NameTokenPattern>) {
    public val length: Int get() = tokens.size

    public companion object {
        /**
         * True if the string first symbol is `^` and last symbol is `$`
         */
        private fun String.isRegexPattern() = length > 2 && first() == '^' && last() == '$'

        public const val ANY_TOKEN: String = "*"

        /**
         * Constructs a [NamePattern] based on the tokens of the given [Name].
         * Each token in the [Name] is transformed into a corresponding [NameTokenPattern],
         * which governs how the token is matched.
         *
         * @param name The [Name] object whose tokens are used to construct the [NamePattern].
         * @return A [NamePattern] that can be used to match names against the rules defined by the tokens.
         */
        public fun ofName(name: Name): NamePattern = NamePattern(name.tokens.map { token ->
            val body = token.body
            val index = token.index
            when {
                body == "*" && index == null -> NameTokenPattern.AnySingleToken
                body == "**" && index == null -> NameTokenPattern.AnyMultipleTokens
                else -> {
                    val bodyPattern = if (body == ANY_TOKEN) {
                        StringPattern.Any
                    } else if (body.isRegexPattern()) {
                        StringPattern.Regex(body.substring(1, body.length - 1).toRegex())
                    } else {
                        StringPattern.Exact(body)
                    }

                    val indexPattern = if (index == ANY_TOKEN) {
                        StringPattern.Any
                    } else if (index == null) {
                        StringPattern.Null
                    } else if (index.isRegexPattern()) {
                        StringPattern.Regex(index.substring(1, index.length - 1).toRegex())
                    } else {
                        StringPattern.Exact(index)
                    }

                    NameTokenPattern.Token(bodyPattern, indexPattern)
                }
            }
        })

        /**
         * Creates a [NamePattern] from a name string.
         */
        public fun ofName(name: String): NamePattern = ofName(name.parseAsName())
    }
}


/**
 * Matches all names in pattern to [NamePattern] tokens.
 */
public fun Name.matches(pattern: List<NameTokenPattern>): Boolean = when {
    pattern.lastOrNull() == NameTokenPattern.AnyMultipleTokens -> {
        length >= (pattern.size - 1) &&
                Name(tokens.subList(0, pattern.size - 1)).matches(pattern.dropLast(1))
    }

    pattern.firstOrNull() == NameTokenPattern.AnyMultipleTokens -> {
        length >= (pattern.size - 1) &&
                Name(tokens.subList(tokens.size - pattern.size + 1, tokens.size)).matches(pattern.drop(1))
    }

    else -> {
        if (pattern.any { it is NameTokenPattern.AnyMultipleTokens }) {
            error("Multiple tokens wildcard in the middle of the name is not supported")
        }
        if (length != pattern.size) false
        else {
            tokens.forEachIndexed { index, token ->
                when (val patternToken = pattern[index]) {
                    is NameTokenPattern.AnyMultipleTokens -> error("Multiple tokens wildcard in the middle of the name is not supported")
                    is NameTokenPattern.AnySingleToken -> {
                        //always true
                    }

                    is NameTokenPattern.Token -> {
                        if (!patternToken.body.matches(token.body)) return false
                        if (!patternToken.index.matches(token.index)) return false
                    }
                }
            }
            true
        }
    }
}

/**
 * Matches name against [pattern]
 */
public fun Name.matches(pattern: NamePattern): Boolean = matches(pattern.tokens)
