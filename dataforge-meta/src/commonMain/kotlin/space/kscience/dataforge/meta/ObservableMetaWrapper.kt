package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.misc.ThreadSafe
import space.kscience.dataforge.names.*

/**
 * A class that takes [MutableMeta] provider and adds obsevability on top of that
 *
 * TODO rewrite to properly work with detached nodes
 */
private class ObservableMetaWrapper(
    val root: MutableMeta,
    val nodeName: Name,
    val listeners: MutableSet<MetaListener>,
) : ObservableMutableMeta {
    override val items: Map<NameToken, ObservableMutableMeta>
        get() = root[nodeName]?.items?.keys?.associateWith {
            ObservableMetaWrapper(root, nodeName + it, listeners)
        } ?: emptyMap()

    override fun get(name: Name): ObservableMutableMeta? = if (root[nodeName + name] == null) {
        null
    } else {
        ObservableMetaWrapper(root, nodeName + name, listeners)
    }

    @ThreadSafe
    override fun onChange(owner: Any?, callback: Meta.(name: Name) -> Unit) {
        listeners.add(
            MetaListener(Pair(owner, nodeName)) { fullName ->
                if (fullName.startsWith(nodeName)) {
                    root[nodeName]?.callback(fullName.removeFirstOrNull(nodeName)!!)
                }
            }
        )
    }

    override fun removeListener(owner: Any?) {
        listeners.removeAll { it.owner === Pair(owner, nodeName) }
    }

    override fun invalidate(name: Name) {
        listeners.forEach { it.callback(this, nodeName + name) }
    }

    override var value: Value?
        get() = root[nodeName]?.value
        set(value) {
            root.getOrCreate(nodeName).value = value
            invalidate(Name.EMPTY)
        }

    override fun getOrCreate(name: Name): ObservableMutableMeta =
        ObservableMetaWrapper(root, nodeName + name, listeners)

    fun removeNode(name: Name): Meta? {
        val oldMeta = get(name)
        //don't forget to remove listener
        oldMeta?.removeListener(this)

        return oldMeta
    }

    override fun set(name: Name, node: Meta?) {
        val oldMeta = removeNode(name)
        root[nodeName + name] = node
        if (oldMeta != node) {
            invalidate(name)
        }
    }

    override fun toMeta(): Meta = root[nodeName]?.toMeta() ?: Meta.EMPTY

    override fun toString(): String = Meta.toString(this)
    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)
    override fun hashCode(): Int = Meta.hashCode(this)

    @DFExperimental
    override fun attach(name: Name, node: ObservableMutableMeta) {
        set(name, node)
        node.onChange(this) { changeName ->
            set(name + changeName, this[changeName])
        }
    }
}


/**
 * Cast this [MutableMeta] to [ObservableMutableMeta] or create an observable wrapper. Only changes made to the result
 * are guaranteed to be observed.
 */
public fun MutableMeta.asObservable(): ObservableMutableMeta =
    (this as? ObservableMutableMeta) ?: ObservableMetaWrapper(this, Name.EMPTY, hashSetOf())
