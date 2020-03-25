package hep.dataforge.meta.transformations

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaItem
import hep.dataforge.meta.get
import hep.dataforge.meta.value
import hep.dataforge.values.*

/**
 * A converter of generic object to and from [MetaItem]
 */
interface MetaCaster<T : Any> {
    fun itemToObject(item: MetaItem<*>): T
    fun objectToMetaItem(obj: T): MetaItem<*>

    companion object {

        val meta = object : MetaCaster<Meta> {
            override fun itemToObject(item: MetaItem<*>): Meta = when (item) {
                is MetaItem.NodeItem -> item.node
                is MetaItem.ValueItem -> item.value.toMeta()
            }

            override fun objectToMetaItem(obj: Meta): MetaItem<*> = MetaItem.NodeItem(obj)
        }

        val value = object : MetaCaster<Value> {
            override fun itemToObject(item: MetaItem<*>): Value = when (item) {
                is MetaItem.NodeItem -> item.node[Meta.VALUE_KEY].value ?: error("Can't convert node to a value")
                is MetaItem.ValueItem -> item.value
            }

            override fun objectToMetaItem(obj: Value): MetaItem<*> = MetaItem.ValueItem(obj)
        }

        val string = object : MetaCaster<String> {
            override fun itemToObject(item: MetaItem<*>): String = when (item) {
                is MetaItem.NodeItem -> item.node[Meta.VALUE_KEY].value ?: error("Can't convert node to a value")
                is MetaItem.ValueItem -> item.value
            }.string

            override fun objectToMetaItem(obj: String): MetaItem<*> = MetaItem.ValueItem(obj.asValue())
        }

        val boolean = object : MetaCaster<Boolean> {
            override fun itemToObject(item: MetaItem<*>): Boolean = when (item) {
                is MetaItem.NodeItem -> item.node[Meta.VALUE_KEY].value ?: error("Can't convert node to a value")
                is MetaItem.ValueItem -> item.value
            }.boolean

            override fun objectToMetaItem(obj: Boolean): MetaItem<*> = MetaItem.ValueItem(obj.asValue())
        }

        val double = object : MetaCaster<Double> {
            override fun itemToObject(item: MetaItem<*>): Double = when (item) {
                is MetaItem.NodeItem -> item.node[Meta.VALUE_KEY].value ?: error("Can't convert node to a value")
                is MetaItem.ValueItem -> item.value
            }.double

            override fun objectToMetaItem(obj: Double): MetaItem<*> = MetaItem.ValueItem(obj.asValue())
        }

        val int = object : MetaCaster<Int> {
            override fun itemToObject(item: MetaItem<*>): Int = when (item) {
                is MetaItem.NodeItem -> item.node[Meta.VALUE_KEY].value ?: error("Can't convert node to a value")
                is MetaItem.ValueItem -> item.value
            }.int

            override fun objectToMetaItem(obj: Int): MetaItem<*> = MetaItem.ValueItem(obj.asValue())
        }
    }
}

fun <T : Any> MetaCaster<T>.metaToObject(meta: Meta): T = itemToObject(MetaItem.NodeItem(meta))
fun <T : Any> MetaCaster<T>.valueToObject(value: Value): T = itemToObject(MetaItem.ValueItem(value))
