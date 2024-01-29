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


public fun <T> DataSink<T>.data(value: NamedData<T>) {
    data(value.name, value.data)
}

public fun <T> DataSink<T>.emitAll(sequence: Sequence<NamedData<T>>) {
    sequence.forEach { data(it) }
}

public fun <T> DataSink<T>.branch(dataTree: DataTree<T>) {
    emitAll(dataTree.asSequence())
}

public inline fun <T> DataSink<T>.branch(
    prefix: Name,
    block: DataSink<T>.() -> Unit,
) {
    if (prefix.isEmpty()) {
        apply(block)
    } else {
        val proxyDataSink = DataSink { nameWithoutPrefix, data ->
            this.data(prefix + nameWithoutPrefix, data)
        }

        proxyDataSink.apply(block)
    }
}

public inline fun <T> DataSink<T>.branch(
    prefix: String,
    block: DataSink<T>.() -> Unit,
): Unit = branch(prefix.asName(), block)


public fun <T> DataSink<T>.data(name: String, value: Data<T>) {
    data(Name.parse(name), value)
}

public fun <T> DataSink<T>.branch(name: Name, set: DataTree<T>) {
    branch(name) { emitAll(set.asSequence()) }
}

public fun <T> DataSink<T>.branch(name: String, set: DataTree<T>) {
    branch(Name.parse(name)) { emitAll(set.asSequence()) }
}

/**
 * Produce lazy [Data] and emit it into the [MutableDataTree]
 */
public inline fun <reified T> DataSink<T>.data(
    name: String,
    meta: Meta = Meta.EMPTY,
    noinline producer: suspend () -> T,
) {
    val data = Data(meta, block = producer)
    data(name, data)
}

public inline fun <reified T> DataSink<T>.data(
    name: Name,
    meta: Meta = Meta.EMPTY,
    noinline producer: suspend () -> T,
) {
    val data = Data(meta, block = producer)
    data(name, data)
}

/**
 * Emit static data with the fixed value
 */
public inline fun <reified T> DataSink<T>.static(
    name: String,
    data: T,
    meta: Meta = Meta.EMPTY,
): Unit = data(name, Data.static(data, meta))

public inline fun <reified T> DataSink<T>.static(
    name: Name,
    data: T,
    meta: Meta = Meta.EMPTY,
): Unit = data(name, Data.static(data, meta))

public inline fun <reified T> DataSink<T>.static(
    name: String,
    data: T,
    mutableMeta: MutableMeta.() -> Unit,
): Unit = data(Name.parse(name), Data.static(data, Meta(mutableMeta)))


public fun <T> DataSink<T>.populateFrom(sequence: Sequence<NamedData<T>>) {
    sequence.forEach {
        data(it.name, it.data)
    }
}

public fun <T> DataSink<T>.populateFrom(tree: DataTree<T>) {
    populateFrom(tree.asSequence())
}


/**
 * Update data with given node data and meta with node meta.
 */
@DFExperimental
public fun <T> MutableDataTree<T>.populateFrom(flow: ObservableDataSource<T>): Job = flow.updates().onEach {
    //TODO check if the place is occupied
    data(it.name, it.data)
}.launchIn(scope)

//public fun <T > DataSetBuilder<T>.populateFrom(flow: Flow<NamedData<T>>) {
//    flow.collect {
//        data(it.name, it.data)
//    }
//}
