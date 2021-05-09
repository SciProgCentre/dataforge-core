package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.Described
import space.kscience.dataforge.meta.descriptors.NodeDescriptor
import space.kscience.dataforge.meta.descriptors.get
import space.kscience.dataforge.meta.descriptors.validateItem
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName
import kotlin.jvm.Synchronized

/**
 * A base for delegate-based or descriptor-based scheme. [Scheme] has an empty constructor to simplify usage from [Specification].
 * Default item provider and [NodeDescriptor] are optional
 */
public open class Scheme() : Described, MetaRepr, ItemPropertyProvider {

    private var items: MutableItemProvider = Config()

    private val listeners = HashSet<ItemListener>()

    private var default: ItemProvider? = null

    final override var descriptor: NodeDescriptor? = null


    /**
     * Add a listener to this scheme changes. If the inner provider is observable, then listening will be delegated to it.
     * Otherwise, local listeners will be created.
     */
    @Synchronized
    override fun onChange(owner: Any?, action: (Name, MetaItem?, MetaItem?) -> Unit) {
        (items as? ObservableItemProvider)?.onChange(owner, action)
            ?: run { listeners.add(ItemListener(owner, action)) }
    }

    /**
     * Remove all listeners belonging to given owner
     */
    @Synchronized
    override fun removeListener(owner: Any?) {
        (items as? ObservableItemProvider)?.removeListener(owner)
            ?: listeners.removeAll { it.owner === owner }
    }

    internal fun wrap(
        items: MutableItemProvider,
        default: ItemProvider? = null,
        descriptor: NodeDescriptor? = null,
    ) {
        //use properties in the init block as default
        this.default = this.items.withDefault(default)
        //reset values, defaults are already saved
        this.items = items
        this.descriptor = descriptor
    }

    private fun getDefaultItem(name: Name): MetaItem? {
        return default?.get(name) ?: descriptor?.get(name)?.defaultValue
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
        val oldItem = items[name]
        if (validateItem(name, item)) {
            items[name] = item
            listeners.forEach { it.action(name, oldItem, item) }
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
                    val item = default?.get(name) ?: itemDescriptor.defaultValue
                    if (item != null) {
                        put(token, item)
                    }
                }
            }
        }

    override fun toMeta(): Laminate = Laminate(items[Name.EMPTY].node, defaultLayer)
}

/**
 * The scheme is considered empty only if its root item does not exist.
 */
public fun Scheme.isEmpty(): Boolean = rootItem == null

/**
 * Create a new empty [Scheme] object (including defaults) and inflate it around existing [MutableItemProvider].
 * Items already present in the scheme are used as defaults.
 */
public fun <T : Scheme, S : Specification<T>> S.wrap(
    items: MutableItemProvider,
    default: ItemProvider? = null,
    descriptor: NodeDescriptor? = null,
): T = empty().apply {
    wrap(items, default, descriptor)
}

/**
 * Relocate scheme target onto given [MutableItemProvider]. Old provider does not get updates anymore.
 * Current state of the scheme used as a default.
 */
public fun <T : Scheme> T.retarget(provider: MutableItemProvider): T = apply { wrap(provider) }

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

    override fun read(items: ItemProvider): T = wrap(Config(), items, descriptor)

    override fun write(target: MutableItemProvider, defaultProvider: ItemProvider): T =
        wrap(target, defaultProvider, descriptor)

    //TODO Generate descriptor from Scheme class
    override val descriptor: NodeDescriptor? get() = null

    @Suppress("OVERRIDE_BY_INLINE")
    final override inline operator fun invoke(action: T.() -> Unit): T = empty().apply(action)
}