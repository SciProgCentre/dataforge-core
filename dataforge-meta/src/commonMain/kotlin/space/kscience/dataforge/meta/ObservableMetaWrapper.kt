package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.*
import kotlin.jvm.Synchronized

/**
 * A class that takes [MutableMeta] provider and adds obsevability on top of that
 */
private class ObservableMetaWrapper(
    val root: MutableMeta,
    val absoluteName: Name,
    val listeners: MutableSet<MetaListener>
) : ObservableMutableMeta {
    override val items: Map<NameToken, ObservableMutableMeta>
        get() = root.items.mapValues {
            ObservableMetaWrapper(root, absoluteName + it.key, listeners)
        }

    override fun getMeta(name: Name): ObservableMutableMeta? =
        root.getMeta(name)?.let { ObservableMetaWrapper(root, this.absoluteName + name, listeners) }

    @Synchronized
    override fun onChange(owner: Any?, callback: Meta.(name: Name) -> Unit) {
        listeners.add(
            MetaListener(Pair(owner, absoluteName)) { name ->
                if (name.startsWith(absoluteName)) {
                    (this[absoluteName] ?: Meta.EMPTY).callback(name.removeHeadOrNull(absoluteName)!!)
                }
            }
        )
    }

    override fun removeListener(owner: Any?) {
        listeners.removeAll { it.owner === Pair(owner, absoluteName) }
    }

    override fun invalidate(name: Name) {
        listeners.forEach { it.callback(this, name) }
    }

    override var value: Value?
        get() = root.value
        set(value) {
            root.value = value
            invalidate(Name.EMPTY)
        }

    override fun getOrCreate(name: Name): ObservableMutableMeta =
        ObservableMetaWrapper(root, this.absoluteName + name, listeners)

    override fun setMeta(name: Name, node: Meta?) {
        val oldMeta = get(name)
        root.setMeta(absoluteName + name, node)
        if (oldMeta != node) {
            invalidate(name)
        }
    }

    override fun toMeta(): Meta = root[absoluteName]?.toMeta() ?: Meta.EMPTY

    override fun toString(): String = Meta.toString(this)
    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)
    override fun hashCode(): Int = Meta.hashCode(this)

    @DFExperimental
    override fun attach(name: Name, node: ObservableMutableMeta) {
        set(name, node)
        node.onChange(this) { changeName ->
            setMeta(name + changeName, this[changeName])
        }
    }
}


/**
 * Cast this [MutableMeta] to [ObservableMutableMeta] or create an observable wrapper. Only changes made to the result
 * are guaranteed to be observed.
 */
public fun MutableMeta.asObservable(): ObservableMutableMeta =
    (this as? ObservableMutableMeta) ?: ObservableMetaWrapper(this, Name.EMPTY, hashSetOf())
