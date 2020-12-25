package hep.dataforge.meta

import hep.dataforge.meta.descriptors.*
import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import hep.dataforge.names.asName

/**
 * A base for delegate-based or descriptor-based scheme. [Scheme] has an empty constructor to simplify usage from [Specification].
 * Default item provider and [NodeDescriptor] are optional
 */
public open class Scheme() : MutableItemProvider, Described, MetaRepr {

    private var items: MutableItemProvider = MetaBuilder()

    private var default: ItemProvider? = null

    final override var descriptor: NodeDescriptor? = null

    internal fun inflate(
        items: MutableItemProvider,
        default: ItemProvider? = null,
        descriptor: NodeDescriptor? = null,
    ) {
        //use properties in the init block as default
        this.default = items.withDefault(default)
        //reset values, defaults are already saved
        this.items = items
        this.descriptor = descriptor
    }

    private fun getDefaultItem(name: Name): MetaItem? {
        return default?.get(name) ?: descriptor?.get(name)?.defaultItem()
    }

    /**
     * Get a property with default
     */
    override fun getItem(name: Name): MetaItem? = items[name] ?: getDefaultItem(name)

    /**
     * Check if property with given [name] could be assigned to [item]
     */
    public open fun validateItem(name: Name, item: MetaItem?): Boolean {
        val descriptor = descriptor?.get(name)
        return descriptor?.validateItem(item) ?: true
    }

    /**
     * Set a configurable property
     */
    override fun setItem(name: Name, item: MetaItem?) {
        if (validateItem(name, item)) {
            items[name] = item
        } else {
            error("Validation failed for property $name with value $item")
        }
    }

    /**
     * Provide a default layer which returns items from [default] and falls back to descriptor
     * values if default value is unavailable.
     * Values from [default] completely replace
     */
    public open val defaultLayer: Meta
        get() = object : MetaBase() {
            override val items: Map<NameToken, MetaItem> = buildMap {
                descriptor?.items?.forEach { (key, itemDescriptor) ->
                    val token = NameToken(key)
                    val name = token.asName()
                    val item = default?.get(name) ?: itemDescriptor.defaultItem()
                    if (item != null) {
                        put(token, item)
                    }
                }
            }
        }

    override fun toMeta(): Laminate = Laminate(items[Name.EMPTY].node, defaultLayer)

    public fun isEmpty(): Boolean = toMeta().isEmpty()
}


public fun <T : Scheme, S : Specification<T>> S.inflate(
    items: MutableItemProvider,
    default: ItemProvider? = null,
    descriptor: NodeDescriptor? = null,
): T = empty().apply {
    inflate(items, default, descriptor)
}


/**
 * A shortcut to edit a [Scheme] object in-place
 */
public inline operator fun <T : Scheme> T.invoke(block: T.() -> Unit): T = apply(block)

/**
 * A specification for simplified generation of wrappers
 */
public open class SchemeSpec<out T : Scheme>(
    private val builder: () -> T,
) : Specification<T>, Described {

    override fun empty(): T = builder()

    override fun read(items: ItemProvider): T = inflate(Config(), items, descriptor)

    override fun write(target: MutableItemProvider, defaultProvider: ItemProvider): T =
        inflate(target, defaultProvider, descriptor)

    //TODO Generate descriptor from Scheme class
    override val descriptor: NodeDescriptor? get() = null

    @Suppress("OVERRIDE_BY_INLINE")
    final override inline operator fun invoke(action: T.() -> Unit): T = empty().apply(action)
}