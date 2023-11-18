package space.kscience.dataforge.provider

import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginBuilder
import space.kscience.dataforge.context.gather
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.misc.DfId
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.names.Name
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation


@DFExperimental
public val KClass<*>.dfId: String
    get() = findAnnotation<DfId>()?.id ?: simpleName ?: ""

/**
 * Provide an object with given name inferring target from its type using [DfId] annotation
 */
@DFExperimental
public inline fun <reified T : Any> Provider.provideByType(name: String): T? {
    val target = T::class.dfId
    return provide(target, name)
}

@DFExperimental
public inline fun <reified T : Any> Provider.top(): Map<Name, T> {
    val target = T::class.dfId
    return top(target)
}

/**
 * All objects provided by plugins with given target and type
 */
@DFExperimental
public inline fun <reified T : Any> Context.gather(inherit: Boolean = true): Map<Name, T> =
    gather<T>(T::class.dfId, inherit)


@DFExperimental
public inline fun <reified T : Any> PluginBuilder.provides(items: Map<Name, T>) {
    provides(T::class.dfId, items)
}

@DFExperimental
public inline fun <reified T : Any> PluginBuilder.provides(vararg items: Named) {
    provides(T::class.dfId, *items)
}
