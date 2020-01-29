package hep.dataforge.meta.scheme

import hep.dataforge.meta.*
import hep.dataforge.meta.descriptors.*
import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import hep.dataforge.names.plus

/**
 * A base for delegate-based or descriptor-based scheme. [Scheme] has an empty constructor to simplify usage from [Specification].
 */
open class Scheme() : Configurable, Described, MetaRepr {
    constructor(config: Config, defaultProvider: (Name) -> MetaItem<*>?) : this() {
        this.config = config
        this.defaultProvider = defaultProvider
    }

    //constructor(config: Config, default: Meta) : this(config, { default[it] })
    constructor(config: Config) : this(config, { null })

    final override var config: Config =
        Config()
        internal set

    lateinit var defaultProvider: (Name) -> MetaItem<*>?
        internal set

    final override var descriptor: NodeDescriptor? = null
        internal set

    override fun getDefaultItem(name: Name): MetaItem<*>? {
        return defaultProvider(name) ?: descriptor?.get(name)?.defaultItem()
    }

    /**
     * Provide a default layer which returns items from [defaultProvider] and falls back to descriptor
     * values if default value is unavailable.
     * Values from [defaultProvider] completely replace
     */
    open val defaultLayer: Meta get() = DefaultLayer(Name.EMPTY)

    override fun toMeta(): Meta = config.seal()

    private inner class DefaultLayer(val path: Name) : MetaBase() {
        override val items: Map<NameToken, MetaItem<*>> =
            (descriptor?.get(path) as? NodeDescriptor)?.items?.entries?.associate { (key, descriptor) ->
                val token = NameToken(key)
                val fullName = path + token
                val item: MetaItem<*> = when (descriptor) {
                    is ValueDescriptor -> getDefaultItem(fullName) ?: descriptor.defaultItem()
                    is NodeDescriptor -> MetaItem.NodeItem(DefaultLayer(fullName))
                }
                token to item
            } ?: emptyMap()
    }

}

/**
 * A specification for simplified generation of wrappers
 */
open class SchemeSpec<T : Scheme>(val builder: () -> T) :
    Specification<T> {
    override fun wrap(config: Config, defaultProvider: (Name) -> MetaItem<*>?): T {
        return builder().apply {
            this.config = config
            this.defaultProvider = defaultProvider
        }
    }
}

/**
 * A scheme that uses [Meta] as a default layer
 */
open class MetaScheme(
    val meta: Meta,
    descriptor: NodeDescriptor? = null,
    config: Config = Config()
) : Scheme(config, meta::get) {
    init {
        this.descriptor = descriptor
    }

    override val defaultLayer: Meta
        get() = Laminate(meta, descriptor?.defaultItem().node)
}

fun Meta.asScheme() =
    MetaScheme(this)

fun <T : Configurable> Meta.toScheme(spec: Specification<T>, block: T.() -> Unit) = spec.wrap(this).apply(block)

/**
 * Create a snapshot laminate
 */
fun Scheme.toMeta(): Laminate =
    Laminate(config, defaultLayer)