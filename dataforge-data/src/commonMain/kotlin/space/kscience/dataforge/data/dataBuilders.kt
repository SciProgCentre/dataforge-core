package space.kscience.dataforge.data

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.isEmpty
import space.kscience.dataforge.names.plus


public suspend fun <T> DataSink<T>.write(value: NamedData<T>) {
    write(value.name, value)
}

public inline fun <T> DataSink<T>.writeAll(
    prefix: Name,
    block: DataSink<T>.() -> Unit,
) {
    if (prefix.isEmpty()) {
        apply(block)
    } else {
        val proxyDataSink = DataSink<T> { name, data -> this@writeAll.write(prefix + name, data) }

        proxyDataSink.apply(block)
    }
}


public inline fun <T> DataSink<T>.writeAll(
    prefix: String,
    block: DataSink<T>.() -> Unit,
): Unit = writeAll(prefix.asName(), block)


public suspend fun <T> DataSink<T>.write(name: String, value: Data<T>) {
    write(Name.parse(name), value)
}

public suspend fun <T> DataSink<T>.writeAll(name: Name, tree: DataTree<T>) {
    writeAll(name) { writeAll(tree.asSequence()) }
}


public suspend fun <T> DataSink<T>.writeAll(name: String, tree: DataTree<T>) {
    writeAll(Name.parse(name)) { writeAll(tree.asSequence()) }
}

/**
 * Produce lazy [Data] and emit it into the [MutableDataTree]
 */
public suspend inline fun <reified T> DataSink<T>.writeValue(
    name: String,
    meta: Meta = Meta.EMPTY,
    noinline producer: suspend () -> T,
) {
    val data = Data(meta, block = producer)
    write(name, data)
}

public suspend inline fun <reified T> DataSink<T>.writeValue(
    name: Name,
    meta: Meta = Meta.EMPTY,
    noinline producer: suspend () -> T,
) {
    val data = Data(meta, block = producer)
    write(name, data)
}

/**
 * Emit static data with the fixed value
 */
public suspend inline fun <reified T> DataSink<T>.writeValue(
    name: Name,
    value: T,
    meta: Meta = Meta.EMPTY,
): Unit = write(name, Data.wrapValue(value, meta))

public suspend inline fun <reified T> DataSink<T>.writeValue(
    name: String,
    value: T,
    meta: Meta = Meta.EMPTY,
): Unit = write(name, Data.wrapValue(value, meta))

public suspend inline fun <reified T> DataSink<T>.writeValue(
    name: String,
    value: T,
    metaBuilder: MutableMeta.() -> Unit,
): Unit = write(Name.parse(name), Data.wrapValue(value, Meta(metaBuilder)))

public suspend fun <T> DataSink<T>.writeAll(sequence: Sequence<NamedData<T>>) {
    sequence.forEach {
        write(it)
    }
}

public suspend fun <T> DataSink<T>.writeAll(map: Map<Name, Data<T>?>) {
    map.forEach { (name, data) ->
        write(name, data)
    }
}

/**
 * Copy all data from [this] and mirror changes if they appear. Suspends indefinitely.
 */
public suspend fun <T : Any> MutableDataTree<T>.writeAllAndWatch(
    source: DataTree<T>,
    prefix: Name = Name.EMPTY,
) {
    writeAll(prefix, source)
    source.updates.collect {
        write(prefix + it, source.read(it))
    }
}