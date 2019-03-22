package hep.dataforge.meta

/**
 * Marker interface for specifications
 */
interface Specification : Configurable {
    operator fun get(name: String): MetaItem<Config>? = config[name]
}

/**
 * Allows to apply custom configuration in a type safe way to simple untyped configuration.
 * By convention [Specification] companion should inherit this class
 *
 */
interface SpecificationCompanion<T : Specification> {
    /**
     * Update given configuration using given type as a builder
     */
    fun update(config: Config, action: T.() -> Unit): T {
        return wrap(config).apply(action)
    }

    fun build(action: T.() -> Unit) = update(Config(), action)

    fun empty() = build {  }

    /**
     * Wrap generic configuration producing instance of desired type
     */
    fun wrap(config: Config): T

    fun wrap(meta: Meta): T = wrap(meta.toConfig())
}

fun <T : Specification> specification(wrapper: (Config) -> T): SpecificationCompanion<T> =
    object : SpecificationCompanion<T> {
        override fun wrap(config: Config): T = wrapper(config)
    }

/**
 * Apply specified configuration to configurable
 */
fun <T : Configurable, C : Specification, S : SpecificationCompanion<C>> T.configure(spec: S, action: C.() -> Unit) =
    apply { spec.update(config, action) }

/**
 * Update configuration using given specification
 */
fun <C : Specification, S : SpecificationCompanion<C>> Specification.update(spec: S, action: C.() -> Unit) =
    apply { spec.update(config, action) }

/**
 * Create a style based on given specification
 */
fun <C : Specification, S : SpecificationCompanion<C>> S.createStyle(action: C.() -> Unit): Meta =
    Config().also { update(it, action) }


fun <M : MutableMetaNode<M>, C : Specification> Specification.spec(
    spec: SpecificationCompanion<C>,
    key: String? = null
) =
    MutableMorphDelegate(config, key) { spec.wrap(config) }