package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.ThreadSafe
import space.kscience.dataforge.names.*
import kotlin.reflect.KProperty1


internal data class MetaListener(
    val owner: Any? = null,
    val callback: Meta.(name: Name) -> Unit,
)

/**
 * An item provider that could be observed and mutated
 */
public interface ObservableMeta : Meta {
    /**
     * Add change listener to this meta. Owner is declared to be able to remove listeners later. Listener without owner could not be removed
     */
    public fun onChange(owner: Any?, callback: Meta.(name: Name) -> Unit)

    /**
     * Remove all listeners belonging to given owner
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
    override fun getOrCreate(name: Name): ObservableMutableMeta

    override fun getMeta(name: Name): ObservableMutableMeta? {
        tailrec fun ObservableMutableMeta.find(name: Name): ObservableMutableMeta? = if (name.isEmpty()) {
            this
        } else {
            items[name.firstOrNull()!!]?.find(name.cutFirst())
        }

        return find(name)
    }
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

/**
 * Use the value of the property in a [callBack].
 * The callback is called once immediately after subscription to pass the initial value.
 *
 * Optional [owner] property is used for
 */
public fun <S : Scheme, T> S.useProperty(
    property: KProperty1<S, T>,
    owner: Any? = null,
    callBack: S.(T) -> Unit,
) {
    //Pass initial value.
    callBack(property.get(this))
    meta.onChange(owner) { name ->
        if (name.startsWith(property.name.asName())) {
            callBack(property.get(this@useProperty))
        }
    }
}