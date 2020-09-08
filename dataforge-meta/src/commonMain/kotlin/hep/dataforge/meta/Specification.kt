package hep.dataforge.meta

import kotlin.jvm.JvmName

/**
 * Allows to apply custom configuration in a type safe way to simple untyped configuration.
 * By convention [Scheme] companion should inherit this class
 *
 */
public interface Specification<T : Configurable> {
    public fun empty(): T = wrap()

    /**
     * Wrap generic configuration producing instance of desired type
     */
    public fun wrap(config: Config = Config(), defaultProvider: ItemProvider = ItemProvider{ null }): T

    public operator fun invoke(action: T.() -> Unit): T = empty().apply(action)
}

/**
 * Update given configuration using given type as a builder
 */
public fun <T : Configurable> Specification<T>.update(config: Config, action: T.() -> Unit): T = wrap(config).apply(action)

/**
 * Wrap a configuration using static meta as default
 */
public fun <T : Configurable> Specification<T>.wrap(source: Meta): T {
    val default = source.seal()
    return wrap(source.asConfig(), default)
}

/**
 * Apply specified configuration to configurable
 */
public fun <T : Configurable, C : Configurable, S : Specification<C>> T.configure(spec: S, action: C.() -> Unit): T =
    apply { spec.update(config, action) }

/**
 * Update configuration using given specification
 */
public fun <C : Configurable, S : Specification<C>> Configurable.update(spec: S, action: C.() -> Unit): Configurable =
    apply { spec.update(config, action) }

/**
 * Create a style based on given specification
 */
public fun <C : Configurable, S : Specification<C>> S.createStyle(action: C.() -> Unit): Meta =
    Config().also { update(it, action) }

public fun <T : Configurable> MetaItem<*>.spec(spec: Specification<T>): T? = node?.let {
    spec.wrap(
        Config(), it
    )
}

@JvmName("configSpec")
public fun <T : Configurable> MetaItem<Config>.spec(spec: Specification<T>): T? = node?.let { spec.wrap(it) }
