package space.kscience.dataforge.data

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.isEmpty
import space.kscience.dataforge.names.plus


public fun <T> DataSink<T>.put(value: NamedData<T>) {
    put(value.name, value.data)
}

public inline fun <T> DataSink<T>.putAll(
    prefix: Name,
    block: DataSink<T>.() -> Unit,
) {
    if (prefix.isEmpty()) {
        apply(block)
    } else {
        val proxyDataSink = DataSink { nameWithoutPrefix, data ->
            this.put(prefix + nameWithoutPrefix, data)
        }

        proxyDataSink.apply(block)
    }
}

@Deprecated("Use putAll", ReplaceWith("putAll(prefix, block)"))
public inline fun <T> DataSink<T>.branch(
    prefix: Name,
    block: DataSink<T>.() -> Unit,
): Unit = putAll(prefix, block)


public inline fun <T> DataSink<T>.putAll(
    prefix: String,
    block: DataSink<T>.() -> Unit,
): Unit = putAll(prefix.asName(), block)

@Deprecated("Use putAll", ReplaceWith("putAll(prefix, block)"))
public inline fun <T> DataSink<T>.branch(
    prefix: String,
    block: DataSink<T>.() -> Unit,
): Unit = putAll(prefix, block)

public fun <T> DataSink<T>.put(name: String, value: Data<T>) {
    put(Name.parse(name), value)
}

public fun <T> DataSink<T>.putAll(name: Name, tree: DataTree<T>) {
    putAll(name) { putAll(tree.asSequence()) }
}

@Deprecated("Use putAll", ReplaceWith("putAll(name, tree)"))
public fun <T> DataSink<T>.branch(name: Name, tree: DataTree<T>): Unit = putAll(name, tree)

public fun <T> DataSink<T>.putAll(name: String, tree: DataTree<T>) {
    putAll(Name.parse(name)) { putAll(tree.asSequence()) }
}

@Deprecated("Use putAll", ReplaceWith("putAll(name, tree)"))
public fun <T> DataSink<T>.branch(name: String, tree: DataTree<T>): Unit = putAll(name, tree)

/**
 * Produce lazy [Data] and emit it into the [MutableDataTree]
 */
public inline fun <reified T> DataSink<T>.put(
    name: String,
    meta: Meta = Meta.EMPTY,
    noinline producer: suspend () -> T,
) {
    val data = Data(meta, block = producer)
    put(name, data)
}

public inline fun <reified T> DataSink<T>.put(
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
public inline fun <reified T> DataSink<T>.wrap(
    name: String,
    data: T,
    meta: Meta = Meta.EMPTY,
): Unit = put(name, Data.static(data, meta))

public inline fun <reified T> DataSink<T>.wrap(
    name: Name,
    data: T,
    meta: Meta = Meta.EMPTY,
): Unit = put(name, Data.static(data, meta))

public inline fun <reified T> DataSink<T>.wrap(
    name: String,
    data: T,
    mutableMeta: MutableMeta.() -> Unit,
): Unit = put(Name.parse(name), Data.static(data, Meta(mutableMeta)))


public fun <T> DataSink<T>.putAll(sequence: Sequence<NamedData<T>>) {
    sequence.forEach {
        put(it.name, it.data)
    }
}

public fun <T> DataSink<T>.putAll(tree: DataTree<T>) {
    putAll(tree.asSequence())
}

/**
 * Copy given data set and mirror its changes to this [DataSink] in [this@setAndObserve]. Returns an update [Job]
 */
public fun <T : Any> DataSink<T>.putAllAndWatch(
    branchName: Name = Name.EMPTY,
    dataSet: ObservableDataTree<T>,
): Job {
    putAll(branchName, dataSet)
    return dataSet.updates().onEach {
        put(branchName + it.name, it.data)
    }.launchIn(dataSet.updatesScope)
}


@Deprecated("Use putAllAndWatch", ReplaceWith("putAllAndWatch(name, dataSet)"))
public fun <T : Any> DataSink<T>.watchBranch(
    name: Name,
    dataSet: ObservableDataTree<T>,
): Job = putAllAndWatch(name, dataSet)