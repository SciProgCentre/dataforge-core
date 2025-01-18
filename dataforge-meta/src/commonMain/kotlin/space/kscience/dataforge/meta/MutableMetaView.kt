package space.kscience.dataforge.meta

import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.plus

/**
 * A [Meta] child proxy that creates required nodes on write
 */
public class MutableMetaView(
    public val origin: MutableMeta,
    public val path: Name
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
        set(path + name, node)
    }


    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)


    override fun hashCode(): Int  = Meta.hashCode(this)

    override fun toString(): String = Meta.toString(this)
}

public fun MutableMeta.view(name: Name): MutableMetaView = MutableMetaView(this, name)