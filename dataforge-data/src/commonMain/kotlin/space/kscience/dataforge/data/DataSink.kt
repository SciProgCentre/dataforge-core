package space.kscience.dataforge.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.isEmpty
import space.kscience.dataforge.names.plus



/**
 * Asynchronous data sink
 */
public fun interface DataSink<in T> : DataBuilderScope<T> {
    /**
     * Put data and notify listeners if needed
     */
    public suspend fun write(name: Name, data: Data<T>?)
}

/**
 * Write single data into the sink
 */
public suspend fun <T> DataSink<T>.write(value: NamedData<T>) {
    write(value.name, value)
}

/**
 * Write all data produced by [block] into this sink with the given prefix.
 */
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

/**
 * Write all data produced by [block] into this sink with the given prefix.
 */
public inline fun <T> DataSink<T>.writeAll(
    prefix: String,
    block: DataSink<T>.() -> Unit,
): Unit = writeAll(prefix.asName(), block)


/**
 * Write single data into the sink
 */
public suspend fun <T> DataSink<T>.write(name: String, value: Data<T>) {
    write(Name.parse(name), value)
}

@Deprecated("Use writeAll(tree, name) instead", ReplaceWith("writeAll(tree, name)"))
public suspend fun <T> DataSink<T>.writeAll(name: Name, tree: DataTree<T>) {
    writeAll(name) { writeAll(tree.asSequence()) }
}

/**
 * Write all data from the tree into this sink with the given prefix. Do not observe the tree changes.
 */
public suspend fun <T> DataSink<T>.writeAll(tree: DataTree<T>, prefix: Name = Name.EMPTY) {
    if (prefix.isEmpty()) {
        writeAll(tree)
    } else {
        writeAll(prefix) { writeAll(tree.asSequence()) }
    }
}

/**
 * Write all data from the tree into this sink with the given prefix. Do not observe the tree changes.
 */
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

/**
 * Produce lazy [Data] and emit it into the [MutableDataTree]
 */
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

/**
 * Emit static data with the fixed value
 */
public suspend inline fun <reified T> DataSink<T>.writeValue(
    name: String,
    value: T,
    metaBuilder: MutableMeta.() -> Unit,
): Unit = write(Name.parse(name), Data.wrapValue(value, Meta(metaBuilder)))

/**
 * Write all data from the sequence into this sink. Does not return until source is exhausted.
 */
public suspend fun <T> DataSink<T>.writeAll(sequence: Sequence<NamedData<T>>) {
    sequence.forEach {
        write(it)
    }
}

/**
 * Write all data from the map into this sink.
 */
public suspend fun <T> DataSink<T>.writeAll(map: Map<Name, Data<T>?>) {
    map.forEach { (name, data) ->
        write(name, data)
    }
}

/**
 * Copy all data from [this] and mirror changes if they appear. Returns a job that can be canceled.
 *
 * Use provided [scope] to launch the job and control its lifecycle.
 */
@OptIn(DFExperimental::class)
public fun <T : Any> DataSink<T>.launchWriteJobFrom(
    source: DataTree<T>,
    scope: CoroutineScope,
    prefix: Name = Name.EMPTY,
): Job = scope.launch {
    writeAll(source, prefix)
    source.updates.collect {
        write(prefix + it, source.read(it))
    }
}