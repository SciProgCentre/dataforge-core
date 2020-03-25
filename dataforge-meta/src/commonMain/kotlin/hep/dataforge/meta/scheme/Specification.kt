package hep.dataforge.meta.scheme

import hep.dataforge.meta.*
import hep.dataforge.names.Name
import kotlin.jvm.JvmName

/**
 * Allows to apply custom configuration in a type safe way to simple untyped configuration.
 * By convention [Scheme] companion should inherit this class
 *
 */
interface Specification<T : Configurable> {
    /**
     * Update given configuration using given type as a builder
     */
    fun update(config: Config, action: T.() -> Unit): T {
        return wrap(config).apply(action)
    }

    operator fun invoke(action: T.() -> Unit) = update(Config(), action)

    fun empty() = wrap()

    /**
     * Wrap generic configuration producing instance of desired type
     */
    fun wrap(config: Config = Config(), defaultProvider: (Name) -> MetaItem<*>? = { null }): T

    /**
     * Wrap a configuration using static meta as default
     */
    fun wrap(config: Config = Config(), default: Meta): T = wrap(config) { default[it] }

    /**
     * Wrap a configuration using static meta as default
     */
    fun wrap(default: Meta): T = wrap(
        Config()
    ) { default[it] }
}

/**
 * Apply specified configuration to configurable
 */
fun <T : Configurable, C : Configurable, S : Specification<C>> T.configure(spec: S, action: C.() -> Unit) =
    apply { spec.update(config, action) }

/**
 * Update configuration using given specification
 */
fun <C : Configurable, S : Specification<C>> Configurable.update(spec: S, action: C.() -> Unit) =
    apply { spec.update(config, action) }

/**
 * Create a style based on given specification
 */
fun <C : Configurable, S : Specification<C>> S.createStyle(action: C.() -> Unit): Meta =
    Config().also { update(it, action) }

fun <T : Configurable> MetaItem<*>.spec(spec: Specification<T>): T? = node?.let {
    spec.wrap(
        Config(), it
    )
}

@JvmName("configSpec")
fun <T : Configurable> MetaItem<Config>.spec(spec: Specification<T>): T? = node?.let { spec.wrap(it) }
