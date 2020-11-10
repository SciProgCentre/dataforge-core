package hep.dataforge.meta

import hep.dataforge.names.*
import hep.dataforge.values.Value
import kotlin.properties.ReadWriteProperty

public interface MutableItemProvider : ItemProvider {
    public fun setItem(name: Name, item: MetaItem<*>?)
}

public fun MutableItemProvider.getItem(key: String): MetaItem<*>? = getItem(key.toName())

public fun MutableItemProvider.setValue(name: Name, value: Value?): Unit =
    setItem(name, value?.let { MetaItem.ValueItem(value) })

public fun MutableItemProvider.setNode(name: Name, meta: Meta?): Unit =
    setItem(name, meta?.let { MetaItem.NodeItem(meta) })

public fun MutableItemProvider.setItem(key: String, item: MetaItem<*>?): Unit = setItem(key.toName(), item)

public fun MutableItemProvider.setValue(key: String, value: Value?): Unit =
    setItem(key, value?.let { MetaItem.ValueItem(value) })

public fun MutableItemProvider.setNode(key: String, meta: Meta?): Unit =
    setItem(key, meta?.let { MetaItem.NodeItem(meta) })

public fun MutableItemProvider.node(key: Name? = null): ReadWriteProperty<Any?, Meta?> = item(key).convert(
    reader = { it.node },
    writer = { it?.let { MetaItem.NodeItem(it) } }
)

@Suppress("NOTHING_TO_INLINE")
public inline fun MutableItemProvider.remove(name: Name): Unit = setItem(name, null)

@Suppress("NOTHING_TO_INLINE")
public inline fun MutableItemProvider.remove(name: String): Unit = remove(name.toName())


/**
 * Universal unsafe set method
 */
public operator fun MutableItemProvider.set(name: Name, value: Any?) {
    when (value) {
        null -> remove(name)
        is MetaItem<*> -> setItem(name, value)
        is Meta -> setNode(name, value)
        is Configurable -> setNode(name, value.config)
        else -> setValue(name, Value.of(value))
    }
}

public operator fun MutableItemProvider.set(name: NameToken, value: Any?): Unit =
    set(name.asName(), value)

public operator fun MutableItemProvider.set(key: String, value: Any?): Unit =
    set(key.toName(), value)

public operator fun MutableItemProvider.set(key: String, index: String, value: Any?): Unit =
    set(key.toName().withIndex(index), value)



/* Same name siblings generation */

public fun MutableItemProvider.setIndexedItems(
    name: Name,
    items: Iterable<MetaItem<*>>,
    indexFactory: (MetaItem<*>, index: Int) -> String = { _, index -> index.toString() }
) {
    val tokens = name.tokens.toMutableList()
    val last = tokens.last()
    items.forEachIndexed { index, meta ->
        val indexedToken = NameToken(last.body, last.index + indexFactory(meta, index))
        tokens[tokens.lastIndex] = indexedToken
        setItem(Name(tokens), meta)
    }
}

public fun MutableItemProvider.setIndexed(
    name: Name,
    metas: Iterable<Meta>,
    indexFactory: (Meta, index: Int) -> String = { _, index -> index.toString() }
) {
    setIndexedItems(name, metas.map { MetaItem.NodeItem(it) }) { item, index -> indexFactory(item.node!!, index) }
}

public operator fun MutableItemProvider.set(name: Name, metas: Iterable<Meta>): Unit = setIndexed(name, metas)
public operator fun MutableItemProvider.set(name: String, metas: Iterable<Meta>): Unit = setIndexed(name.toName(), metas)
