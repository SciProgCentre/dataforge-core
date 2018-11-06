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
package hep.dataforge.provider

import hep.dataforge.names.Name
import hep.dataforge.names.toName

/**
 *
 *
 * Path interface.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
inline class Path(val tokens: List<PathToken>) : Iterable<PathToken> {

    val head: PathToken? get() = tokens.firstOrNull()

    val length: Int get() = tokens.size

    /**
     * Returns non-empty optional containing the chain without first segment in case of chain path.
     *
     * @return
     */
    val tail: Path? get() = if (tokens.isEmpty()) null else Path(tokens.drop(1))

    override fun iterator(): Iterator<PathToken> = tokens.iterator()

    companion object {
        const val PATH_SEGMENT_SEPARATOR = "/"

        fun parse(path: String): Path {
            val head = path.substringBefore(PATH_SEGMENT_SEPARATOR)
            val tail = path.substringAfter(PATH_SEGMENT_SEPARATOR)
            return PathToken.parse(head).toPath() + parse(tail)
        }
    }
}

operator fun Path.plus(path: Path) = Path(this.tokens + path.tokens)

data class PathToken(val name: Name, val target: String? = null) {
    override fun toString(): String = if (target == null) {
        name.toString()
    } else {
        "$target$TARGET_SEPARATOR$name"
    }

    companion object {
        const val TARGET_SEPARATOR = "::"
        fun parse(token: String): PathToken {
            val target = token.substringBefore(TARGET_SEPARATOR, "")
            val name = token.substringAfter(TARGET_SEPARATOR).toName()
            if (target.contains("[")) TODO("target separators in queries are not supported")
            return PathToken(name, target)
        }
    }
}

fun PathToken.toPath() = Path(listOf(this))
