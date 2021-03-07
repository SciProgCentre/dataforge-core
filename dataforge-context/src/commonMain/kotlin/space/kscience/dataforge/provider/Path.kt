/*
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package space.kscience.dataforge.provider

import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.toName

/**
 * Path interface.
 *
 */
public inline class Path(public val tokens: List<PathToken>) : Iterable<PathToken> {

    override fun iterator(): Iterator<PathToken> = tokens.iterator()

    override fun toString(): String = tokens.joinToString(separator = PATH_SEGMENT_SEPARATOR)

    public companion object {
        public const val PATH_SEGMENT_SEPARATOR: String = "/"

        public fun parse(path: String): Path {
            val head = path.substringBefore(PATH_SEGMENT_SEPARATOR)
            val tail = path.substringAfter(PATH_SEGMENT_SEPARATOR)
            return PathToken.parse(head).asPath() + parse(tail)
        }
    }
}

public val Path.length: Int get() = tokens.size

public val Path.head: PathToken? get() = tokens.firstOrNull()


/**
 * Returns non-empty optional containing the chain without first segment in case of chain path.
 *
 * @return
 */
public val Path.tail: Path? get() = if (tokens.isEmpty()) null else Path(tokens.drop(1))


public operator fun Path.plus(path: Path): Path = Path(this.tokens + path.tokens)

public data class PathToken(val name: Name, val target: String? = null) {
    override fun toString(): String = if (target == null) {
        name.toString()
    } else {
        "$target$TARGET_SEPARATOR$name"
    }

    public companion object {
        public const val TARGET_SEPARATOR: String = "::"
        public fun parse(token: String): PathToken {
            val target = token.substringBefore(TARGET_SEPARATOR, "")
            val name = token.substringAfter(TARGET_SEPARATOR).toName()
            if (target.contains("[")) TODO("target separators in queries are not supported")
            return PathToken(name, target)
        }
    }
}

/**
 * Represent this path token as full path
 */
public fun PathToken.asPath(): Path = Path(listOf(this))

/**
 * Represent a name with optional [target] as a [Path]
 */
public fun Name.asPath(target: String? = null): Path = PathToken(this, target).asPath()

/**
 * Build a path from given names using default targets
 */
public fun Path(vararg names: Name): Path = Path(names.map { PathToken(it) })

/**
 * Use an array of [Name]-target pairs to construct segmented [Path]
 */
public fun Path(vararg tokens: Pair<Name, String?>): Path = Path(tokens.map { PathToken(it.first, it.second) })