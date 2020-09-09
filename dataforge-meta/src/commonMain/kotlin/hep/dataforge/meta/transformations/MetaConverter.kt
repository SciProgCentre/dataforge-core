package hep.dataforge.meta.transformations

import hep.dataforge.meta.*
import hep.dataforge.values.*

/**
 * A converter of generic object to and from [MetaItem]
 */
public interface MetaConverter<T : Any> {
    public fun itemToObject(item: MetaItem<*>): T
    public fun objectToMetaItem(obj: T): MetaItem<*>

    public companion object {

        public val item: MetaConverter<MetaItem<*>> = object : MetaConverter<MetaItem<*>> {
            override fun itemToObject(item: MetaItem<*>): MetaItem<*> = item
            override fun objectToMetaItem(obj: MetaItem<*>): MetaItem<*> = obj
        }

        public val meta: MetaConverter<Meta> = object : MetaConverter<Meta> {
            override fun itemToObject(item: MetaItem<*>): Meta = when (item) {
                is MetaItem.NodeItem -> item.node
                is MetaItem.ValueItem -> item.value.toMeta()
            }

            override fun objectToMetaItem(obj: Meta): MetaItem<*> = MetaItem.NodeItem(obj)
        }

        public val value: MetaConverter<Value> = object : MetaConverter<Value> {
            override fun itemToObject(item: MetaItem<*>): Value = when (item) {
                is MetaItem.NodeItem -> item.node[Meta.VALUE_KEY].value ?: error("Can't convert node to a value")
                is MetaItem.ValueItem -> item.value
            }

            override fun objectToMetaItem(obj: Value): MetaItem<*> = MetaItem.ValueItem(obj)
        }

        public val string: MetaConverter<String> = object : MetaConverter<String> {
            override fun itemToObject(item: MetaItem<*>): String = when (item) {
                is MetaItem.NodeItem -> item.node[Meta.VALUE_KEY].value ?: error("Can't convert node to a value")
                is MetaItem.ValueItem -> item.value
            }.string

            override fun objectToMetaItem(obj: String): MetaItem<*> = MetaItem.ValueItem(obj.asValue())
        }

        public val boolean: MetaConverter<Boolean> = object : MetaConverter<Boolean> {
            override fun itemToObject(item: MetaItem<*>): Boolean = when (item) {
                is MetaItem.NodeItem -> item.node[Meta.VALUE_KEY].value ?: error("Can't convert node to a value")
                is MetaItem.ValueItem -> item.value
            }.boolean

            override fun objectToMetaItem(obj: Boolean): MetaItem<*> = MetaItem.ValueItem(obj.asValue())
        }

        public val number: MetaConverter<Number> = object : MetaConverter<Number> {
            override fun itemToObject(item: MetaItem<*>): Number = when (item) {
                is MetaItem.NodeItem -> item.node[Meta.VALUE_KEY].value ?: error("Can't convert node to a value")
                is MetaItem.ValueItem -> item.value
            }.number

            override fun objectToMetaItem(obj: Number): MetaItem<*> = MetaItem.ValueItem(obj.asValue())
        }

        public val double: MetaConverter<Double> = object : MetaConverter<Double> {
            override fun itemToObject(item: MetaItem<*>): Double = when (item) {
                is MetaItem.NodeItem -> item.node[Meta.VALUE_KEY].value ?: error("Can't convert node to a value")
                is MetaItem.ValueItem -> item.value
            }.double

            override fun objectToMetaItem(obj: Double): MetaItem<*> = MetaItem.ValueItem(obj.asValue())
        }

        public val float: MetaConverter<Float> = object : MetaConverter<Float> {
            override fun itemToObject(item: MetaItem<*>): Float = when (item) {
                is MetaItem.NodeItem -> item.node[Meta.VALUE_KEY].value ?: error("Can't convert node to a value")
                is MetaItem.ValueItem -> item.value
            }.float

            override fun objectToMetaItem(obj: Float): MetaItem<*> = MetaItem.ValueItem(obj.asValue())
        }

        public val int: MetaConverter<Int> = object : MetaConverter<Int> {
            override fun itemToObject(item: MetaItem<*>): Int = when (item) {
                is MetaItem.NodeItem -> item.node[Meta.VALUE_KEY].value ?: error("Can't convert node to a value")
                is MetaItem.ValueItem -> item.value
            }.int

            override fun objectToMetaItem(obj: Int): MetaItem<*> = MetaItem.ValueItem(obj.asValue())
        }

        public val long: MetaConverter<Long> = object : MetaConverter<Long> {
            override fun itemToObject(item: MetaItem<*>): Long = when (item) {
                is MetaItem.NodeItem -> item.node[Meta.VALUE_KEY].value ?: error("Can't convert node to a value")
                is MetaItem.ValueItem -> item.value
            }.long

            override fun objectToMetaItem(obj: Long): MetaItem<*> = MetaItem.ValueItem(obj.asValue())
        }

        public inline fun <reified E : Enum<E>> enum(): MetaConverter<E> = object : MetaConverter<E> {
            @Suppress("USELESS_CAST")
            override fun itemToObject(item: MetaItem<*>): E = item.enum<E>() as? E ?: error("The Item is not a Enum")

            override fun objectToMetaItem(obj: E): MetaItem<*> = MetaItem.ValueItem(obj.asValue())
        }

        public fun <T> valueList(writer: (T) -> Value = {Value.of(it)}, reader: (Value) -> T): MetaConverter<List<T>> =
            object : MetaConverter<List<T>> {
                override fun itemToObject(item: MetaItem<*>): List<T> =
                    item.value?.list?.map(reader) ?: error("The item is not a value list")

                override fun objectToMetaItem(obj: List<T>): MetaItem<*> =
                    MetaItem.ValueItem(obj.map(writer).asValue())
            }

    }
}

public fun <T : Any> MetaConverter<T>.nullableItemToObject(item: MetaItem<*>?): T? = item?.let { itemToObject(it) }
public fun <T : Any> MetaConverter<T>.nullableObjectToMetaItem(obj: T?): MetaItem<*>? = obj?.let { objectToMetaItem(it) }

public fun <T : Any> MetaConverter<T>.metaToObject(meta: Meta): T = itemToObject(MetaItem.NodeItem(meta))
public fun <T : Any> MetaConverter<T>.valueToObject(value: Value): T = itemToObject(MetaItem.ValueItem(value))
