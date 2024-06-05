package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.ThreadSafe
import space.kscience.dataforge.names.Name


internal data class MetaListener(
    val owner: Any? = null,
    val callback: Meta.(name: Name) -> Unit,
)

/**
 * An item provider that could be observed and mutated
 */
public interface ObservableMeta : Meta {
    /**
     * Add change listener to this meta. The Owner is declared to be able to remove listeners later.
     * Listeners without an owner could be only removed all together.
     *
     * `this` object in the listener represents the current state of this meta. The name points to a changed node
     */
    public fun onChange(owner: Any?, callback: Meta.(name: Name) -> Unit)

    /**
     * Remove all listeners belonging to the given [owner]. Passing null removes all listeners.
     */
    public fun removeListener(owner: Any?)

    /**
     * Force-send invalidation signal for given name to all listeners
     */
    public fun invalidate(name: Name)
}

/**
 * A [Meta] which is both observable and mutable
 */
public interface ObservableMutableMeta : ObservableMeta, MutableMeta, MutableTypedMeta<ObservableMutableMeta> {
    override val self: ObservableMutableMeta get() = this
}

internal abstract class AbstractObservableMeta : ObservableMeta {
    private val listeners: MutableList<MetaListener> = mutableListOf()

    override fun invalidate(name: Name) {
        listeners.forEach { it.callback(this, name) }
    }

    @ThreadSafe
    override fun onChange(owner: Any?, callback: Meta.(name: Name) -> Unit) {
        listeners.add(MetaListener(owner, callback))
    }

    @ThreadSafe
    override fun removeListener(owner: Any?) {
        listeners.removeAll { it.owner === owner }
    }

    override fun toString(): String = Meta.toString(this)
    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)
    override fun hashCode(): Int = Meta.hashCode(this)
}