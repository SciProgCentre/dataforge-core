package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.asName
import kotlin.jvm.JvmName
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Marker interface for classes with specifications
 */
interface Specific : Configurable

//TODO separate mutable config from immutable meta to allow free wrapping of meta

operator fun Specific.get(name: String): MetaItem<*>? = config[name]

/**
 * Editor for specific objects
 */
inline operator fun <S : Specific> S.invoke(block: S.() -> Unit): Unit {
    run(block)
}

/**
 * Allows to apply custom configuration in a type safe way to simple untyped configuration.
 * By convention [Specific] companion should inherit this class
 *
 */
interface Specification<T : Specific> {
    /**
     * Update given configuration using given type as a builder
     */
    fun update(config: Config, action: T.() -> Unit): T {
        return wrap(config).apply(action)
    }

    operator fun invoke(action: T.() -> Unit) = update(Config(), action)

    fun empty() = wrap(Config())

    /**
     * Wrap generic configuration producing instance of desired type
     */
    fun wrap(config: Config): T

    //TODO replace by free wrapper
    fun wrap(meta: Meta): T = wrap(meta.toConfig())
}

fun <T : Specific> specification(wrapper: (Config) -> T): Specification<T> =
    object : Specification<T> {
        override fun wrap(config: Config): T = wrapper(config)
    }

/**
 * Apply specified configuration to configurable
 */
fun <T : Configurable, C : Specific, S : Specification<C>> T.configure(spec: S, action: C.() -> Unit) =
    apply { spec.update(config, action) }

/**
 * Update configuration using given specification
 */
fun <C : Specific, S : Specification<C>> Specific.update(spec: S, action: C.() -> Unit) =
    apply { spec.update(config, action) }

/**
 * Create a style based on given specification
 */
fun <C : Specific, S : Specification<C>> S.createStyle(action: C.() -> Unit): Meta =
    Config().also { update(it, action) }

class SpecDelegate<T : Specific, S : Specification<T>>(
    val target: Specific,
    val spec: S,
    val key: Name? = null
) : ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val name = key ?: property.name.asName()
        return target.config[name]?.node?.let { spec.wrap(it) } ?: (spec.empty().also {
            target.config[name] = it.config
        })
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        target.config[key ?: property.name.asName()] = value.config
    }
}

fun <T : Specific, S : Specification<T>> Specific.spec(
    spec: S,
    key: Name? = null
): SpecDelegate<T, S> = SpecDelegate(this, spec, key)

fun <T : Specific> MetaItem<*>.spec(spec: Specification<T>): T? = node?.let { spec.wrap(it) }

@JvmName("configSpec")
fun <T : Specific> MetaItem<Config>.spec(spec: Specification<T>): T? = node?.let { spec.wrap(it) }

