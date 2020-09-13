package hep.dataforge.context

import hep.dataforge.meta.DFExperimental
import hep.dataforge.names.Name
import hep.dataforge.names.plus
import hep.dataforge.provider.Provider
import hep.dataforge.provider.top
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Resolve a specific element in top level elements of the provider and attempt to cast it to the given type
 */
private fun <T : Any> Provider.provide(target: String, name: Name, type: KClass<out T>): T? {
    return content(target)[name]?.let { type.cast(it) }
}

/**
 * Resolve a top level object with given [target] and [name] in a [Context] own scope or its plugins.
 */
public fun <T : Any> Context.resolve(target: String, name: Name, type: KClass<out T>): T? {
    //Try searching for plugin an context property
    provide(target, name, type)?.let { return it }
    val pluginContent = plugins.mapNotNull { it.provide(target, name, type) }
    return if (pluginContent.isEmpty()) {
        parent?.resolve<T>(target, name, type)
    } else {
        pluginContent.single() // throws error in case of name/type conflicts
    }
}

/**
 * Resolve a top level object with given [target] and [name] in a [Context] own scope or its plugins.
 */
public inline fun <reified T : Any> Context.resolve(target: String, name: Name): T? =
    resolve(target, name, T::class)

/**
 * Gather a map of all top-level objects with given [target] from context plugins.
 * Content from plugins is prefixed by plugin name so name conflicts are impossible
 * This operation could be slow in case of large number of plugins
 */
public fun <T : Any> Context.gather(
    target: String,
    type: KClass<out T>,
    inherit: Boolean = true,
): Map<Name, T> = buildMap {
    putAll(top(target, type))
    plugins.forEach { plugin ->
        plugin.top(target, type).forEach { (name, value) ->
            if (containsKey(name)) error("Name conflict during gather. An item with name $name could not be gathered from $plugin because key is already present.")
            put(plugin.name + name, value)
        }
    }
    if (inherit) {
        parent?.gather(target, type, inherit)?.forEach {
            //put all values from parent if they are not conflicting
            if (!containsKey(it.key)) {
                put(it.key, it.value)
            }
        }
    }
}

public inline fun <reified T : Any> Context.gather(target: String, inherit: Boolean = true): Map<Name, T> =
    gather(target, T::class, inherit)

/**
 * Gather all content from context itself and its plugins in a form of sequence of name-value pairs. Ignores name conflicts.
 *
 * Adds parent context sequence as well if [inherit] is true
 */
@DFExperimental
public fun <T : Any> Context.gatherInSequence(
    target: String,
    type: KClass<out T>,
    inherit: Boolean = true,
): Sequence<Map.Entry<Name, T>> = sequence {
    yieldAll(top(target, type).entries)
    plugins.forEach { plugin ->
        yieldAll(plugin.top(target, type).mapKeys { plugin.name + it.key }.entries)
    }
    if (inherit) {
        parent?.gather(target, type, inherit)?.let {
            yieldAll(it.entries)
        }
    }
}

@DFExperimental
public inline fun <reified T : Any> Context.gatherInSequence(
    target: String,
    inherit: Boolean = true,
): Sequence<Map.Entry<Name, T>> = gatherInSequence(target, T::class, inherit)

public val <T> Sequence<Map.Entry<Name, T>>.values: Sequence<T> get() = map { it.value }