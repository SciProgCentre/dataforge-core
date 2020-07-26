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
 * A marker utility interface for providers.
 *
 * @author Alexander Nozik
 */
interface Provider {

    /**
     * Default target for this provider
     *
     * @return
     */
    val defaultTarget: String get() = ""

    /**
     * Default target for next chain segment
     *
     * @return
     */
    val defaultChainTarget: String get() = ""


    /**
     * A map of direct children for specific target
     */
    fun provideTop(target: String): Map<Name, Any> =
        throw IllegalArgumentException("Target $target is not supported for $this")
}

fun Provider.provide(path: Path, targetOverride: String? = null): Any? {
    if (path.length == 0) throw IllegalArgumentException("Can't provide by empty path")
    val first = path.first()
    val target = targetOverride ?: first.target ?: defaultTarget
    val res = provideTop(target)[first.name] ?: return null
    return when (path.length) {
        1 -> res
        else -> {
            when (res) {
                is Provider -> res.provide(path.tail!!, targetOverride = defaultChainTarget)
                else -> throw IllegalStateException("Chain path not supported: child is not a provider")
            }
        }
    }
}

/**
 * Type checked provide
 */
inline fun <reified T : Any> Provider.provide(path: String): T? {
    return provide(Path.parse(path)) as? T
}

inline fun <reified T : Any> Provider.provide(target: String, name: Name): T? {
    return provide(PathToken(name, target).toPath()) as? T
}

inline fun <reified T : Any> Provider.provide(target: String, name: String): T? =
    provide(target, name.toName())

/**
 *  Typed top level content
 */
inline fun <reified T : Any> Provider.top(target: String): Map<Name, T> {
    return provideTop(target).mapValues {
        it.value as? T ?: error("The type of element $it is ${it::class} but ${T::class} is expected")
    }
}


