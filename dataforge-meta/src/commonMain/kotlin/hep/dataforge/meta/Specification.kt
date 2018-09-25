package hep.dataforge.meta

/**
 * Marker interface for specifications
 */
interface Specification: Configurable{
    operator fun get(name: String): MetaItem<Config>? = config.get(name)
}

/**
 * Specification allows to apply custom configuration in a type safe way to simple untyped configuration
 */
interface SpecificationBuilder<T : Specification> {
    /**
     * Update given configuration using given type as a builder
     */
    fun update(config: Config, action: T.() -> Unit) {
        wrap(config).apply(action)
    }

    /**
     * Wrap generic configuration producing instance of desired type
     */
    fun wrap(config: Config): T

    fun wrap(meta: Meta): T = wrap(meta.toConfig())
}

fun <T : Specification> specification(wrapper: (Config) -> T): SpecificationBuilder<T> = object : SpecificationBuilder<T> {
    override fun wrap(config: Config): T  = wrapper(config)
}

/**
 * Apply specified configuration to configurable
 */
fun <T : Configurable, C : Specification, S : SpecificationBuilder<C>> T.configure(spec: S, action: C.() -> Unit) = apply { spec.update(config, action) }

/**
 * Update configuration using given specification
 */
fun <C : Specification, S : SpecificationBuilder<C>> Specification.update(spec: S, action: C.() -> Unit) = apply { spec.update(config, action) }

/**
 * Create a style based on given specification
 */
fun <C : Specification, S : SpecificationBuilder<C>> S.createStyle(action: C.() -> Unit): Meta = Config().also { update(it, action) }


fun <C : Specification> Specification.spec(spec: SpecificationBuilder<C>, key: String? = null) = ChildConfigDelegate<C>(key) { spec.wrap(config) }