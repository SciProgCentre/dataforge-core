package hep.dataforge.meta

import hep.dataforge.meta.MetaItem.NodeItem
import hep.dataforge.meta.MetaItem.ValueItem
import hep.dataforge.values.*

/**
 * A member of the meta tree. Could be represented as one of following:
 * * a [ValueItem] (leaf)
 * * a [NodeItem] (node)
 */
@Serializable(MetaItemSerializer::class)
public sealed class MetaItem<out M : Meta>() {

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    @Serializable(MetaItemSerializer::class)
    public class ValueItem(public val value: Value) : MetaItem<Nothing>() {
        override fun toString(): String = value.toString()

        override fun equals(other: Any?): Boolean {
            return this.value == (other as? ValueItem)?.value
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }
    }

    @Serializable(MetaItemSerializer::class)
    public class NodeItem<M : Meta>(public val node: M) : MetaItem<M>() {
        //Fixing serializer for node could cause class cast problems, but it should not since Meta descendants are not serializeable
        override fun toString(): String = node.toString()

        override fun equals(other: Any?): Boolean = node == (other as? NodeItem<*>)?.node

        override fun hashCode(): Int = node.hashCode()
    }

    public companion object {
        public fun of(arg: Any?): MetaItem<*> {
            return when (arg) {
                null -> ValueItem(Null)
                is MetaItem<*> -> arg
                is Meta -> NodeItem(arg)
                else -> ValueItem(Value.of(arg))
            }
        }
    }
}

public fun Value.asMetaItem(): MetaItem.ValueItem = MetaItem.ValueItem(this)
public fun <M : Meta> M.asMetaItem(): MetaItem.NodeItem<M> = MetaItem.NodeItem(this)


/**
 * Unsafe methods to access values and nodes directly from [MetaItem]
 */
public val MetaItem<*>?.value: Value?
    get() = (this as? MetaItem.ValueItem)?.value
        ?: (this?.node?.get(Meta.VALUE_KEY) as? MetaItem.ValueItem)?.value

public val MetaItem<*>?.string: String? get() = value?.string
public val MetaItem<*>?.boolean: Boolean? get() = value?.boolean
public val MetaItem<*>?.number: Number? get() = value?.numberOrNull
public val MetaItem<*>?.double: Double? get() = number?.toDouble()
public val MetaItem<*>?.float: Float? get() = number?.toFloat()
public val MetaItem<*>?.int: Int? get() = number?.toInt()
public val MetaItem<*>?.long: Long? get() = number?.toLong()
public val MetaItem<*>?.short: Short? get() = number?.toShort()

public inline fun <reified E : Enum<E>> MetaItem<*>?.enum(): E? = if (this is MetaItem.ValueItem && this.value is EnumValue<*>) {
    this.value.value as E
} else {
    string?.let { enumValueOf<E>(it) }
}

public val MetaItem<*>.stringList: List<String>? get() = value?.list?.map { it.string }

public val <M : Meta> MetaItem<M>?.node: M?
    get() = when (this) {
        null -> null
        is MetaItem.ValueItem -> null//error("Trying to interpret value meta item as node item")
        is MetaItem.NodeItem -> node
    }