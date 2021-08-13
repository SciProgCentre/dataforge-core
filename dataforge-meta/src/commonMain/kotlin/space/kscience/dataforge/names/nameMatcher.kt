package space.kscience.dataforge.names

import space.kscience.dataforge.misc.DFExperimental


/**
 * Checks if this token matches a given [NameToken]. The match successful if:
 * * Token body matches pattern body as a regex
 * * Index body matches pattern body as a regex of both are null
 */
@DFExperimental
public fun NameToken.matches(pattern: NameToken): Boolean {
    if (pattern == Name.MATCH_ANY_TOKEN) return true
    val bodyMatches = body.matches(pattern.body.toRegex())
    val indexMatches = (index == null && pattern.index == null) || pattern.index?.let { patternIndex ->
        (index ?: "").matches(patternIndex.toRegex())
    } ?: false
    return bodyMatches && indexMatches
}


/**
 * Matches all names in pattern according to [NameToken.matches] rules.
 */
@DFExperimental
public fun Name.matches(pattern: Name): Boolean = when {
    pattern.endsWith(Name.MATCH_ALL_TOKEN) -> {
        length >= pattern.length
                && Name(tokens.subList(0, pattern.length - 1)).matches(pattern.cutLast())
    }
    pattern.startsWith(Name.MATCH_ALL_TOKEN) -> {
        length >= pattern.length
                && Name(tokens.subList(tokens.size - pattern.length + 1, tokens.size)).matches(pattern.cutFirst())
    }
    else -> {
        tokens.indices.forEach {
            val thisToken = tokens.getOrNull(it) ?: return false
            if (thisToken == Name.MATCH_ALL_TOKEN) error("Match-all token in the middle of the name is not supported yet")
            val patternToken = pattern.tokens.getOrNull(it) ?: return false
            if (!thisToken.matches(patternToken)) return false
        }
        true
    }
}

@OptIn(DFExperimental::class)
public fun Name.matches(pattern: String): Boolean = matches(Name.parse(pattern))