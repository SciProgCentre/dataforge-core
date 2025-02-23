package space.kscience.dataforge.meta

import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.parseAsName
import space.kscience.dataforge.names.plus

/**
 * A [Meta] child proxy that creates required nodes on value write
 */
private class MutableMetaView(
    val origin: MutableMetaProvider,
    val path: Name
) : MutableMeta {

    override val items: Map<NameToken, MutableMeta>
        get() = origin[path]?.items ?: emptyMap()

    override var value: Value?
        get() = origin[path]?.value
        set(value) {
            origin[path] = value
        }

    override fun getOrCreate(name: Name): MutableMeta = MutableMetaView(origin, path + name)

    override fun set(name: Name, node: Meta?) {
        if (origin[path + name] == null && node?.isEmpty() == true) return
        origin[path + name] = node
    }

    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)

    override fun hashCode(): Int = Meta.hashCode(this)

    override fun toString(): String = Meta.toString(this)
}

/**
 * Create a view of this [MutableMeta] node that creates child items only when their values are written.
 *
 * The difference between this method and regular [getOrCreate] is that [getOrCreate] always creates and attaches node
 * even if it is empty.
 */
public fun MutableMetaProvider.view(name: Name): MutableMeta = MutableMetaView(this, name)

public fun MutableMetaProvider.view(name: String): MutableMeta = view(name.parseAsName())

/**
 * Create a view of root node, thus effectively representing [MutableMetaProvider] as [MutableMeta]
 */
public fun MutableMetaProvider.asMutableMeta(): MutableMeta = view(Name.EMPTY)