package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.Described
import space.kscience.dataforge.meta.descriptors.NodeDescriptor
import space.kscience.dataforge.meta.descriptors.get
import space.kscience.dataforge.meta.descriptors.validateItem
import space.kscience.dataforge.names.Name
import kotlin.jvm.Synchronized

/**
 * A base for delegate-based or descriptor-based scheme. [Scheme] has an empty constructor to simplify usage from [Specification].
 * Default item provider and [NodeDescriptor] are optional
 */
public open class Scheme(
    private var items: ObservableItemProvider = ObservableMeta(),
    final override var descriptor: NodeDescriptor? = null
) : Described, MetaRepr, ObservableItemProvider {

    /**
     * Add a listener to this scheme changes. If the inner provider is observable, then listening will be delegated to it.
     * Otherwise, local listeners will be created.
     */
    @Synchronized
    override fun onChange(owner: Any?, action: (Name, MetaItem?, MetaItem?) -> Unit) {
        items.onChange(owner, action)
    }

    /**
     * Remove all listeners belonging to given owner
     */
    @Synchronized
    override fun removeListener(owner: Any?) {
        items.removeListener(owner)
    }

    internal fun wrap(
        items: MutableItemProvider,
        preserveDefault: Boolean = false
    ) {
        this.items = if (preserveDefault) items.withDefault(this.items).asObservable() else items.asObservable()
    }

    /**
     * Get a property with default
     */
    override fun getItem(name: Name): MetaItem? = items[name] ?: descriptor?.get(name)?.defaultValue

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
        if (oldItem != item) {
            if (validateItem(name, item)) {
                items[name] = item
            } else {
                error("Validation failed for property $name with value $item")
            }
        }
    }

    override fun toMeta(): Laminate = Laminate(items.rootNode, descriptor?.defaultMeta)
}

/**
 * The scheme is considered empty only if its root item does not exist.
 */
public fun Scheme.isEmpty(): Boolean = rootItem == null

/**
 * Relocate scheme target onto given [MutableItemProvider]. Old provider does not get updates anymore.
 * Current state of the scheme used as a default.
 */
public fun <T : Scheme> T.retarget(provider: MutableItemProvider): T = apply {
    wrap(provider, true)
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

    override fun read(items: ItemProvider): T = empty().also {
        it.wrap(ObservableMeta().withDefault(items))
    }

    override fun write(target: MutableItemProvider): T = empty().also {
        it.wrap(target)
    }

    //TODO Generate descriptor from Scheme class
    override val descriptor: NodeDescriptor? get() = null

    override fun empty(): T = builder().also {
        it.descriptor = descriptor
    }

    @Suppress("OVERRIDE_BY_INLINE")
    final override inline operator fun invoke(action: T.() -> Unit): T = empty().apply(action)

}