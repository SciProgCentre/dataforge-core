package space.kscience.dataforge.data

import kotlinx.coroutines.CoroutineScope
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
        val proxyDataSink = object :DataSink<T>{
            override fun put(name: Name, data: Data<T>?) {
                this@putAll.put(prefix + name, data)
            }

            override suspend fun update(name: Name, data: Data<T>?) {
                this@putAll.update(prefix + name, data)
            }

        }

        proxyDataSink.apply(block)
    }
}


public inline fun <T> DataSink<T>.putAll(
    prefix: String,
    block: DataSink<T>.() -> Unit,
): Unit = putAll(prefix.asName(), block)


public fun <T> DataSink<T>.put(name: String, value: Data<T>) {
    put(Name.parse(name), value)
}

public fun <T> DataSink<T>.putAll(name: Name, tree: DataTree<T>) {
    putAll(name) { putAll(tree.asSequence()) }
}


public fun <T> DataSink<T>.putAll(name: String, tree: DataTree<T>) {
    putAll(Name.parse(name)) { putAll(tree.asSequence()) }
}

/**
 * Produce lazy [Data] and emit it into the [MutableDataTree]
 */
public inline fun <reified T> DataSink<T>.putValue(
    name: String,
    meta: Meta = Meta.EMPTY,
    noinline producer: suspend () -> T,
) {
    val data = Data(meta, block = producer)
    put(name, data)
}

public inline fun <reified T> DataSink<T>.putValue(
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
public inline fun <reified T> DataSink<T>.putValue(
    name: String,
    value: T,
    meta: Meta = Meta.EMPTY,
): Unit = put(name, Data.wrapValue(value, meta))

public inline fun <reified T> DataSink<T>.putValue(
    name: Name,
    value: T,
    meta: Meta = Meta.EMPTY,
): Unit = put(name, Data.wrapValue(value, meta))

public inline fun <reified T> DataSink<T>.putValue(
    name: String,
    value: T,
    metaBuilder: MutableMeta.() -> Unit,
): Unit = put(Name.parse(name), Data.wrapValue(value, Meta(metaBuilder)))

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
    scope: CoroutineScope,
    branchName: Name = Name.EMPTY,
    source: DataTree<T>,
): Job {
    putAll(branchName, source)
    return source.updates.onEach {
        put(branchName + it.name, it.data)
    }.launchIn(scope)
}