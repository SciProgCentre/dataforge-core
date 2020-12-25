package hep.dataforge.meta

import hep.dataforge.values.*
import kotlinx.serialization.Serializable

/**
 * A member of the meta tree. Could be represented as one of following:
 * * a [ValueItem] (leaf)
 * * a [NodeItem] (node)
 */
@Serializable(MetaItemSerializer::class)
public sealed class TypedMetaItem<out M : Meta>() {

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    public companion object {
        public fun of(arg: Any?): MetaItem {
            return when (arg) {
                null -> Null.asMetaItem()
                is MetaItem -> arg
                is Meta -> arg.asMetaItem()
                is ItemProvider -> arg.rootItem ?: Null.asMetaItem()
                else -> Value.of(arg).asMetaItem()
            }
        }
    }
}

public typealias MetaItem = TypedMetaItem<*>

@Serializable(MetaItemSerializer::class)
public class ValueItem(public val value: Value) : TypedMetaItem<Nothing>() {
    override fun toString(): String = value.toString()

    override fun equals(other: Any?): Boolean {
        return this.value == (other as? ValueItem)?.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

@Serializable(MetaItemSerializer::class)
public class NodeItem<M : Meta>(public val node: M) : TypedMetaItem<M>() {
    //Fixing serializer for node could cause class cast problems, but it should not since Meta descendants are not serializable
    override fun toString(): String = node.toString()

    override fun equals(other: Any?): Boolean = node == (other as? NodeItem<*>)?.node

    override fun hashCode(): Int = node.hashCode()
}

public fun Value.asMetaItem(): ValueItem = ValueItem(this)
public fun <M : Meta> M.asMetaItem(): NodeItem<M> = NodeItem(this)


/**
 * Unsafe methods to access values and nodes directly from [TypedMetaItem]
 */
public val MetaItem?.value: Value?
    get() = (this as? ValueItem)?.value
        ?: (this?.node?.get(Meta.VALUE_KEY) as? ValueItem)?.value

public val MetaItem?.string: String? get() = value?.string
public val MetaItem?.boolean: Boolean? get() = value?.boolean
public val MetaItem?.number: Number? get() = value?.numberOrNull
public val MetaItem?.double: Double? get() = number?.toDouble()
public val MetaItem?.float: Float? get() = number?.toFloat()
public val MetaItem?.int: Int? get() = number?.toInt()
public val MetaItem?.long: Long? get() = number?.toLong()
public val MetaItem?.short: Short? get() = number?.toShort()

public inline fun <reified E : Enum<E>> MetaItem?.enum(): E? = if (this is ValueItem && this.value is EnumValue<*>) {
    this.value.value as E
} else {
    string?.let { enumValueOf<E>(it) }
}

public val MetaItem.stringList: List<String>? get() = value?.list?.map { it.string }

public val <M : Meta> TypedMetaItem<M>?.node: M?
    get() = when (this) {
        null -> null
        is ValueItem -> null//error("Trying to interpret value meta item as node item")
        is NodeItem -> node
    }