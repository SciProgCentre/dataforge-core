package hep.dataforge.meta

import hep.dataforge.meta.descriptors.*
import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import hep.dataforge.names.asName

/**
 * A base for delegate-based or descriptor-based scheme. [Scheme] has an empty constructor to simplify usage from [Specification].
 * Default item provider and [NodeDescriptor] are optional
 */
public open class Scheme(
    config: Config = Config(),
    internal var default: ItemProvider? = null,
    descriptor: NodeDescriptor? = null,
) : Configurable, MutableItemProvider, Described, MetaRepr {

    override var config: Config = config
        internal set

    override var descriptor: NodeDescriptor? = descriptor
        internal set


    public fun getDefaultItem(name: Name): MetaItem<*>? {
        return default?.getItem(name) ?: descriptor?.get(name)?.defaultItem()
    }

    /**
     * Get a property with default
     */
    override fun getItem(name: Name): MetaItem<*>? = config[name] ?: getDefaultItem(name)

    /**
     * Check if property with given [name] could be assigned to [item]
     */
    public fun validateItem(name: Name, item: MetaItem<*>?): Boolean {
        val descriptor = descriptor?.get(name)
        return descriptor?.validateItem(item) ?: true
    }

    /**
     * Set a configurable property
     */
    override fun setItem(name: Name, item: MetaItem<*>?) {
        if (validateItem(name, item)) {
            config.setItem(name, item)
        } else {
            error("Validation failed for property $name with value $item")
        }
    }

    /**
     * Provide a default layer which returns items from [defaultProvider] and falls back to descriptor
     * values if default value is unavailable.
     * Values from [defaultProvider] completely replace
     */
    public open val defaultLayer: Meta
        get() = object : MetaBase() {
            override val items: Map<NameToken, MetaItem<*>> = buildMap {
                descriptor?.items?.forEach { (key, itemDescriptor) ->
                    val token = NameToken(key)
                    val name = token.asName()
                    val item = default?.getItem(name) ?: itemDescriptor.defaultItem()
                    if (item != null) {
                        put(token, item)
                    }
                }
            }
        }

    override fun toMeta(): Laminate = Laminate(config, defaultLayer)

    public fun isEmpty(): Boolean = config.isEmpty()
}

/**
 * A shortcut to edit a [Scheme] object in-place
 */
public inline operator fun <T : Scheme> T.invoke(block: T.() -> Unit): T = apply(block)

/**
 * A specification for simplified generation of wrappers
 */
public open class SchemeSpec<T : Scheme>(
    private val builder: (config: Config, defaultProvider: ItemProvider, descriptor: NodeDescriptor?) -> T,
) : Specification<T>, Described {

    public constructor(emptyBuilder: () -> T) : this({ config: Config, defaultProvider: ItemProvider, descriptor: NodeDescriptor? ->
        emptyBuilder().apply {
            this.config = config
            this.default = defaultProvider
            this.descriptor = descriptor
        }
    })

    /**
     * If the provided [Meta] is a [Config] use it as a scheme base, otherwise use it as default.
     */
    override fun wrap(meta: Meta, defaultProvider: ItemProvider): T = if (meta is Config) {
        builder(meta, defaultProvider, descriptor)
    } else {
        builder(Config(), meta.withDefault(defaultProvider), descriptor)
    }

    //TODO Generate descriptor from Scheme class
    override val descriptor: NodeDescriptor? get() = null
}

///**
// * A scheme that uses [Meta] as a default layer
// */
//public open class MetaScheme(
//    private val meta: Meta,
//    override val descriptor: NodeDescriptor? = null,
//    config: Config = Config(),
//) : Scheme(config, meta) {
//    override val defaultLayer: Meta get() = Laminate(meta, descriptor?.defaultItem().node)
//}

public fun Meta.asScheme(): Scheme = Scheme(this.asConfig(), null, null)

public fun <T : MutableItemProvider> Meta.toScheme(spec: Specification<T>, block: T.() -> Unit = {}): T =
    spec.wrap(this).apply(block)
