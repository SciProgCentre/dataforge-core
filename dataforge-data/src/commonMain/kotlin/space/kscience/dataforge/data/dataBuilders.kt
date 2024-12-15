package space.kscience.dataforge.data

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.isEmpty
import space.kscience.dataforge.names.plus


public suspend fun <T> DataSink<T>.put(value: NamedData<T>) {
    put(value.name, value.data)
}

public inline fun <T> DataSink<T>.putAll(
    prefix: Name,
    block: DataSink<T>.() -> Unit,
) {
    if (prefix.isEmpty()) {
        apply(block)
    } else {
        val proxyDataSink = DataSink<T> { name, data -> this@putAll.put(prefix + name, data) }

        proxyDataSink.apply(block)
    }
}


public inline fun <T> DataSink<T>.putAll(
    prefix: String,
    block: DataSink<T>.() -> Unit,
): Unit = putAll(prefix.asName(), block)


public suspend fun <T> DataSink<T>.put(name: String, value: Data<T>) {
    put(Name.parse(name), value)
}

public suspend fun <T> DataSink<T>.putAll(name: Name, tree: DataTree<T>) {
    putAll(name) { putAll(tree.asSequence()) }
}


public suspend fun <T> DataSink<T>.putAll(name: String, tree: DataTree<T>) {
    putAll(Name.parse(name)) { putAll(tree.asSequence()) }
}

/**
 * Produce lazy [Data] and emit it into the [MutableDataTree]
 */
public suspend inline fun <reified T> DataSink<T>.putValue(
    name: String,
    meta: Meta = Meta.EMPTY,
    noinline producer: suspend () -> T,
) {
    val data = Data(meta, block = producer)
    put(name, data)
}

public suspend inline fun <reified T> DataSink<T>.putValue(
    name: Name,
    meta: Meta = Meta.EMPTY,
    noinline producer: suspend () -> T,
) {
    val data = Data(meta, block = producer)
    put(name, data)
}

/**
 * Emit static data with the fixed value
 */
public suspend inline fun <reified T> DataSink<T>.putValue(
    name: Name,
    value: T,
    meta: Meta = Meta.EMPTY,
): Unit = put(name, Data.wrapValue(value, meta))

public suspend inline fun <reified T> DataSink<T>.putValue(
    name: String,
    value: T,
    meta: Meta = Meta.EMPTY,
): Unit = put(name, Data.wrapValue(value, meta))

public suspend inline fun <reified T> DataSink<T>.putValue(
    name: String,
    value: T,
    metaBuilder: MutableMeta.() -> Unit,
): Unit = put(Name.parse(name), Data.wrapValue(value, Meta(metaBuilder)))

public suspend fun <T> DataSink<T>.putAll(sequence: Sequence<NamedData<T>>) {
    sequence.forEach {
        put(it.name, it.data)
    }
}

public suspend fun <T> DataSink<T>.putAll(map: Map<Name, Data<T>?>) {
    map.forEach { (name, data) ->
        put(name, data)
    }
}

public suspend fun <T> DataSink<T>.putAll(tree: DataTree<T>) {
    putAll(tree.asSequence())
}

/**
 * Copy given data set and mirror its changes to this [DataSink]. Suspends indefinitely.
 */
public suspend fun <T : Any> DataSink<T>.putAllAndWatch(
    source: DataTree<T>,
    branchName: Name = Name.EMPTY,
) {
    putAll(branchName, source)
    source.updates.collect {
        put(branchName + it.name, it.data)
    }
}