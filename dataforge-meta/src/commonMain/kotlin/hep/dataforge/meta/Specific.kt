package hep.dataforge.meta

/**
 * Marker interface for classes with specifications
 */
interface Specific : Configurable

//TODO separate mutable config from immutable meta to allow free wrapping of meta

operator fun Specific.get(name: String): MetaItem<*>? = config[name]

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

    fun build(action: T.() -> Unit) = update(Config(), action)

    fun empty() = build { }

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


fun <C : Specific> Specific.spec(
    spec: Specification<C>,
    key: String? = null
): MutableMorphDelegate<Config, C> = MutableMorphDelegate(config, key) { spec.wrap(it) }