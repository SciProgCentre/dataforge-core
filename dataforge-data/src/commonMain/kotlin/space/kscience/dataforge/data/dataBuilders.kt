package space.kscience.dataforge.data

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.isEmpty
import space.kscience.dataforge.names.plus


public fun <T> DataSink<T>.emit(value: NamedData<T>) {
    emit(value.name, value.data)
}

public fun <T> DataSink<T>.emitAll(sequence: Sequence<NamedData<T>>) {
    sequence.forEach { emit(it) }
}

public fun <T> DataSink<T>.emitAll(dataTree: DataTree<T>) {
    emitAll(dataTree.asSequence())
}

public inline fun <T> DataSink<T>.emitAll(
    prefix: Name,
    block: DataSink<T>.() -> Unit,
) {
    if (prefix.isEmpty()) {
        apply(block)
    } else {
        val proxyDataSink = DataSink { nameWithoutPrefix, data ->
            this.emit(prefix + nameWithoutPrefix, data)
        }

        proxyDataSink.apply(block)
    }
}


public fun <T> DataSink<T>.emit(name: String, value: Data<T>) {
    emit(Name.parse(name), value)
}

public fun <T> DataSink<T>.emitAll(name: Name, set: DataTree<T>) {
    emitAll(name) { emitAll(set.asSequence()) }
}

public fun <T> DataSink<T>.emitAll(name: String, set: DataTree<T>) {
    emitAll(Name.parse(name)) { emitAll(set.asSequence()) }
}

/**
 * Produce lazy [Data] and emit it into the [MutableDataTree]
 */
public inline fun <reified T> DataSink<T>.produce(
    name: String,
    meta: Meta = Meta.EMPTY,
    noinline producer: suspend () -> T,
) {
    val data = Data(meta, block = producer)
    emit(name, data)
}

public inline fun <reified T> DataSink<T>.produce(
    name: Name,
    meta: Meta = Meta.EMPTY,
    noinline producer: suspend () -> T,
) {
    val data = Data(meta, block = producer)
    emit(name, data)
}

/**
 * Emit static data with the fixed value
 */
public inline fun <reified T> DataSink<T>.static(
    name: String,
    data: T,
    meta: Meta = Meta.EMPTY,
): Unit = emit(name, Data.static(data, meta))

public inline fun <reified T> DataSink<T>.static(
    name: Name,
    data: T,
    meta: Meta = Meta.EMPTY,
): Unit = emit(name, Data.static(data, meta))

public inline fun <reified T> DataSink<T>.static(
    name: String,
    data: T,
    mutableMeta: MutableMeta.() -> Unit,
): Unit = emit(Name.parse(name), Data.static(data, Meta(mutableMeta)))


public fun <T> DataSink<T>.populateFrom(sequence: Sequence<NamedData<T>>) {
    sequence.forEach {
        emit(it.name, it.data)
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
    emit(it.name, it.data)
}.launchIn(scope)

//public fun <T > DataSetBuilder<T>.populateFrom(flow: Flow<NamedData<T>>) {
//    flow.collect {
//        data(it.name, it.data)
//    }
//}
