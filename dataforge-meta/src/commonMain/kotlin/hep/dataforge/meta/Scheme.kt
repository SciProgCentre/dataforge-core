package hep.dataforge.meta

import hep.dataforge.meta.descriptors.Described
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.meta.descriptors.defaultItem
import hep.dataforge.meta.descriptors.get
import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import hep.dataforge.names.asName

/**
 * A base for delegate-based or descriptor-based scheme. [Scheme] has an empty constructor to simplify usage from [Specification].
 */
public open class Scheme(
    config: Config,
    defaultProvider: ItemProvider,
) : Configurable, Described, MetaRepr {

    override var config: Config = config
        internal set

    public var defaultProvider: ItemProvider = defaultProvider
        internal set

    public constructor() : this(Config(), ItemProvider { null })

    override fun getDefaultItem(name: Name): MetaItem<*>? {
        return defaultProvider.getItem(name) ?: descriptor?.get(name)?.defaultItem()
    }

    /**
     * Provide a default layer which returns items from [defaultProvider] and falls back to descriptor
     * values if default value is unavailable.
     * Values from [defaultProvider] completely replace
     */
    public open val defaultLayer: Meta get() = DefaultLayer()

    override fun toMeta(): Laminate = Laminate(config, defaultLayer)

    private inner class DefaultLayer : MetaBase() {
        override val items: Map<NameToken, MetaItem<*>> = buildMap {
            descriptor?.items?.forEach { (key, itemDescriptor) ->
                val token = NameToken(key)
                val name = token.asName()
                val item = defaultProvider.getItem(name) ?: itemDescriptor.defaultItem()
                if (item != null) {
                    put(token, item)
                }
            }
        }
    }
}

/**
 * A shortcut to edit a [Scheme] object in-place
 */
public inline operator fun <T : Scheme> T.invoke(block: T.() -> Unit): T = apply(block)

/**
 * A specification for simplified generation of wrappers
 */
public open class SchemeSpec<T : Scheme>(
    private val builder: (config: Config, defaultProvider: ItemProvider) -> T,
) : Specification<T> {

    public constructor(emptyBuilder: () -> T) : this({ config: Config, defaultProvider: ItemProvider ->
        emptyBuilder().apply {
            this.config = config
            this.defaultProvider = defaultProvider
        }
    })

    override fun empty(): T = builder(Config(), ItemProvider.EMPTY)

    override fun wrap(config: Config, defaultProvider: ItemProvider): T = builder(config, defaultProvider)

    @Suppress("OVERRIDE_BY_INLINE")
    final override inline operator fun invoke(action: T.() -> Unit): T = empty().apply(action)
}

/**
 * A scheme that uses [Meta] as a default layer
 */
public open class MetaScheme(
    private val meta: Meta,
    override val descriptor: NodeDescriptor? = null,
    config: Config = Config(),
) : Scheme(config, meta) {
    override val defaultLayer: Meta get() = Laminate(meta, descriptor?.defaultItem().node)
}

public fun Meta.asScheme(): MetaScheme = MetaScheme(this)

public fun <T : Configurable> Meta.toScheme(spec: Specification<T>, block: T.() -> Unit): T =
    spec.wrap(this).apply(block)