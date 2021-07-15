package space.kscience.dataforge.meta

import kotlinx.serialization.json.Json
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.toName

/**
 * A meta node that ensures that all of its descendants has at least the same type
 */
public interface TypedMeta<out M : TypedMeta<M>> : Meta {
    override val items: Map<NameToken, TypedMetaItem<M>>

    @Suppress("UNCHECKED_CAST")
    override fun getItem(name: Name): TypedMetaItem<M>? = super.getItem(name)?.let { it as TypedMetaItem<M> }
    //Typed meta guarantees that all children have M type
}


/**
 * The same as [Meta.get], but with specific node type
 */
public operator fun <M : TypedMeta<M>> M.get(name: Name): TypedMetaItem<M>? = getItem(name)


public operator fun <M : TypedMeta<M>> M.get(key: String): TypedMetaItem<M>? = this[key.toName()]
public operator fun <M : TypedMeta<M>> M.get(key: NameToken): TypedMetaItem<M>? = items[key]

/**
 * Equals, hashcode and to string for any meta
 */
public abstract class MetaBase : Meta {

    override fun equals(other: Any?): Boolean = if (other is Meta) {
        Meta.equals(this, other)
    } else {
        false
    }

    override fun hashCode(): Int = items.hashCode()

    override fun toString(): String = json.encodeToString(MetaSerializer, this)

    public companion object{
        private val json = Json {
            prettyPrint = true
            useArrayPolymorphism = true
        }

    }
}

/**
 * Equals and hash code implementation for meta node
 */
public abstract class AbstractTypedMeta<M : TypedMeta<M>> : TypedMeta<M>, MetaBase()
