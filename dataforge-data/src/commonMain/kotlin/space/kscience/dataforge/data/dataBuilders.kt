package space.kscience.dataforge.data

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.isEmpty
import space.kscience.dataforge.names.plus


public fun <T> DataSink<T>.put(value: NamedData<T>) {
    put(value.name, value.data)
}

public fun <T> DataSink<T>.branch(dataTree: DataTree<T>) {
    putAll(dataTree.asSequence())
}

public inline fun <T> DataSink<T>.branch(
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

public inline fun <T> DataSink<T>.branch(
    prefix: String,
    block: DataSink<T>.() -> Unit,
): Unit = branch(prefix.asName(), block)


public fun <T> DataSink<T>.put(name: String, value: Data<T>) {
    put(Name.parse(name), value)
}

public fun <T> DataSink<T>.branch(name: Name, set: DataTree<T>) {
    branch(name) { putAll(set.asSequence()) }
}

public fun <T> DataSink<T>.branch(name: String, set: DataTree<T>) {
    branch(Name.parse(name)) { putAll(set.asSequence()) }
}

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
    this.putAll(tree.asSequence())
}


/**
 * Update data with given node data and meta with node meta.
 */
@DFExperimental
public fun <T> MutableDataTree<T>.putAll(source: DataTree<T>) {
    source.forEach {
        put(it.name, it.data)
    }
}

/**
 * Copy given data set and mirror its changes to this [DataSink] in [this@setAndObserve]. Returns an update [Job]
 */
public fun <T : Any> DataSink<T>.watchBranch(
    name: Name,
    dataSet: ObservableDataTree<T>,
): Job {
    branch(name, dataSet)
    return dataSet.updates().onEach {
        put(name + it.name, it.data)
    }.launchIn(dataSet.updatesScope)

}